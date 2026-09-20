package com.reelblocker.tracking

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * The user's "day" runs 6:00am to 5:59:59.999am the next calendar day, not midnight to
 * midnight - most late-night usage happens before the user's day has actually "started".
 */
object DayBoundary {
    val DAY_START: LocalTime = LocalTime.of(6, 0)

    fun appDayFor(dateTime: LocalDateTime): LocalDate {
        return if (dateTime.toLocalTime().isBefore(DAY_START)) {
            dateTime.toLocalDate().minusDays(1)
        } else {
            dateTime.toLocalDate()
        }
    }

    fun appDayFor(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate {
        return appDayFor(LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), zone))
    }

    fun currentAppDay(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        appDayFor(LocalDateTime.now(zone))

    fun startOfAppDayMillis(day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        day.atTime(DAY_START).atZone(zone).toInstant().toEpochMilli()

    fun endOfAppDayMillis(day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        day.plusDays(1).atTime(DAY_START).atZone(zone).toInstant().toEpochMilli() - 1
}
