package com.reelblocker.security

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Device Admin is used only so [GuardAccessibilityService] can detect when the user is on the
 * "deactivate device admin" screen and intercept it with a PIN prompt - not to enforce any
 * device policy itself.
 */
class ReelBlockerDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Turning this off removes Reel Blocker's protection against impulsively " +
            "disabling itself. Enter your PIN in the app to do this deliberately instead."
    }

    companion object {
        fun componentName(context: Context): ComponentName =
            ComponentName(context, ReelBlockerDeviceAdminReceiver::class.java)
    }
}
