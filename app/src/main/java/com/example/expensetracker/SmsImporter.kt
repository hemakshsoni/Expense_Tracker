package com.example.expensetracker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class SmsImporter(private val context: Context) {

    suspend fun importTransactions(months: Int) = withContext(Dispatchers.IO) {
        val startTime = Calendar.getInstance().apply {
            add(Calendar.MONTH, -months)
        }.timeInMillis
        performImport(startTime)
    }

    suspend fun importTransactionsRange(startTime: Long, endTime: Long) = withContext(Dispatchers.IO) {
        performImport(startTime, endTime)
    }

    private suspend fun performImport(startTime: Long, endTime: Long = System.currentTimeMillis()) {
        try {
            Log.d("SMS_IMPORT", "Starting SMS import from $startTime to $endTime")
            
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) 
                != PackageManager.PERMISSION_GRANTED) {
                Log.e("SMS_IMPORT", "Permission READ_SMS not granted!")
                return
            }

            val cursor = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                "${Telephony.Sms.DATE} > ? AND ${Telephony.Sms.DATE} < ?",
                arrayOf(startTime.toString(), endTime.toString()),
                "${Telephony.Sms.DATE} DESC"
            )

            if (cursor == null) {
                Log.e("SMS_IMPORT", "Cursor is null")
                return
            }

            cursor.use {
                val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)

                if (addressIndex == -1 || bodyIndex == -1 || dateIndex == -1) {
                    Log.e("SMS_IMPORT", "Column index error: $addressIndex, $bodyIndex, $dateIndex")
                    return
                }

                while (it.moveToNext()) {
                    while (cursor.moveToNext()) {
                        val rawSms = RawSms(
                            sender = cursor.getString(addressIndex),
                            body = cursor.getString(bodyIndex),
                            timestamp = cursor.getLong(dateIndex)
                        )
                        val processor = SmsProcessor(context)
                        processor.process(rawSms)
                    }

                }
            }
        } catch (e: Exception) {
            Log.e("SMS_IMPORT", "Error during SMS import", e)
        }
    }

}
