package com.remink.ui.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import com.mudita.mmd.components.time.DatePickerMMD
import com.mudita.mmd.components.time.TimeInputMMD
import com.mudita.mmd.components.time.rememberDatePickerMMDState
import com.mudita.mmd.components.time.rememberTimeInputMMDState
import com.remink.ui.add.AddReminderViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(
    onNavigateUp: () -> Unit,
    viewModel: AddReminderViewModel = hiltViewModel(),
) {
    val formState by viewModel.formState.collectAsState()
    val datePickerState = rememberDatePickerMMDState()
    val timeInputState = rememberTimeInputMMDState()

    LaunchedEffect(formState.isSaved) {
        if (formState.isSaved) onNavigateUp()
    }

    // Recompute scheduledAtMs whenever date or time selection changes
    LaunchedEffect(datePickerState.selectedDateMillis, timeInputState.hour, timeInputState.minute) {
        val dateMs = datePickerState.selectedDateMillis ?: return@LaunchedEffect
        val cal = Calendar.getInstance().apply {
            timeInMillis = dateMs
            set(Calendar.HOUR_OF_DAY, timeInputState.hour)
            set(Calendar.MINUTE, timeInputState.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        viewModel.onScheduledAtChanged(cal.timeInMillis)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButtonMMD(onClick = onNavigateUp) {
                Text("Cancel")
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "New Reminder",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(80.dp))
        }

        Spacer(Modifier.height(24.dp))

        Text("Reminder", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(Modifier.height(8.dp))
        TextFieldMMD(
            value = formState.message,
            onValueChange = { v -> viewModel.onMessageChanged(v) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4,
            placeholder = { Text("What do you need to remember?") },
        )
        Text(
            text = "${formState.message.length} / 200",
            fontSize = 12.sp,
            color = Color.Black,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(24.dp))

        Text("Date", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(Modifier.height(8.dp))
        DatePickerMMD(
            state = datePickerState,
            modifier = Modifier.fillMaxWidth(),
            showModeToggle = true,
        )

        Spacer(Modifier.height(24.dp))

        Text("Time", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(Modifier.height(8.dp))
        TimeInputMMD(state = timeInputState)

        formState.errorMessage?.let { error ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = error,
                fontSize = 14.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(32.dp))

        ButtonMMD(
            onClick = { viewModel.onSaveClicked() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text("Save", fontSize = 18.sp)
        }
    }
}
