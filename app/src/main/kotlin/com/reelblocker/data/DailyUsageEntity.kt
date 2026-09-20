package com.reelblocker.data

import androidx.room.Entity

/**
 * One row per (app-day, monitored app). appDay is an ISO-8601 date string (yyyy-MM-dd) as
 * produced by [com.reelblocker.tracking.DayBoundary] - not a calendar/midnight day.
 */
@Entity(tableName = "daily_usage", primaryKeys = ["appDay", "packageName"])
data class DailyUsageEntity(
    val appDay: String,
    val packageName: String,
    val foregroundMillis: Long,
    val updatedAtMillis: Long,
)
