package com.remink

import com.remink.data.model.Reminder
import com.remink.data.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory fake for use in ViewModel unit tests.
 * Not a mock — holds real state so tests can assert on it directly.
 */
class FakeReminderRepository : ReminderRepository {

    val remindersFlow = MutableStateFlow<List<Reminder>>(emptyList())
    val addedReminders = mutableListOf<Reminder>()
    val acknowledgedTimestamps = mutableMapOf<Long, Long>()
    var nextInsertedId: Long = 1L

    fun setReminders(reminders: List<Reminder>) {
        remindersFlow.value = reminders
    }

    override fun getUnacknowledgedReminders(): Flow<List<Reminder>> = remindersFlow

    override suspend fun addReminder(scheduledAt: Long, message: String): Long {
        val id = nextInsertedId++
        val reminder = Reminder(
            id = id,
            scheduledAt = scheduledAt,
            message = message,
            createdAt = 0L,
            acknowledgedAt = null
        )
        addedReminders.add(reminder)
        remindersFlow.value = remindersFlow.value + reminder
        return id
    }

    override suspend fun getById(id: Long): Reminder? =
        remindersFlow.value.firstOrNull { it.id == id }

    override suspend fun deleteById(id: Long) {
        remindersFlow.value = remindersFlow.value.filter { it.id != id }
    }

    override suspend fun markAcknowledged(id: Long) {
        // FakeClock is not injected here; tests that need a real timestamp set it via acknowledgedTimestamps directly.
        // We record a non-zero sentinel so the test can assert acknowledgement happened.
        val timestamp = System.currentTimeMillis()
        acknowledgedTimestamps[id] = timestamp
        remindersFlow.value = remindersFlow.value.map { r ->
            if (r.id == id) r.copy(acknowledgedAt = timestamp) else r
        }
    }

    fun markAcknowledgedAt(id: Long, timestamp: Long) {
        acknowledgedTimestamps[id] = timestamp
        remindersFlow.value = remindersFlow.value.map { r ->
            if (r.id == id) r.copy(acknowledgedAt = timestamp) else r
        }
    }

    override suspend fun getFutureUnacknowledged(): List<Reminder> =
        remindersFlow.value.filter { it.acknowledgedAt == null }
}
