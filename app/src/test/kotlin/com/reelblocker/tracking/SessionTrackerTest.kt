package com.reelblocker.tracking

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class SessionTrackerTest {

    private val day1 = LocalDate.of(2026, 3, 15)
    private val day2 = LocalDate.of(2026, 3, 16)
    private val emptyState = SessionTracker.State(trackedDay = day1, dayTotalsMillis = emptyMap(), session = null)
    private val zeroBaseline: suspend (LocalDate, MonitoredApp) -> Long = { _, _ -> 0L }

    @Test
    fun `opening a tracked app for the first time starts a session at the DB baseline`() = runTest {
        val tracker = SessionTracker()
        val outcome = tracker.tick(
            state = emptyState,
            foregroundApp = MonitoredApp.INSTAGRAM,
            deltaMillis = 5_000,
            nowAppDay = day1,
            baselineLookup = { _, _ -> 120_000L },
        )

        assertEquals(MonitoredApp.INSTAGRAM, outcome.newState.session?.app)
        assertEquals(0L, outcome.newState.session?.sessionMillis) // session timer starts at 0, not the baseline
        assertEquals(120_000L, outcome.persist?.totalMillis) // but the persisted DB total includes the baseline
        assertEquals(120_000L, outcome.newState.dayTotalsMillis[MonitoredApp.INSTAGRAM])
    }

    @Test
    fun `staying in the same app accumulates session time across ticks`() = runTest {
        val tracker = SessionTracker()
        var state = emptyState
        state = tracker.tick(state, MonitoredApp.YOUTUBE, 5_000, day1, zeroBaseline).newState
        val second = tracker.tick(state, MonitoredApp.YOUTUBE, 5_000, day1, zeroBaseline)
        val third = tracker.tick(second.newState, MonitoredApp.YOUTUBE, 5_000, day1, zeroBaseline)

        // The tick that first detects the app contributes 0 (nothing elapsed "in app" yet);
        // accumulation starts from the next tick onward.
        assertEquals(5_000L, second.newState.session?.sessionMillis)
        assertEquals(10_000L, third.newState.session?.sessionMillis)
        assertEquals(10_000L, third.persist?.totalMillis)
        assertEquals(10_000L, third.newState.dayTotalsMillis[MonitoredApp.YOUTUBE])
    }

    @Test
    fun `switching to a different tracked app starts a fresh session, not a continuation`() = runTest {
        val tracker = SessionTracker()
        var state = tracker.tick(emptyState, MonitoredApp.INSTAGRAM, 5_000, day1, zeroBaseline).newState
        val switched = tracker.tick(state, MonitoredApp.FACEBOOK, 5_000, day1) { _, app ->
            if (app == MonitoredApp.FACEBOOK) 30_000L else 0L
        }

        assertEquals(MonitoredApp.FACEBOOK, switched.newState.session?.app)
        assertEquals(0L, switched.newState.session?.sessionMillis)
        assertEquals(30_000L, switched.persist?.totalMillis)
    }

    @Test
    fun `leaving every tracked app ends the session`() = runTest {
        val tracker = SessionTracker()
        val withSession = tracker.tick(emptyState, MonitoredApp.INSTAGRAM, 5_000, day1, zeroBaseline).newState
        val afterLeaving = tracker.tick(withSession, null, 5_000, day1, zeroBaseline)

        assertNull(afterLeaving.newState.session)
        assertNull(afterLeaving.persist)
    }

    @Test
    fun `day rolls over immediately when no session is in progress`() = runTest {
        val tracker = SessionTracker()
        val outcome = tracker.tick(emptyState, null, 5_000, day2, zeroBaseline)

        assertEquals(day2, outcome.newState.trackedDay)
        assertEquals(day2, outcome.needsDayReload)
        assertTrue(outcome.newState.dayTotalsMillis.isEmpty())
    }

    @Test
    fun `a session running across 6am keeps crediting the OLD day until it ends`() = runTest {
        val tracker = SessionTracker()
        // Session starts on day1, well before the boundary.
        var state = tracker.tick(emptyState, MonitoredApp.INSTAGRAM, 5_000, day1, zeroBaseline).newState
        assertEquals(day1, state.trackedDay)

        // Clock crosses into day2, but the SAME session (same app, never left foreground) is still running.
        val stillRunning = tracker.tick(state, MonitoredApp.INSTAGRAM, 60_000, day2, zeroBaseline)

        // The tracked "today" must NOT roll over yet - the in-progress session still belongs to day1.
        assertEquals(day1, stillRunning.newState.trackedDay)
        assertNull(stillRunning.needsDayReload)
        assertEquals(day1, stillRunning.newState.session?.appDay)
        assertEquals(60_000L, stillRunning.persist?.totalMillis)
        assertEquals(day1, stillRunning.persist?.appDay)
    }

    @Test
    fun `the next session after a cross-boundary session ends rolls over to the new day`() = runTest {
        val tracker = SessionTracker()
        var state = tracker.tick(emptyState, MonitoredApp.INSTAGRAM, 5_000, day1, zeroBaseline).newState
        state = tracker.tick(state, MonitoredApp.INSTAGRAM, 60_000, day2, zeroBaseline).newState // still day1, crossing midnight

        // User leaves Instagram - nothing running, day boundary already in the past.
        val leftApp = tracker.tick(state, null, 5_000, day2, zeroBaseline)
        assertEquals(day2, leftApp.newState.trackedDay)
        assertEquals(day2, leftApp.needsDayReload)

        // The next tracked app opened now correctly starts crediting the new day.
        val newSession = tracker.tick(leftApp.newState, MonitoredApp.YOUTUBE, 5_000, day2, zeroBaseline)
        assertEquals(day2, newSession.newState.session?.appDay)
        assertFalse(newSession.newState.dayTotalsMillis.containsKey(MonitoredApp.INSTAGRAM))
    }

    @Test
    fun `day rolls over in the same tick a new session starts under the new day`() = runTest {
        val tracker = SessionTracker()
        // No session running, but the foreground app changes to a tracked app in the very same
        // tick the day boundary is crossed (e.g. the app was opened right at 6:00:00am).
        val outcome = tracker.tick(emptyState, MonitoredApp.YOUTUBE, 5_000, day2) { _, _ -> 0L }

        assertEquals(day2, outcome.newState.trackedDay)
        assertEquals(day2, outcome.needsDayReload)
        assertEquals(day2, outcome.newState.session?.appDay)
    }
}
