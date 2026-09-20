package com.reelblocker.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.reelblocker.MainActivity
import com.reelblocker.tracking.LiveUsage
import com.reelblocker.util.DurationFormat

object TimerNotification {
    const val CHANNEL_ID = "reelblocker_timer"
    const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Usage timer",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Ongoing session and daily usage timer for tracked apps"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun build(context: Context, usage: LiveUsage): android.app.Notification {
        val sessionText = if (usage.sessionApp != null) {
            "${usage.sessionApp.displayName} session: ${DurationFormat.format(usage.sessionMillis)}"
        } else {
            "No tracked app open"
        }
        val perAppText = usage.dayTotalsMillis.entries
            .filter { it.value > 0 }
            .joinToString("  ·  ") { (app, millis) -> "${app.displayName} ${DurationFormat.format(millis)}" }
            .ifBlank { "Nothing tracked yet today" }

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentTitle("$sessionText  ·  Today: ${DurationFormat.format(usage.combinedDayMillis)}")
            .setContentText(perAppText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(perAppText))
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
