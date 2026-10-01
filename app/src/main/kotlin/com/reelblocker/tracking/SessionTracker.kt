package com.reelblocker.tracking

import java.time.LocalDate

/**
 * Pure state machine for the session/day-boundary tracking logic - no Android/Service
 * dependencies, so it's fully unit-testable on the JVM. [com.reelblocker.tracking.TrackingService]
 * is a thin wrapper that feeds it ticks and does the actual DB I/O [TickOutcome] asks for.
 *
 * The day-boundary contract this implements (see DayBoundary): if a session is already running
 * when the app-day rolls over (e.g. you're still in Instagram at 6am), it keeps crediting the
 * *old* day until that session ends - the next session after that starts crediting the new day.
 */
class SessionTracker {

    data class Session(
        val app: MonitoredApp,
        val appDay: LocalDate,
        val baselineMillis: Long,
        val sessionMillis: Long = 0L,
    )

    data class State(
        val trackedDay: LocalDate,
        val dayTotalsMillis: Map<MonitoredApp, Long>,
        val session: Session?,
    )

    data class Persist(val appDay: LocalDate, val app: MonitoredApp, val totalMillis: Long)

    data class TickOutcome(
        val newState: State,
        /** Non-null when there's a (day, app, total) that should be upserted into the DB this tick. */
        val persist: Persist?,
        /**
         * Non-null when the tracked day just rolled over and the caller must re-fetch that day's
         * totals from the DB and fold them into [newState] before the next tick (the fresh day
         * may already have a persisted total if [persist] fired for it in this same tick).
         */
        val needsDayReload: LocalDate?,
    )

    suspend fun tick(
        state: State,
        foregroundApp: MonitoredApp?,
        deltaMillis: Long,
        nowAppDay: LocalDate,
        baselineLookup: suspend (LocalDate, MonitoredApp) -> Long,
    ): TickOutcome {
        val previous = state.session
        val session = when {
            foregroundApp == null -> null
            previous == null || previous.app != foregroundApp ->
                Session(foregroundApp, nowAppDay, baselineLookup(nowAppDay, foregroundApp))
            else -> previous.copy(sessionMillis = previous.sessionMillis + deltaMillis)
        }

        var dayTotals = state.dayTotalsMillis
        var persist: Persist? = null
        if (session != null) {
            val total = session.baselineMillis + session.sessionMillis
            persist = Persist(session.appDay, session.app, total)
            if (session.appDay == state.trackedDay) {
                dayTotals = dayTotals + (session.app to total)
            }
        }

        var trackedDay = state.trackedDay
        var needsReload: LocalDate? = null
        if (nowAppDay != trackedDay && (session == null || session.appDay == nowAppDay)) {
            trackedDay = nowAppDay
            needsReload = nowAppDay
            dayTotals = emptyMap()
        }

        return TickOutcome(
            newState = State(trackedDay = trackedDay, dayTotalsMillis = dayTotals, session = session),
            persist = persist,
            needsDayReload = needsReload,
        )
    }
}
