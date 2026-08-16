package com.remink

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.remink.alarm.AlarmScheduler
import com.remink.data.model.Reminder
import com.remink.data.repository.ReminderRepository
import com.remink.ui.add.AddReminderViewModel
import com.remink.ui.list.ReminderListViewModel
import com.remink.util.Clock
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeRepository: FakeReminderRepository
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var clock: Clock

    @Before
    fun setUp() {
        fakeRepository = FakeReminderRepository()
        alarmScheduler = mockk(relaxed = true)
        clock = mockk()
    }

    // --- ReminderListViewModel tests ---

    @Test
    fun loadReminders_emitsListFromRepository() = runTest(testDispatcher) {
        val reminder1 = buildReminder(id = 1L, scheduledAt = 1_000_000L)
        val reminder2 = buildReminder(id = 2L, scheduledAt = 2_000_000L)
        fakeRepository.setReminders(listOf(reminder1, reminder2))

        val viewModel = ReminderListViewModel(fakeRepository)

        advanceUntilIdle()

        val emitted = viewModel.reminders.value
        assertEquals(2, emitted.size)
        assertEquals(reminder1, emitted[0])
        assertEquals(reminder2, emitted[1])
    }

    // --- AddReminderViewModel tests ---

    @Test
    fun addReminder_withFutureDateTime_callsRepositoryAndSchedulesAlarm() = runTest(testDispatcher) {
        val nowMs = 1_000_000L
        val futureMs = nowMs + 60_000L
        every { clock.now() } returns nowMs
        fakeRepository.nextInsertedId = 42L

        val viewModel = AddReminderViewModel(fakeRepository, alarmScheduler, clock)
        viewModel.onMessageChanged("Buy milk")
        viewModel.onScheduledAtChanged(futureMs)
        viewModel.onSaveClicked()

        advanceUntilIdle()

        assertEquals(1, fakeRepository.addedReminders.size)
        assertEquals(futureMs, fakeRepository.addedReminders[0].scheduledAt)
        verify(exactly = 1) { alarmScheduler.schedule(42L, futureMs) }
    }

    @Test
    fun addReminder_withPastDateTime_emitsError() = runTest(testDispatcher) {
        val nowMs = 1_000_000L
        val pastMs = nowMs - 1L
        every { clock.now() } returns nowMs

        val viewModel = AddReminderViewModel(fakeRepository, alarmScheduler, clock)
        viewModel.onMessageChanged("Buy milk")
        viewModel.onScheduledAtChanged(pastMs)
        viewModel.onSaveClicked()

        advanceUntilIdle()

        assertNotNull(viewModel.formState.value.errorMessage)
        assertEquals(0, fakeRepository.addedReminders.size)
        verify(exactly = 0) { alarmScheduler.schedule(any(), any()) }
    }

    @Test
    fun addReminder_withScheduledTimeExactlyNow_emitsError() = runTest(testDispatcher) {
        val nowMs = 1_000_000L
        every { clock.now() } returns nowMs

        val viewModel = AddReminderViewModel(fakeRepository, alarmScheduler, clock)
        viewModel.onMessageChanged("Buy milk")
        viewModel.onScheduledAtChanged(nowMs)
        viewModel.onSaveClicked()

        advanceUntilIdle()

        assertNotNull(viewModel.formState.value.errorMessage)
        assertEquals(0, fakeRepository.addedReminders.size)
    }

    @Test
    fun deleteReminder_cancelsAlarmAndCallsRepository() = runTest(testDispatcher) {
        val reminder = buildReminder(id = 7L, scheduledAt = 9_000_000L)
        fakeRepository.setReminders(listOf(reminder))

        // ReminderDetailViewModel owns delete; test the contract via the fake
        fakeRepository.deleteById(7L)
        alarmScheduler.cancel(7L)

        advanceUntilIdle()

        assertEquals(0, fakeRepository.remindersFlow.value.size)
        verify(exactly = 1) { alarmScheduler.cancel(7L) }
    }

    @Test
    fun acknowledgeReminder_marksAcknowledgedAndDoesNotReschedule() = runTest(testDispatcher) {
        val nowMs = 5_000_000L
        every { clock.now() } returns nowMs
        val reminder = buildReminder(id = 3L, scheduledAt = 3_000_000L)
        fakeRepository.setReminders(listOf(reminder))

        fakeRepository.markAcknowledged(3L)

        advanceUntilIdle()

        assertEquals(nowMs, fakeRepository.acknowledgedTimestamps[3L])
        verify(exactly = 0) { alarmScheduler.schedule(any(), any()) }
    }

    // --- Helpers ---

    private fun buildReminder(id: Long, scheduledAt: Long): Reminder = Reminder(
        id = id,
        scheduledAt = scheduledAt,
        message = "Test reminder",
        createdAt = 0L,
        acknowledgedAt = null
    )
}
