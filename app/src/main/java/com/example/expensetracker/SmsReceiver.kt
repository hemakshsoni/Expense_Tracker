package com.example.expensetracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // DEBUG LOG: This proves the app is actually listening
        Log.d("SMS_DEBUG", "Broadcast Received! Starting analysis...")

        try {
            if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                val pendingResult = goAsync()
                val appContext = context.applicationContext
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

                if (messages.isNullOrEmpty()) {
                    Log.d("SMS_DEBUG", "No messages found in intent.")
                    pendingResult.finish()
                    return
                }

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val sender = messages[0].originatingAddress ?: ""
                        val fullMessage = messages.joinToString("") { it.messageBody ?: "" }

                        Log.d("SMS_DEBUG", "Raw Data -> Sender: '$sender' | Msg: '$fullMessage'")

                        // 1. SENDER CHECK
                        if (!isValidBankSender(sender)) {
                            Log.d("SMS_DEBUG", "❌ Rejected: Sender '$sender' is not in Bank Whitelist.")
                            return@launch
                        }

                        // 2. PARSE with strict Reference rules
                        val transactionData = parseSms(fullMessage, sender)

                        if (transactionData != null) {
                            saveTransaction(appContext, transactionData)
                            Log.d("SMS_DEBUG", "✅ SAVED: $transactionData")
                        } else {
                            Log.d("SMS_DEBUG", "❌ Rejected: Valid Bank, but invalid format or missing Reference No.")
                        }

                    } catch (e: Exception) {
                        Log.e("SMS_DEBUG", "CRASH in Coroutine", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SMS_DEBUG", "CRASH in onReceive", e)
        }
    }

    // --- SENDER FILTER ---
    private fun isValidBankSender(sender: String): Boolean {
        // If testing via Emulator, you might use a number like "1234".
        // Ensure you type a valid Bank ID like "VM-HDFC" in the emulator.

        if (sender.matches(Regex("^[+]?\\d+\$"))) return false
        val prefixPattern = Regex("^[a-zA-Z]{2}-")
        if (!sender.contains(prefixPattern)) return false

        val senderBody = sender.substringAfter("-")
        val validBanks = listOf(
            "SBI", "HDFC", "ICICI", "AXIS", "KOTAK", "IDFC", "YES",
            "RBL", "INDUS", "BOB", "PNB", "CANARA", "UNION", "IOB",
            "UBI", "IDBI", "PAYTM", "CBOI", "MAHB", "CITI", "HSBC",
            "STANSC", "AMEX", "DBS", "BOM", "CORP", "VIJAYA", "J&K",
            "FED", "SOUTH", "KARNATAKA", "KVB"
        )
        return validBanks.any { senderBody.contains(it, true) }
    }

    // --- PARSING LOGIC ---
    data class ParsedTransaction(
        val amount: Double,
        val type: String,
        val merchant: String,
        val paymentMethod: String,
        val reference: String
    )

    private fun parseSms(message: String, sender: String): ParsedTransaction? {
        val lowerMsg = message.lowercase().replace(",", "")

        // --- 1. EXTRACT REFERENCE (Structural Rule: [Letters]*[Numbers]+) ---
        // Matches "HDFC12345" or "12345"
        // DOES NOT Match "12AB34"
        // --- 1. EXTRACT REFERENCE (Structural Rule + Word Boundary) ---
        // Added \\b at the end.
        // This ensures "abcd123456789123d" FAILS because the number doesn't end cleanly.
        val refPattern = Regex("(?i)(?:ref\\s?no|utr|upi\\s?ref|txn\\s?id|imps|id|txn|ref)[:\\s\\/.-]*([a-zA-Z]*\\d+)\\b")

        val refMatch = refPattern.find(message)
        if (refMatch == null) {
            Log.d("SMS_DEBUG", "Parser Fail: No Reference Pattern Found")
            return null
        }

        val reference = refMatch.groupValues[1]

        // --- 2. LENGTH CHECK (12 to 22) ---
        if (reference.length !in 12..22) {
            Log.d("SMS_DEBUG", "Parser Fail: Ref found '$reference' but length ${reference.length} is invalid.")
            return null
        }

        // --- 3. EXTRACT AMOUNT & TYPE ---
        var amount: Double? = null
        var type = "Expense"

        val incomePatterns = listOf(
            Regex("(?:credited|received)\\s+(?:by|with)?\\s*(?:rs\\.?|inr|₹)?\\s*([0-9]+(?:\\.[0-9]{1,2})?)"),
            Regex("(?:rs\\.?|inr|₹)\\s*([0-9]+(?:\\.[0-9]{1,2})?)\\s*(?:credited|received)")
        )
        val expensePatterns = listOf(
            Regex("(?:debited|spent|paid|sent|withdraw|purchase|tran|transaction)\\s+(?:by|with|to|for)?\\s*(?:rs\\.?|inr|₹)?\\s*([0-9]+(?:\\.[0-9]{1,2})?)"),
            Regex("(?:rs\\.?|inr|₹)\\s*([0-9]+(?:\\.[0-9]{1,2})?)\\s*(?:debited|spent|paid|sent|withdraw)")
        )

        for (pattern in incomePatterns) {
            val match = pattern.find(lowerMsg)
            if (match != null) {
                amount = match.groupValues[1].toDoubleOrNull()
                type = "Income"
                break
            }
        }
        if (amount == null) {
            for (pattern in expensePatterns) {
                val match = pattern.find(lowerMsg)
                if (match != null) {
                    amount = match.groupValues[1].toDoubleOrNull()
                    type = "Expense"
                    break
                }
            }
        }

        if (amount == null) {
            Log.d("SMS_DEBUG", "Parser Fail: No Amount Found")
            return null
        }

        // --- 4. EXTRACT MERCHANT ---
        var merchant = extractMerchant(message)
        if (merchant == null) {
            merchant = cleanSenderName(sender)
        }

        val method = when {
            lowerMsg.contains("upi") -> "UPI"
            lowerMsg.contains("card") -> "Card"
            lowerMsg.contains("atm") -> "Cash"
            else -> "Online"
        }

        return ParsedTransaction(amount, type, merchant, method, reference)
    }

    private suspend fun saveTransaction(context: Context, data: ParsedTransaction) {
        val db = TransactionDatabase.getDatabase(context)
        val newTransaction = Transaction(
            title = data.merchant,
            amount = data.amount,
            type = data.type,
            category = if (data.type == "Income") "Salary" else "Other",
            paymentMethod = data.paymentMethod,
            date = System.currentTimeMillis(),
            reference = data.reference
        )
        db.transactionDao.upsertTransaction(newTransaction)
    }

    private fun extractMerchant(message: String): String? {
        val cleaned = message
            .lowercase()
            .replace("\n", " ")
            .replace(Regex("avl bal.*"), "")
            .replace(Regex("ref no.*"), "")
            .replace(Regex("\\d{1,2}[a-z]{3}\\d{0,4}"), "")

        val patterns = listOf(
            Regex("(?i)(?:at|to|by)\\s+([a-z][a-z0-9@._*-]{2,})"),
            Regex("(?i)vpa[:\\-]?\\s*([a-z0-9@._*-]+)"),
            Regex("(?i)info[:\\-]?\\s*([a-z0-9@._*-]+)")
        )

        for (pattern in patterns) {
            val match = pattern.find(cleaned)
            if (match != null) {
                return match.groupValues.last().uppercase()
            }
        }
        return null
    }

    private fun cleanSenderName(sender: String): String {
        val prefixPattern = Regex("^[A-Z]{2}-")
        return sender.replace(prefixPattern, "")
    }
}