package com.example.fitsforyou.database

import androidx.room.*
import com.example.fitsforyou.model.WearEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface WearEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: WearEvent)

    @Query("SELECT COUNT(*) FROM wear_events WHERE userId = :userId AND timestamp >= :startTime AND timestamp < :endTime")
    fun countWornEventsInRange(userId: String, startTime: Long, endTime: Long): Flow<Int>

    @Query("SELECT MAX(timestamp) FROM wear_events WHERE userId = :userId AND itemId = :itemId")
    suspend fun getLastWearTimestamp(userId: String, itemId: Int): Long?
}
