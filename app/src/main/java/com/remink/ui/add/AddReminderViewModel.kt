package com.remink.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remink.alarm.AlarmScheduler
import com.remink.data.model.ReminderValidation
import com.remink.data.repository.ReminderRepository
import com.remink.util.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddReminderFormState(
    val message: String = "",
    val scheduledAtMs: Long? = null,
    val errorMessage: String? = null,
    val isSaved: Boolean = false,
)

@HiltViewModel
class AddReminderViewModel @Inject constructor(
    private val repository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler,
    private val clock: Clock,
) : ViewModel() {

    private val _formState = MutableStateFlow(AddReminderFormState())
    val formState: StateFlow<AddReminderFormState> = _formState.asStateFlow()

    fun onMessageChanged(text: String) {
        val capped = if (text.length > 200) text.substring(0, 200) else text
        _formState.update { it.copy(message = capped, errorMessage = null) }
    }

    fun onScheduledAtChanged(scheduledAtMs: Long) {
        _formState.update { it.copy(scheduledAtMs = scheduledAtMs, errorMessage = null) }
    }

    fun onSaveClicked() {
        val state = _formState.value
        val scheduledAt = state.scheduledAtMs

        if (scheduledAt == null || !ReminderValidation.isScheduledTimeValid(scheduledAt, clock.now())) {
            _formState.update { it.copy(errorMessage = "Scheduled time has already passed.") }
            return
        }
        if (!ReminderValidation.isMessageValid(state.message)) {
            _formState.update { it.copy(errorMessage = "Please enter a valid reminder message.") }
            return
        }

        viewModelScope.launch {
            val id = repository.addReminder(scheduledAt, state.message)
            alarmScheduler.schedule(id, scheduledAt)
            _formState.update { it.copy(isSaved = true) }
        }
    }
}
