package com.omnisync.phone

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat

class CallDetectionService : Service() {

    private lateinit var telephonyManager: TelephonyManager
    private var lastPhoneNumber: String? = null
    private var lastCallStartTime: Long = 0L

    override fun onCreate() {
        super.onCreate()
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        startForegroundService()
        listenForCalls()
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "omnisync_channel",
                "OmniSync",
                NotificationManager.IMPORTANCE_NONE
            ).apply {
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "omnisync_channel")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    private fun listenForCalls() {
        val phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                when (state) {
                    TelephonyManager.CALL_STATE_RINGING -> {
                        lastPhoneNumber = phoneNumber
                        lastCallStartTime = System.currentTimeMillis()
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        if (lastPhoneNumber != null && lastCallStartTime > 0) {
                            val duration = System.currentTimeMillis() - lastCallStartTime
                            val callType = if (duration < 1000) 3 else 1
                            saveCall(lastPhoneNumber!!, callType, lastCallStartTime)
                        }
                        lastPhoneNumber = null
                        lastCallStartTime = 0L
                    }
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        if (lastPhoneNumber == null && phoneNumber != null) {
                            lastPhoneNumber = phoneNumber
                            lastCallStartTime = System.currentTimeMillis()
                        }
                    }
                }
            }
        }
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private fun saveCall(phoneNumber: String, type: Int, timestamp: Long) {
        val db = DatabaseHelper(this)
        db.insertCall(phoneNumber, type, timestamp)
        SyncWorker.enqueueSync(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}