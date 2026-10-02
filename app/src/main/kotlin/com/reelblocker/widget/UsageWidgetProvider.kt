package com.reelblocker.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.reelblocker.R
import com.reelblocker.data.UsageRepository
import com.reelblocker.tracking.DayBoundary
import com.reelblocker.tracking.LiveUsage
import com.reelblocker.tracking.TrackingState
import com.reelblocker.util.DurationFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UsageWidgetProvider : AppWidgetProvider() {

    /**
     * System-triggered updates (placement, reboot, the periodic refresh) can land before the
     * foreground service has ticked even once this process - [TrackingState] would still be at
     * its untouched zero default then. Fall back to reading straight from Room in that case
     * instead of showing a false "0m" the moment the widget appears.
     */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val usage = resolveUsage(context)
                appWidgetIds.forEach { id -> appWidgetManager.updateAppWidget(id, buildViews(context, usage)) }
            } catch (e: Exception) {
                // A Room read failure here must not crash the whole app process - leave the
                // widget showing its last-known content rather than taking the app down.
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        fun pushUpdate(context: Context, usage: LiveUsage) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, UsageWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val views = buildViews(context, usage)
            ids.forEach { id -> manager.updateAppWidget(id, views) }
        }

        private suspend fun resolveUsage(context: Context): LiveUsage {
            val live = TrackingState.live.value
            if (live.sessionApp != null || live.dayTotalsMillis.isNotEmpty()) return live

            val today = DayBoundary.currentAppDay()
            val dayUsage = UsageRepository(context).getDay(today)
            return LiveUsage(sessionApp = null, sessionMillis = 0L, dayTotalsMillis = dayUsage.perAppMillis)
        }

        private fun buildViews(context: Context, usage: LiveUsage): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_usage)
            views.setTextViewText(R.id.widget_total_value, DurationFormat.format(usage.combinedDayMillis))
            val perApp = usage.dayTotalsMillis.entries
                .filter { it.value > 0 }
                .joinToString("   ") { (app, millis) -> "${app.displayName} ${DurationFormat.format(millis)}" }
            views.setTextViewText(R.id.widget_per_app, perApp)
            return views
        }
    }
}
