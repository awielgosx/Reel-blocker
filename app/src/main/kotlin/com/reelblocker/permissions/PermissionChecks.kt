package com.reelblocker.permissions

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import androidx.core.content.ContextCompat
import com.reelblocker.security.GuardAccessibilityService
import com.reelblocker.security.ReelBlockerDeviceAdminReceiver
import com.reelblocker.tracking.UsagePoller

/** Central place to check whether each permission this app needs is actually granted. */
object PermissionChecks {

    fun hasUsageAccess(context: Context): Boolean = UsagePoller.hasUsageAccess(context)

    fun hasNotificationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun isAccessibilityGuardEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val expected = "${context.packageName}/${GuardAccessibilityService::class.java.name}"
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expected, ignoreCase = true)) return true
        }
        return false
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(ReelBlockerDeviceAdminReceiver.componentName(context))
    }

    fun allCoreGranted(context: Context): Boolean =
        hasUsageAccess(context) &&
            hasNotificationPermission(context) &&
            isIgnoringBatteryOptimizations(context)
}
