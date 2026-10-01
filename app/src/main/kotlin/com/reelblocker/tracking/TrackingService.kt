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

/**
 * Foreground service that is the source of truth for "what's on screen right now" and "how long
 * has it been there today". Polls [UsagePoller] every [POLL_INTERVAL_MILLIS], delegates the
 * actual session/day-boundary decisions to [SessionTracker] (unit-tested separately), persists
 * what it says to Room, and publishes to [TrackingState] for the UI/widget plus the notification.
 */
class TrackingService : Service() {

    private lateinit var repository: UsageRepository
    private val tracker = SessionTracker()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loopJob: Job? = null
    private var state: SessionTracker.State = SessionTracker.State(DayBoundary.currentAppDay(), emptyMap(), null)
    private var ticksSinceWidgetPush = 0

    override fun onCreate() {
        super.onCreate()
        repository = UsageRepository(this)
        TimerNotification.ensureChannel(this)
        startForeground(TimerNotification.NOTIFICATION_ID, TimerNotification.build(this, TrackingState.live.value))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (loopJob == null) {
            loopJob = scope.launch {
                state = state.copy(dayTotalsMillis = repository.getDay(state.trackedDay).perAppMillis)
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

    private suspend fun runLoop() {
        var lastElapsed = SystemClock.elapsedRealtime()
        while (true) {
            delay(POLL_INTERVAL_MILLIS)
            val now = SystemClock.elapsedRealtime()
            val deltaMillis = (now - lastElapsed).coerceAtLeast(0)
            lastElapsed = now

            val foregroundPackage = UsagePoller.getCurrentForegroundPackage(this@TrackingService)
            val foregroundApp = MonitoredApp.fromPackageName(foregroundPackage)

            val outcome = tracker.tick(
                state = state,
                foregroundApp = foregroundApp,
                deltaMillis = deltaMillis,
                nowAppDay = DayBoundary.currentAppDay(),
                baselineLookup = { day, app -> repository.getDay(day).perAppMillis[app] ?: 0L },
            )

            outcome.persist?.let { repository.recordForegroundMillis(it.appDay, it.app, it.totalMillis) }

            state = if (outcome.needsDayReload != null) {
                val fresh = repository.getDay(outcome.needsDayReload).perAppMillis
                outcome.newState.copy(dayTotalsMillis = fresh)
            } else {
                outcome.newState
            }

            val live = LiveUsage(
                sessionApp = state.session?.app,
                sessionMillis = state.session?.sessionMillis ?: 0L,
                dayTotalsMillis = state.dayTotalsMillis,
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
