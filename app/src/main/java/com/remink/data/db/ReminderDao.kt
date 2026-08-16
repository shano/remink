package com.remink.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.remink.data.model.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Insert
    suspend fun insert(reminder: Reminder): Long

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM reminders WHERE acknowledged_at IS NULL ORDER BY scheduled_at ASC")
    fun getAllUnacknowledged(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): Reminder?

    @Query("UPDATE reminders SET acknowledged_at = :timestamp WHERE id = :id")
    suspend fun markAcknowledged(id: Long, timestamp: Long)

    @Query("SELECT * FROM reminders WHERE acknowledged_at IS NULL AND scheduled_at > :now")
    suspend fun getFutureUnacknowledged(now: Long): List<Reminder>
}
