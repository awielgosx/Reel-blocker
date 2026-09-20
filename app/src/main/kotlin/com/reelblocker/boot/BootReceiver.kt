package com.reelblocker.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.reelblocker.permissions.PermissionChecks
import com.reelblocker.tracking.TrackingService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (PermissionChecks.hasUsageAccess(context)) {
            TrackingService.start(context)
        }
    }
}
