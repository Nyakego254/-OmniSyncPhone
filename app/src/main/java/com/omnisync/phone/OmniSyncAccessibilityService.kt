package com.omnisync.phone

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.provider.Settings

class OmniSyncAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) return
        
        val packageName = event.packageName?.toString() ?: return
        if (packageName == "com.omnisync.phone") return

        val text = event.text?.joinToString(" ") ?: ""
        
        val db = DatabaseHelper(applicationContext)
        db.insertNotification(packageName, event.source?.packageName?.toString() ?: packageName, text, System.currentTimeMillis())
        SyncWorker.enqueueSync(applicationContext)
    }

    override fun onInterrupt() {
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            "enabled_accessibility_services"
        )
        return enabledServices?.contains(packageName) == true
    }
}