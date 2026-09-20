package com.reelblocker.data

import android.content.Context
import com.reelblocker.tracking.DayBoundary
import com.reelblocker.tracking.MonitoredApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Per-app-day, per-monitored-app foreground time, backed by Room. */
class UsageRepository(context: Context) {
    private val dao = AppDatabase.get(context).dailyUsageDao()

    suspend fun recordForegroundMillis(appDay: LocalDate, app: MonitoredApp, foregroundMillis: Long) {
        dao.upsert(
            DailyUsageEntity(
                appDay = appDay.format(ISO),
                packageName = app.packageName,
                foregroundMillis = foregroundMillis,
                updatedAtMillis = System.currentTimeMillis(),
            )
        )
    }

    fun observeDay(day: LocalDate): Flow<DayUsage> =
        dao.observeForDay(day.format(ISO)).map { rows -> DayUsage.from(day, rows) }

    suspend fun getDay(day: LocalDate): DayUsage = DayUsage.from(day, dao.getForDay(day.format(ISO)))

    fun observeRange(start: LocalDate, end: LocalDate): Flow<List<DayUsage>> =
        dao.observeRange(start.format(ISO), end.format(ISO)).map { rows ->
            rows.groupBy { it.appDay }
                .map { (dayString, dayRows) -> DayUsage.from(LocalDate.parse(dayString, ISO), dayRows) }
                .sortedBy { it.day }
        }

    suspend fun getRange(start: LocalDate, end: LocalDate): List<DayUsage> =
        dao.getRange(start.format(ISO), end.format(ISO))
            .groupBy { it.appDay }
            .map { (dayString, dayRows) -> DayUsage.from(LocalDate.parse(dayString, ISO), dayRows) }
            .sortedBy { it.day }

    companion object {
        private val ISO = DateTimeFormatter.ISO_LOCAL_DATE
    }
}

data class DayUsage(
    val day: LocalDate,
    val perAppMillis: Map<MonitoredApp, Long>,
) {
    val totalMillis: Long get() = perAppMillis.values.sum()

    companion object {
        fun from(day: LocalDate, rows: List<DailyUsageEntity>): DayUsage {
            val map = rows.mapNotNull { row ->
                MonitoredApp.fromPackageName(row.packageName)?.let { it to row.foregroundMillis }
            }.toMap()
            return DayUsage(day, map)
        }
    }
}
