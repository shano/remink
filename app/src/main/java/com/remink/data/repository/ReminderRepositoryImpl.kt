package com.remink.data.repository

import com.remink.data.db.ReminderDao
import com.remink.data.model.Reminder
import com.remink.util.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ReminderRepositoryImpl @Inject constructor(
    private val dao: ReminderDao,
    private val clock: Clock,
) : ReminderRepository {

    override fun getUnacknowledgedReminders(): Flow<List<Reminder>> =
        dao.getAllUnacknowledged()

    override suspend fun addReminder(scheduledAt: Long, message: String): Long =
        withContext(Dispatchers.IO) {
            val reminder = Reminder(
                scheduledAt = scheduledAt,
                message = message,
                createdAt = clock.now(),
            )
            dao.insert(reminder)
        }

    override suspend fun getById(id: Long): Reminder? =
        withContext(Dispatchers.IO) { dao.getById(id) }

    override suspend fun deleteById(id: Long) =
        withContext(Dispatchers.IO) { dao.deleteById(id) }

    override suspend fun markAcknowledged(id: Long) =
        withContext(Dispatchers.IO) { dao.markAcknowledged(id, clock.now()) }

    override suspend fun getFutureUnacknowledged(): List<Reminder> =
        withContext(Dispatchers.IO) { dao.getFutureUnacknowledged(clock.now()) }
}
