package com.remink.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remink.alarm.AlarmScheduler
import com.remink.data.model.Reminder
import com.remink.data.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReminderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler,
) : ViewModel() {

    private val reminderId: Long = checkNotNull(savedStateHandle["reminderId"])

    private val _reminder = MutableStateFlow<Reminder?>(null)
    val reminder: StateFlow<Reminder?> = _reminder.asStateFlow()

    private val _isDeleted = MutableStateFlow(false)
    val isDeleted: StateFlow<Boolean> = _isDeleted.asStateFlow()

    init {
        viewModelScope.launch {
            _reminder.value = repository.getById(reminderId)
        }
    }

    fun onDeleteConfirmed() {
        viewModelScope.launch {
            alarmScheduler.cancel(reminderId)
            repository.deleteById(reminderId)
            _isDeleted.value = true
        }
    }
}
