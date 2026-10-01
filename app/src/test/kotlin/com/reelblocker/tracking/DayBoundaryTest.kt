package com.reelblocker.tracking

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DayBoundaryTest {

    @Test
    fun `time at or after 6am belongs to that calendar date`() {
        val day = DayBoundary.appDayFor(LocalDateTime.of(2026, 3, 15, 6, 0, 0))
        assertEquals(LocalDate.of(2026, 3, 15), day)
    }

    @Test
    fun `time just before 6am belongs to the previous calendar date`() {
        val day = DayBoundary.appDayFor(LocalDateTime.of(2026, 3, 15, 5, 59, 59))
        assertEquals(LocalDate.of(2026, 3, 14), day)
    }

    @Test
    fun `late night usage at 2am counts toward the day that started the previous morning`() {
        val day = DayBoundary.appDayFor(LocalDateTime.of(2026, 3, 15, 2, 30, 0))
        assertEquals(LocalDate.of(2026, 3, 14), day)
    }

    @Test
    fun `midday is unambiguous`() {
        val day = DayBoundary.appDayFor(LocalDateTime.of(2026, 3, 15, 13, 0, 0))
        assertEquals(LocalDate.of(2026, 3, 15), day)
    }

    @Test
    fun `an app-day runs a full 24 hours from 6am to just before 6am the next day`() {
        val day = LocalDate.of(2026, 3, 15)
        val startMillis = DayBoundary.startOfAppDayMillis(day)
        val endMillis = DayBoundary.endOfAppDayMillis(day)
        val durationMillis = endMillis - startMillis + 1
        assertEquals(24 * 60 * 60 * 1000L, durationMillis)
    }
}
