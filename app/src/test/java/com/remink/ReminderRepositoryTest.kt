package com.remink

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.remink.data.db.ReminkDatabase
import com.remink.data.model.Reminder
import com.remink.data.repository.ReminderRepositoryImpl
import com.remink.util.Clock
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ReminderRepositoryTest {

    private lateinit var db: ReminkDatabase
    private lateinit var repository: ReminderRepositoryImpl
    private lateinit var clock: Clock

    private val fixedNow = 1_000_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ReminkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        clock = mockk()
        every { clock.now() } returns fixedNow
        repository = ReminderRepositoryImpl(db.reminderDao(), clock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getAll_returnsEmpty_whenNoRemindersInserted() = runTest {
        val result = repository.getUnacknowledgedReminders().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun insert_thenGetAll_returnsInsertedReminder() = runTest {
        val futureMs = fixedNow + 60_000L
        repository.addReminder(scheduledAt = futureMs, message = "Pick up parcel")

        val result = repository.getUnacknowledgedReminders().first()

        assertEquals(1, result.size)
        assertEquals("Pick up parcel", result[0].message)
        assertEquals(futureMs, result[0].scheduledAt)
    }

    @Test
    fun insert_thenDelete_reminderRemovedFromList() = runTest {
        val id = repository.addReminder(scheduledAt = fixedNow + 60_000L, message = "Temporary")

        repository.deleteById(id)

        val result = repository.getUnacknowledgedReminders().first()
        assertTrue(result.none { it.id == id })
    }

    @Test
    fun markAcknowledged_setsAcknowledgedTrue() = runTest {
        val id = repository.addReminder(scheduledAt = fixedNow + 60_000L, message = "Call back")

        repository.markAcknowledged(id)

        val reminder = repository.getById(id)
        assertNotNull(reminder)
        assertNotNull(reminder!!.acknowledgedAt)
    }

    @Test
    fun getAll_returnsOnlyUnacknowledgedReminders() = runTest {
        val idA = repository.addReminder(scheduledAt = fixedNow + 60_000L, message = "Unacknowledged")
        val idB = repository.addReminder(scheduledAt = fixedNow + 120_000L, message = "Will be acknowledged")

        repository.markAcknowledged(idB)

        val result = repository.getUnacknowledgedReminders().first()
        assertEquals(1, result.size)
        assertEquals(idA, result[0].id)
    }
}
