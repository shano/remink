package com.remink.data.repository

import com.remink.data.model.Reminder
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun getUnacknowledgedReminders(): Flow<List<Reminder>>
    suspend fun addReminder(scheduledAt: Long, message: String): Long
    suspend fun getById(id: Long): Reminder?
    suspend fun deleteById(id: Long)
    suspend fun markAcknowledged(id: Long)
    suspend fun getFutureUnacknowledged(): List<Reminder>
}
