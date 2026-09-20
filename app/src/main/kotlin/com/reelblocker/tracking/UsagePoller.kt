package com.reelblocker.tracking

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process

/** Thin wrapper around [UsageStatsManager] to answer "what app is on screen right now?". */
object UsagePoller {

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Returns the package currently in the foreground, or null if nothing relevant happened
     * in the lookback window (e.g. screen off, or the foreground app already went to background).
     */
    fun getCurrentForegroundPackage(context: Context, lookbackMillis: Long = 20_000): String? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - lookbackMillis
        val events = usm.queryEvents(start, end)
        val event = UsageEvents.Event()

        var lastForegroundPackage: String? = null
        var wentToBackground = false

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    lastForegroundPackage = event.packageName
                    wentToBackground = false
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    if (event.packageName == lastForegroundPackage) {
                        wentToBackground = true
                    }
                }
            }
        }
        return if (wentToBackground) null else lastForegroundPackage
    }
}
