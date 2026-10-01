package com.reelblocker.data

import com.reelblocker.tracking.MonitoredApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class DayUsageTest {

    private val day = LocalDate.of(2026, 3, 15)

    @Test
    fun `no rows means an empty day with zero total`() {
        val usage = DayUsage.from(day, emptyList())
        assertEquals(0L, usage.totalMillis)
        assertEquals(emptyMap<MonitoredApp, Long>(), usage.perAppMillis)
    }

    @Test
    fun `rows map to their monitored app and sum to the total`() {
        val rows = listOf(
            DailyUsageEntity("2026-03-15", MonitoredApp.INSTAGRAM.packageName, 60_000, 0),
            DailyUsageEntity("2026-03-15", MonitoredApp.YOUTUBE.packageName, 90_000, 0),
        )
        val usage = DayUsage.from(day, rows)

        assertEquals(60_000L, usage.perAppMillis[MonitoredApp.INSTAGRAM])
        assertEquals(90_000L, usage.perAppMillis[MonitoredApp.YOUTUBE])
        assertEquals(150_000L, usage.totalMillis)
    }

    @Test
    fun `a row for an unrecognized package is ignored, not counted or crashed on`() {
        val rows = listOf(
            DailyUsageEntity("2026-03-15", MonitoredApp.INSTAGRAM.packageName, 60_000, 0),
            DailyUsageEntity("2026-03-15", "com.some.unknown.app", 999_000, 0),
        )
        val usage = DayUsage.from(day, rows)

        assertEquals(60_000L, usage.totalMillis)
        assertFalse(usage.perAppMillis.values.contains(999_000L))
    }
}
