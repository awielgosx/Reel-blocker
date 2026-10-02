package com.reelblocker.tracking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Edge cases the original test pass didn't cover, flagged during a correctness review: DST
 * transitions and cross-timezone attribution. [DayBoundary] trusts the device's system clock -
 * there's no defense against a user manually changing the date/time (not a meaningful threat for
 * a single trusted personal device), but DST and timezone changes are real scenarios (daylight
 * saving twice a year, travel) and must not corrupt day attribution.
 */
class DayBoundaryDstTest {

    @Test
    fun `day attribution stays correct across a real DST transition`() {
        val zone = ZoneId.of("America/New_York")
        // Find the next real DST transition in this zone after a fixed point, rather than
        // hardcoding a date that could drift with calendar rule changes.
        val transition = zone.rules.nextTransition(Instant.parse("2026-01-01T00:00:00Z"))
        val transitionDate = LocalDateTime.ofInstant(transition.instant, zone).toLocalDate()

        val justBeforeBoundary = LocalDateTime.of(transitionDate, LocalTime.of(5, 59, 59))
        assertEquals(transitionDate.minusDays(1), DayBoundary.appDayFor(justBeforeBoundary))

        val atBoundary = LocalDateTime.of(transitionDate, LocalTime.of(6, 0))
        assertEquals(transitionDate, DayBoundary.appDayFor(atBoundary))

        // The app-day's millis range must still be well-formed even though the transition shifts
        // the real UTC offset partway through this calendar day (the day may be 23 or 25 real
        // hours long, not exactly 24 - that's correct, just confirming nothing breaks).
        val start = DayBoundary.startOfAppDayMillis(transitionDate, zone)
        val end = DayBoundary.endOfAppDayMillis(transitionDate, zone)
        assertTrue(end > start)
    }

    @Test
    fun `the same instant can map to a different app-day in a different device timezone`() {
        // Not a bug - the "day" is always relative to wherever the device currently thinks it is,
        // which is what a traveling user would expect ("my day" follows local time).
        val instant = LocalDateTime.of(2026, 6, 15, 20, 0).atZone(ZoneId.of("America/Los_Angeles")).toInstant()

        val dayInLA = DayBoundary.appDayFor(instant.toEpochMilli(), ZoneId.of("America/Los_Angeles"))
        val dayInTokyo = DayBoundary.appDayFor(instant.toEpochMilli(), ZoneId.of("Asia/Tokyo"))

        assertNotEquals(dayInLA, dayInTokyo)
    }
}
