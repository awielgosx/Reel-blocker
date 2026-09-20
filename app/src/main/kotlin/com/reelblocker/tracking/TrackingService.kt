package com.reelblocker.tracking

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.reelblocker.data.UsageRepository
import com.reelblocker.notifications.TimerNotification
import com.reelblocker.widget.UsageWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Foreground service that is the source of truth for "what's on screen right now" and "how long
 * has it been there today". Polls [UsagePoller] every [POLL_INTERVAL_MILLIS], persists to Room,
 * and publishes to [TrackingState] for the UI/widget plus the ongoing notification.
 */
class TrackingService : Service() {

    private lateinit var repository: UsageRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loopJob: Job? = null

    private var currentSession: SessionState? = null
    private var trackedDay: LocalDate = DayBoundary.currentAppDay()
    private var dayTotals: MutableMap<MonitoredApp, Long> = mutableMapOf()
    private var ticksSinceWidgetPush = 0

    private data class SessionState(
        val app: MonitoredApp,
        val appDay: LocalDate,
        val baselineMillis: Long,
        var sessionMillis: Long = 0L,
    )

    override fun onCreate() {
        super.onCreate()
        repository = UsageRepository(this)
        TimerNotification.ensureChannel(this)
        startForeground(TimerNotification.NOTIFICATION_ID, TimerNotification.build(this, TrackingState.live.value))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (loopJob == null) {
            loopJob = scope.launch {
                loadDayTotals(trackedDay)
                runLoop()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        loopJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun loadDayTotals(day: LocalDate) {
        dayTotals = repository.getDay(day).perAppMillis.toMutableMap()
    }

    private suspend fun runLoop() {
        var lastElapsed = SystemClock.elapsedRealtime()
        while (true) {
            delay(POLL_INTERVAL_MILLIS)
            val now = SystemClock.elapsedRealtime()
            val deltaMillis = (now - lastElapsed).coerceAtLeast(0)
            lastElapsed = now

            val foregroundPackage = UsagePoller.getCurrentForegroundPackage(this@TrackingService)
            val foregroundApp = MonitoredApp.fromPackageName(foregroundPackage)
            val session = currentSession

            if (foregroundApp == null) {
                currentSession = null
            } else if (session == null || session.app != foregroundApp) {
                val appDay = DayBoundary.currentAppDay()
                val baseline = repository.getDay(appDay).perAppMillis[foregroundApp] ?: 0L
                currentSession = SessionState(foregroundApp, appDay, baseline)
            } else {
                session.sessionMillis += deltaMillis
            }

            currentSession?.let { s ->
                val totalForDay = s.baselineMillis + s.sessionMillis
                repository.recordForegroundMillis(s.appDay, s.app, totalForDay)
                if (s.appDay == trackedDay) {
                    dayTotals[s.app] = totalForDay
                }
            }

            val nowAppDay = DayBoundary.currentAppDay()
            if (nowAppDay != trackedDay && (currentSession == null || currentSession?.appDay == nowAppDay)) {
                trackedDay = nowAppDay
                loadDayTotals(trackedDay)
            }

            val live = LiveUsage(
                sessionApp = currentSession?.app,
                sessionMillis = currentSession?.sessionMillis ?: 0L,
                dayTotalsMillis = dayTotals.toMap(),
            )
            TrackingState.update(live)
            NotificationManagerCompat.from(this@TrackingService)
                .notify(TimerNotification.NOTIFICATION_ID, TimerNotification.build(this@TrackingService, live))

            ticksSinceWidgetPush++
            if (ticksSinceWidgetPush >= WIDGET_PUSH_EVERY_N_TICKS) {
                ticksSinceWidgetPush = 0
                UsageWidgetProvider.pushUpdate(this@TrackingService, live)
            }
        }
    }

    companion object {
        private const val POLL_INTERVAL_MILLIS = 5_000L
        private const val WIDGET_PUSH_EVERY_N_TICKS = 12 // ~60s

        fun start(context: Context) {
            val intent = Intent(context, TrackingService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
