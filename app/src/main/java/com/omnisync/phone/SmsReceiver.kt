package com.omnisync.phone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (smsMessage in messages) {
                val address = smsMessage.displayOriginatingAddress
                val body = smsMessage.displayMessageBody
                val timestamp = smsMessage.timestampMillis

                val db = DatabaseHelper(context)
                db.insertSms(address, body, timestamp)
                // Trigger sync
                SyncWorker.enqueueSync(context)
            }
        }
    }
}