package com.example.expensetracker

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.first
import java.security.MessageDigest

class SmsProcessor(
    private val context: Context
) {
    // Accepts: AX-CENTBK-T, VM-HDFCBK, AD-SBIINB, VK-ICICIB, BOB-BANK, CBoI, etc.
    private val senderIdPattern =
        Regex("^[A-Za-z0-9]{2,}-[A-Za-z0-9]{2,10}(?:-[A-Za-z0-9])?$", RegexOption.IGNORE_CASE)

    private val merchantBlocklist = setOf(
        "UPI","IMPS","NEFT","RTGS","ATM","CASH","BANK","ACCOUNT",
        "TXN","TRANSFER","PAYMENT","INFO","VPA","FROM","A/C","REF","REFNO"
    )

    private val blockedSenderKeywords = listOf(
        "NSE", "NSEIL", "BSE", "BSEIND", "NSEALR", "NSEIPO", "BSEIPO", "NSESMS"
    )

    suspend fun process(rawSms: RawSms) {
        val lower = rawSms.body.lowercase()
        
        // Refined security check: block actual OTP messages but allow transactions that contain security warnings in the footer
        val securityKeywords = Regex("\\b(otp|verification|verification code|security code|secret code|auth code|login code|auth status)\\b")
        val transactionKeywords = Regex("\\b(debited|credited|spent|paid|sent|received|purchase|withdraw|refund|reversed|\\bdr\\b|\\bcr\\b)\\b")
        
        if (lower.contains(securityKeywords) && !lower.contains(transactionKeywords)) {
            Log.d("SMS_DEBUG", "Filtered out security/OTP message")
            return
        }

        if (!isValidSender(rawSms.sender)) {
            Log.d("SMS_DEBUG", "Invalid sender: ${rawSms.sender}")
            return
        }

        val parsed = parseSms(rawSms.body, rawSms.sender, rawSms.timestamp) ?: return
        saveTransaction(parsed, rawSms.sender)
    }

    private fun isValidSender(sender: String): Boolean {
        if (sender.matches(Regex("^[+]?\\d+$"))) return false
        val upperSender = sender.uppercase()
        if (blockedSenderKeywords.any { upperSender.contains(it) }) {
            Log.d("SMS_DEBUG", "Blocked exchange sender: $sender")
            return false
        }
        // Standard bank SMS format: CC-BANKID or alphanumeric BANKID (min 3 chars)
        return sender.contains("-") || sender.length >= 3 || senderIdPattern.matches(sender)
    }

    private fun parseSms(message: String, sender: String, timestamp: Long): ParsedTransaction? {
        val lower = message.lowercase().replace(",", "")

        // 🚫 BLOCK recharge / plan activation confirmations
        if (
            lower.contains("recharge") &&
            (lower.contains("credited") || lower.contains("successfully")) &&
            !lower.contains("debited")
        ) {
            return null
        }

        // 🚫 BLOCK payment requests / mandates / approvals
        if (
            lower.contains("request") ||
            lower.contains("approve") ||
            lower.contains("autopay") ||
            lower.contains("mandate") ||
            lower.contains("collect request") ||
            lower.contains("has sent you") ||
            lower.contains("click to approve") ||
            lower.contains("tap to approve") ||
            lower.contains("subscription") ||
            lower.contains("setup")
        ) {
            return null
        }

        if (
            lower.contains("ipo") ||
            lower.contains("asba") ||
            lower.contains("mandate") ||
            lower.contains("blocking of funds") ||
            lower.contains("upi-mandate") ||
            lower.contains("nseil")
        ) {
            return null
        }

        // --- AMOUNT EXTRACTION ---
        // Handles "Rs.100", "₹ 100", "debited by 100", "spent 100", etc.
        val amountRegex = Regex("(?i)(?:rs\\.?|inr|₹|amt|amount)\\s*([0-9]+(?:\\.[0-9]{1,2})?)")
        val amountMatch = amountRegex.find(lower)
            ?: Regex("(?i)(?:debited|credited|spent|paid|sent|received|amt|amount)\\s+(?:by|with|to|of)?\\s*([0-9]+(?:\\.[0-9]{1,2})?)").find(lower)
        
        val amount = amountMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: return null

        // --- TYPE DETECTION ---
        // Strip the balance part for type detection to avoid picking up balance indicators like "Bal: Rs. 100 CR"
        val sanitizedForType = lower.replace(Regex("(?i)(total bal|avl bal|available balance|clr bal|balance|clr bal|clear bal).*"), "")

        val isExpense = sanitizedForType.contains(Regex("(?i)(debited|spent|paid|sent|withdraw|purchase|\\bdr\\b|\\bdr\\.)"))
        val isIncome = sanitizedForType.contains(Regex("(?i)(credited|received|refund|reversed|\\bcr\\b|\\bcr\\.)"))

        val type = when {
            sanitizedForType.contains(Regex("(?i)([0-9.]+\\s*dr\\.?)")) -> "EXPENSE"
            sanitizedForType.contains(Regex("(?i)([0-9.]+\\s*cr\\.?)")) -> "INCOME"
            isExpense && !isIncome -> "EXPENSE"
            isIncome && !isExpense -> "INCOME"
            lower.contains("debited") -> "EXPENSE"
            lower.contains("credited") -> "INCOME"
            else -> return null
        }

        // --- MERCHANT ---
        val (merchant, source) = extractMerchant(message, lower)
            ?: (cleanSenderName(sender) to MerchantSource.SENDER)

        // --- PAYMENT METHOD ---
        val method = when {
            lower.contains("upi") -> "UPI"
            lower.contains("credit card") -> "Credit Card"
            lower.contains("debit card") -> "Debit Card"
            lower.contains("atm") || lower.contains("withdraw") -> "Cash"
            lower.contains("neft") -> "Net Banking"
            else -> "Online"
        }

        // --- REFERENCE ---
        val refRegex = Regex("(?i)(?:utr|rrn|ref\\s?no|ref|txn\\s?id|upi\\s?ref|refno\\s)[:\\-\\s]*([a-z0-9]{6,})\\b")
        val reference = refRegex.find(message)?.groupValues?.get(1)
                ?: "SMS_${hash("$sender|$amount|$timestamp")}"

        return ParsedTransaction(
            amount = amount,
            type = type,
            merchant = merchant,
            paymentMethod = method,
            reference = reference,
            merchantSource = source,
            date = timestamp
        )
    }

    private fun extractMerchant(message: String, lower: String): Pair<String, MerchantSource>? {

        // 1. Strip reference/txn sections completely
        val sanitizedLower = lower.replace(
            Regex("(?i)\\b(ref|refno|utr|rrn|txn|transaction)\\b.*"),
            ""
        )

        // 2. VPA (highest confidence)
        val vpa = Regex("(?i)([a-z0-9._-]+@[a-z]{3,})").find(message)
        if (vpa != null) {
            return vpa.groupValues[1].substringBefore("@").uppercase() to MerchantSource.BODY
        }

        // 3. Contextual
        val cleaned = sanitizedLower.replace(Regex("avl\\.?\\s*bal.*"), "")
        val ctx = Regex(
            "(?i)(?:to|at|paid to|spent on|trf to|from)\\s+" +
                    "([a-z0-9&._-]+(?:\\s+[a-z0-9&._-]+){0,2})"
        ).find(cleaned)

        if (ctx != null) {
            val candidate = ctx.groupValues[1].uppercase()

            val badTokens = listOf("REF", "REFNO", "UTR", "RRN", "TXN")
            if (badTokens.any { candidate.contains(it) }) return null
            if (merchantBlocklist.any { candidate.contains(it) }) return null

            return candidate to MerchantSource.BODY
        }

        return null
    }

    private fun cleanSenderName(sender: String): String {
        val parts = sender.split("-")
        val bankBody = if (parts.size > 1) parts[1] else parts[0]

        return bankBody
            .replace(Regex("(?i)(BK|BANK|INB|SMS|TXN|ALERT|INFO)"), "")
            .trim()
            .uppercase()
    }

    private fun hash(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(input.toByteArray()).take(4).joinToString("") { "%02x".format(it) }
    }

    private suspend fun saveTransaction(parsed: ParsedTransaction, sender: String) {
        val db = TransactionDatabase.getDatabase(context)

        // Dedup: same reference OR same merchant+amount within 30s of original date
        val exists = db.transactionDao.existsDuplicate(
            reference = parsed.reference,
            amount = parsed.amount,
            merchant = parsed.merchant,
            type = parsed.type,
            sinceTime = parsed.date - 30_000
        )
        if (exists) return

        // 🏦 1. Identify potential bank name from sender
        val extractedBank = cleanSenderName(sender)

        // 🏦 2. Get all user-defined payment methods
        val allMethods = db.paymentMethodDao.getAllPaymentMethods().first()
        
        // 🏦 3. Matching Strategy:
        // Priority 1: Exact or contains match with user's accounts (e.g., "SBI" matches "SBI Bank")
        // Priority 2: Fallback to the transaction type detected in message (e.g., "UPI")
        val matchedAccount = allMethods.find { method ->
            val mName = method.name.uppercase()
            mName.contains(extractedBank) || extractedBank.contains(mName) ||
            mName.replace("BANK", "").contains(extractedBank)
        }

        val finalPaymentMethod = matchedAccount?.name ?: parsed.paymentMethod

        // 🔥 NEW: Check for auto-learned category
        val learnedCategory = db.merchantCategoryDao().getCategoryForMerchant(normalizeMerchant(parsed.merchant))
        val finalCategory = learnedCategory ?: "Other"
        val needsReview = learnedCategory == null

        val txn = Transaction(
            title = parsed.merchant,
            amount = parsed.amount,
            type = parsed.type,
            category = finalCategory,
            paymentMethod = finalPaymentMethod,
            date = parsed.date,
            reference = parsed.reference,
            needsReview = needsReview, // ✅ Automatically review if category is already learned
            merchantSource = parsed.merchantSource,
            isAutoDetected = true
        )

        db.transactionDao.upsertTransaction(txn)
        Log.d("SMS_DEBUG", "Saved transaction: ${txn.title} [${txn.type}] via $finalPaymentMethod")
    }
}
