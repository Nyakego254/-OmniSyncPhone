package com.omnisync.phone

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.ActivityCompat

object PermissionHelper {

    private const val PREFS_NAME = "OmniSyncPrefs"
    private const val KEY_FIRST_RUN = "first_run"
    private const val KEY_PERMISSION_SETUP_COMPLETE = "permission_setup_complete"

    const val REQUEST_CODE_PERMISSIONS = 100
    const val REQUEST_CODE_NOTIFICATION = 101
    const val REQUEST_CODE_BATTERY = 102

    fun isFirstRun(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_FIRST_RUN, true)
    }

    fun setFirstRunComplete(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_FIRST_RUN, false).apply()
    }

    fun isPermissionSetupComplete(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PERMISSION_SETUP_COMPLETE, false)
    }

    fun setPermissionSetupComplete(context: Context, complete: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PERMISSION_SETUP_COMPLETE, complete).apply()
    }

    val runtimePermissions: Array<String>
        get() = buildList {
            add(Manifest.permission.READ_PHONE_STATE)
            add(Manifest.permission.READ_CALL_LOG)
            add(Manifest.permission.RECEIVE_SMS)
            add(Manifest.permission.READ_SMS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()

    fun requestRuntimePermissions(activity: Activity) {
        val permissionsToRequest = runtimePermissions.filter {
            ActivityCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissionsToRequest, REQUEST_CODE_PERMISSIONS)
        }
    }

    fun requestNotificationAccess(activity: Activity) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        activity.startActivityForResult(intent, REQUEST_CODE_NOTIFICATION)
    }

    fun requestBatteryOptimization(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:${activity.packageName}")
            activity.startActivityForResult(intent, REQUEST_CODE_BATTERY)
        }
    }

    fun openAutoStartSettings(context: Context) {
        val manufacturers = listOf(
            Pair("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            Pair("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity"),
            Pair("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
            Pair("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            Pair("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            Pair("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
            Pair("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            Pair("com.asus.mobilemanager", "com.asus.mobilemanager.MainActivity")
        )

        for ((packageName, className) in manufacturers) {
            try {
                val intent = Intent()
                intent.component = ComponentName(packageName, className)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                continue
            }
        }

        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:${context.packageName}")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:${context.packageName}")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return enabledListeners?.contains(context.packageName) == true
    }

    fun isBatteryOptimizationDisabled(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun areAllRuntimePermissionsGranted(activity: Activity): Boolean {
        return runtimePermissions.all {
            ActivityCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun areAllPermissionsReady(context: Context): Boolean {
        return areAllRuntimePermissionsGranted(context as? Activity ?: return false) &&
                isNotificationListenerEnabled(context) &&
                isBatteryOptimizationDisabled(context)
    }
}
