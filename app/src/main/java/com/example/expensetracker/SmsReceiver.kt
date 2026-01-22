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
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val pendingResult = goAsync()
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        if (messages.isNullOrEmpty()) {
            pendingResult.finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sender = messages[0].originatingAddress ?: ""
                val fullMessage = messages.joinToString(" ") { it.messageBody ?: "" }
                val rawSms = RawSms(
                    sender = sender,
                    body = fullMessage,
                    timestamp = System.currentTimeMillis()
                )
                val processedSms = SmsProcessor(context.applicationContext)
                processedSms.process(rawSms)
                Log.d("SMS_DEBUG", "Processed SMS: $rawSms")
            } catch (e: Exception) {
                Log.e("SMS_DEBUG", "SMS processing failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

}
