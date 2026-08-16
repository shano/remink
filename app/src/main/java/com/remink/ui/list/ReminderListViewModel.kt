package com.remink.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remink.data.model.Reminder
import com.remink.data.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ReminderListViewModel @Inject constructor(
    repository: ReminderRepository,
) : ViewModel() {

    val reminders: StateFlow<List<Reminder>> = repository
        .getUnacknowledgedReminders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )
}
