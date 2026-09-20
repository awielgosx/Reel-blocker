package com.reelblocker.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyUsageDao {

    @Upsert
    suspend fun upsert(entity: DailyUsageEntity)

    @Query("SELECT * FROM daily_usage WHERE appDay = :appDay")
    fun observeForDay(appDay: String): Flow<List<DailyUsageEntity>>

    @Query("SELECT * FROM daily_usage WHERE appDay = :appDay")
    suspend fun getForDay(appDay: String): List<DailyUsageEntity>

    @Query("SELECT * FROM daily_usage WHERE appDay BETWEEN :startDay AND :endDay ORDER BY appDay ASC")
    fun observeRange(startDay: String, endDay: String): Flow<List<DailyUsageEntity>>

    @Query("SELECT * FROM daily_usage WHERE appDay BETWEEN :startDay AND :endDay ORDER BY appDay ASC")
    suspend fun getRange(startDay: String, endDay: String): List<DailyUsageEntity>
}
