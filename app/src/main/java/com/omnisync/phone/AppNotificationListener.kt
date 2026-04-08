package com.omnisync.phone

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class OmniSyncNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val packageName = sbn.packageName
        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getString(android.app.Notification.EXTRA_TITLE)
        val text = extras.getString(android.app.Notification.EXTRA_TEXT)
        val timestamp = sbn.postTime

        // Skip our own app's notifications
        if (packageName == "com.omnisync.phone") return

        val db = DatabaseHelper(applicationContext)
        db.insertNotification(packageName, title, text, timestamp)
        SyncWorker.enqueueSync(applicationContext)
        Log.d("OmniSync", "Notification captured from $packageName")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Not needed for sync
    }
}