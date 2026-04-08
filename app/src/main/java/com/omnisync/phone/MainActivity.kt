package com.omnisync.phone

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MainActivity : AppCompatActivity() {

    private lateinit var statusPhoneState: TextView
    private lateinit var statusCallLog: TextView
    private lateinit var statusSms: TextView
    private lateinit var statusNotification: TextView
    private lateinit var statusBattery: TextView
    private lateinit var statusAutostart: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnRequestPermissions: Button
    private lateinit var btnOpenSettings: Button

    private var pendingAction: PendingAction? = null

    private enum class PendingAction {
        NOTIFICATION, BATTERY, AUTOSTART
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initFirebase()
        
        updatePermissionStatus()
        
        if (!areAllPermissionsReady()) {
            showPermissionSetupDialog()
        }
    }

    private fun initViews() {
        statusPhoneState = findViewById(R.id.statusPhoneState)
        statusCallLog = findViewById(R.id.statusCallLog)
        statusSms = findViewById(R.id.statusSms)
        statusNotification = findViewById(R.id.statusNotification)
        statusBattery = findViewById(R.id.statusBattery)
        statusAutostart = findViewById(R.id.statusAutostart)
        tvStatus = findViewById(R.id.tvStatus)
        btnRequestPermissions = findViewById(R.id.btnRequestPermissions)
        btnOpenSettings = findViewById(R.id.btnOpenSettings)

        btnRequestPermissions.setOnClickListener {
            startPermissionFlow()
        }

        btnOpenSettings.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun initFirebase() {
        val options = FirebaseOptions.Builder()
            .setApplicationId("1:610577076954:android:f2d625e2426cf7f0bbcbe3")
            .setApiKey("AIzaSyBBbIKHF4Bh0VN9pDIz75COXNdeLx29alc")
            .setDatabaseUrl("https://omnisync-78f1e-default-rtdb.firebaseio.com")
            .setProjectId("omnisync-78f1e")
            .build()

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this, options)
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        pendingAction = null
        
        updatePermissionStatus()
        
        if (!areAllPermissionsReady()) {
            showNextPendingPermission()
        } else {
            onAllPermissionsReady()
        }
    }

    private fun showPermissionSetupDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Setup")
            .setMessage("OmniSync needs several permissions to sync your calls, SMS, and notifications. Would you like to set them up now?")
            .setPositiveButton("Yes, Setup") { _, _ ->
                startPermissionFlow()
            }
            .setNegativeButton("Later") { _, _ ->
                updatePermissionStatus()
            }
            .setCancelable(false)
            .show()
    }

    private fun startPermissionFlow() {
        if (!PermissionHelper.areAllRuntimePermissionsGranted(this)) {
            pendingAction = null
            PermissionHelper.requestRuntimePermissions(this)
            return
        }

        showNextPendingPermission()
    }

    private fun showNextPendingPermission() {
        when {
            !PermissionHelper.isNotificationListenerEnabled(this) -> {
                pendingAction = PendingAction.NOTIFICATION
                showSinglePermissionDialog(
                    "Notification Access Required",
                    "OmniSync needs notification access to sync your notifications. Tap 'Open Settings' to enable it.",
                    "Open Settings"
                ) {
                    PermissionHelper.requestNotificationAccess(this)
                }
            }
            !PermissionHelper.isBatteryOptimizationDisabled(this) -> {
                pendingAction = PendingAction.BATTERY
                showSinglePermissionDialog(
                    "Battery Optimization Required",
                    "Disable battery optimization to ensure OmniSync works in the background. Tap 'Disable' to proceed.",
                    "Disable"
                ) {
                    PermissionHelper.requestBatteryOptimization(this)
                }
            }
            else -> {
                if (areAllPermissionsReady()) {
                    onAllPermissionsReady()
                }
            }
        }
    }

    private fun showSinglePermissionDialog(title: String, message: String, positiveText: String, onPositive: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveText) { _, _ ->
                onPositive()
            }
            .setNegativeButton("Skip") { _, _ ->
                pendingAction = null
                updatePermissionStatus()
                showNextPendingPermission()
            }
            .setCancelable(false)
            .show()
    }

    private fun showSettingsDialog() {
        val items = mutableListOf<String>()
        if (!PermissionHelper.isNotificationListenerEnabled(this)) {
            items.add("Notification Listener")
        }
        if (!PermissionHelper.isBatteryOptimizationDisabled(this)) {
            items.add("Battery Optimization")
        }
        items.add("Auto-start Settings")
        items.add("App Details")

        if (items.isEmpty()) {
            Toast.makeText(this, "All settings configured!", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Open Settings")
            .setItems(items.toTypedArray()) { _, which ->
                val item = items[which]
                when {
                    item == "Notification Listener" -> {
                        PermissionHelper.requestNotificationAccess(this)
                    }
                    item == "Battery Optimization" -> {
                        PermissionHelper.requestBatteryOptimization(this)
                    }
                    item == "Auto-start Settings" -> {
                        PermissionHelper.openAutoStartSettings(this)
                    }
                    item == "App Details" -> {
                        PermissionHelper.openAppSettings(this)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updatePermissionStatus() {
        updatePermissionView(Manifest.permission.READ_PHONE_STATE, statusPhoneState)
        updatePermissionView(Manifest.permission.READ_CALL_LOG, statusCallLog)
        updatePermissionView(Manifest.permission.READ_SMS, statusSms)

        val notificationEnabled = PermissionHelper.isNotificationListenerEnabled(this)
        statusNotification.text = if (notificationEnabled) "✓ Enabled" else "✗ Disabled"
        statusNotification.setTextColor(if (notificationEnabled) 0xFF00FF00.toInt() else 0xFFFF0000.toInt())

        val batteryOptimized = !PermissionHelper.isBatteryOptimizationDisabled(this)
        statusBattery.text = if (batteryOptimized) "✗ Enabled" else "✓ Disabled"
        statusBattery.setTextColor(if (batteryOptimized) 0xFFFF0000.toInt() else 0xFF00FF00.toInt())

        val autostartEnabled = isAutoStartEnabled()
        statusAutostart.text = if (autostartEnabled) "✓ Enabled" else "✗ Check Required"
        statusAutostart.setTextColor(if (autostartEnabled) 0xFF00FF00.toInt() else 0xFFFF0000.toInt())

        updateSyncStatus()
    }

    private fun updatePermissionView(permission: String, textView: TextView) {
        val granted = ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        textView.text = if (granted) "✓ Granted" else "✗ Denied"
        textView.setTextColor(if (granted) 0xFF00FF00.toInt() else 0xFFFF0000.toInt())
    }

    private fun updateSyncStatus() {
        val allReady = areAllPermissionsReady()

        if (allReady) {
            tvStatus.text = "✓ All permissions granted - Sync active"
            tvStatus.setTextColor(0xFF00FF00.toInt())
            btnRequestPermissions.isEnabled = false
            btnOpenSettings.isEnabled = true
        } else {
            val missingPermissions = mutableListOf<String>()
            if (!PermissionHelper.areAllRuntimePermissionsGranted(this)) missingPermissions.add("Permissions")
            if (!PermissionHelper.isNotificationListenerEnabled(this)) missingPermissions.add("Notification")
            if (!PermissionHelper.isBatteryOptimizationDisabled(this)) missingPermissions.add("Battery")
            
            tvStatus.text = "⚠ ${missingPermissions.joinToString(", ")} required"
            tvStatus.setTextColor(0xFFFFA500.toInt())
            btnRequestPermissions.isEnabled = true
            btnOpenSettings.isEnabled = true
        }
    }

    private fun areAllPermissionsReady(): Boolean {
        return PermissionHelper.areAllRuntimePermissionsGranted(this) &&
                PermissionHelper.isNotificationListenerEnabled(this) &&
                PermissionHelper.isBatteryOptimizationDisabled(this)
    }

    private fun onAllPermissionsReady() {
        PermissionHelper.setPermissionSetupComplete(this, true)
        Toast.makeText(this, "All permissions granted! Sync is now active.", Toast.LENGTH_LONG).show()
        startServices()
    }

    private fun startServices() {
        val callIntent = Intent(this, CallDetectionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(callIntent)
        } else {
            startService(callIntent)
        }

        SyncWorker.schedulePeriodicSync(this)
    }

    private fun isAutoStartEnabled(): Boolean {
        val intent = Intent()
        val componentName = ComponentName("com.omnisync.phone", "com.omnisync.phone.MainActivity")
        intent.component = componentName
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo != null
    }
}
