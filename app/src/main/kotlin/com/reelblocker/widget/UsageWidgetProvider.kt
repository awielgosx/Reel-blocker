package com.reelblocker.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.reelblocker.R
import com.reelblocker.tracking.LiveUsage
import com.reelblocker.tracking.TrackingState
import com.reelblocker.util.DurationFormat

class UsageWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val usage = TrackingState.live.value
        appWidgetIds.forEach { id -> appWidgetManager.updateAppWidget(id, buildViews(context, usage)) }
    }

    companion object {
        fun pushUpdate(context: Context, usage: LiveUsage) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, UsageWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val views = buildViews(context, usage)
            ids.forEach { id -> manager.updateAppWidget(id, views) }
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
