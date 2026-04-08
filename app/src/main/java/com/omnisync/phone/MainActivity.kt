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
        
        if (!areCorePermissionsReady()) {
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
        
        if (!areCorePermissionsReady()) {
            showNextPendingPermission()
        } else {
            onAllPermissionsReady()
        }
    }

    private fun showPermissionSetupDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Setup")
            .setMessage("OmniSync needs permissions to sync your calls and SMS. Would you like to set them up now?\n\nNote: Notification access is optional - the app works with just calls and SMS.")
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
            !PermissionHelper.isBatteryOptimizationDisabled(this) -> {
                pendingAction = PendingAction.BATTERY
                showSinglePermissionDialog(
                    "Battery Optimization",
                    "Disable battery optimization to ensure OmniSync works in background.",
                    "Disable"
                ) {
                    PermissionHelper.requestBatteryOptimization(this)
                }
            }
            !PermissionHelper.isAccessibilityServiceEnabled(this) -> {
                pendingAction = PendingAction.NOTIFICATION
                showSinglePermissionDialog(
                    "Notification Access Required",
                    "Enable accessibility service to sync notifications from apps.",
                    "Enable"
                ) {
                    PermissionHelper.requestAccessibilityService(this)
                }
            }
            else -> {
                if (areCorePermissionsReady()) {
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
        items.add("Runtime Permissions")
        if (!PermissionHelper.isBatteryOptimizationDisabled(this)) {
            items.add("Battery Optimization")
        }
        if (!PermissionHelper.isAccessibilityServiceEnabled(this)) {
            items.add("Notification Access")
        }
        items.add("Auto-start Settings")
        items.add("App Details")

        AlertDialog.Builder(this)
            .setTitle("Open Settings")
            .setItems(items.toTypedArray()) { _, which ->
                val item = items[which]
                when {
                    item == "Runtime Permissions" -> startPermissionFlow()
                    item == "Battery Optimization" -> PermissionHelper.requestBatteryOptimization(this)
                    item == "Notification Access" -> PermissionHelper.requestAccessibilityService(this)
                    item == "Auto-start Settings" -> PermissionHelper.openAutoStartSettings(this)
                    item == "App Details" -> PermissionHelper.openAppSettings(this)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updatePermissionStatus() {
        updatePermissionView(Manifest.permission.READ_PHONE_STATE, statusPhoneState)
        updatePermissionView(Manifest.permission.READ_CALL_LOG, statusCallLog)
        updatePermissionView(Manifest.permission.READ_SMS, statusSms)

        val accessibilityEnabled = PermissionHelper.isAccessibilityServiceEnabled(this)
        statusNotification.text = if (accessibilityEnabled) "✓ Enabled" else "✗ Disabled"
        statusNotification.setTextColor(if (accessibilityEnabled) 0xFF00FF00.toInt() else 0xFFFF0000.toInt())

        val batteryOptimized = !PermissionHelper.isBatteryOptimizationDisabled(this)
        statusBattery.text = if (batteryOptimized) "✗ Enabled" else "✓ Disabled"
        statusBattery.setTextColor(if (batteryOptimized) 0xFFFF0000.toInt() else 0xFF00FF00.toInt())

        val autostartEnabled = isAutoStartEnabled()
        statusAutostart.text = if (autostartEnabled) "✓ Enabled" else "○ Check Required"
        statusAutostart.setTextColor(if (autostartEnabled) 0xFF00FF00.toInt() else 0xFF888888.toInt())

        updateSyncStatus()
    }

    private fun updatePermissionView(permission: String, textView: TextView) {
        val granted = ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        textView.text = if (granted) "✓ Granted" else "✗ Denied"
        textView.setTextColor(if (granted) 0xFF00FF00.toInt() else 0xFFFF0000.toInt())
    }

    private fun updateSyncStatus() {
        val coreReady = areCorePermissionsReady()

        if (coreReady) {
            val notifStatus = if (PermissionHelper.isAccessibilityServiceEnabled(this)) " + Notifications" else ""
            tvStatus.text = "✓ Active$notifStatus"
            tvStatus.setTextColor(0xFF00FF00.toInt())
            btnRequestPermissions.isEnabled = false
            btnOpenSettings.isEnabled = true
        } else {
            val missingPermissions = mutableListOf<String>()
            if (!PermissionHelper.areAllRuntimePermissionsGranted(this)) missingPermissions.add("Permissions")
            if (!PermissionHelper.isBatteryOptimizationDisabled(this)) missingPermissions.add("Battery")
            if (!PermissionHelper.isAccessibilityServiceEnabled(this)) missingPermissions.add("Notifications")
            
            tvStatus.text = "⚠ ${missingPermissions.joinToString(", ")} required"
            tvStatus.setTextColor(0xFFFFA500.toInt())
            btnRequestPermissions.isEnabled = true
            btnOpenSettings.isEnabled = true
        }
    }

    private fun areCorePermissionsReady(): Boolean {
        return PermissionHelper.areAllRuntimePermissionsGranted(this) &&
                PermissionHelper.isBatteryOptimizationDisabled(this) &&
                PermissionHelper.isAccessibilityServiceEnabled(this)
    }

    private fun onAllPermissionsReady() {
        PermissionHelper.setPermissionSetupComplete(this, true)
        Toast.makeText(this, "Sync is now active!", Toast.LENGTH_SHORT).show()
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