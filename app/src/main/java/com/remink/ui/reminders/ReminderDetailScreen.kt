package com.remink.ui.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.remink.ui.detail.ReminderDetailViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val detailDateFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy · HH:mm", Locale.getDefault())

@Composable
fun ReminderDetailScreen(
    reminderId: Long,
    onNavigateUp: () -> Unit,
    viewModel: ReminderDetailViewModel = hiltViewModel(),
) {
    val reminder by viewModel.reminder.collectAsState()
    val isDeleted by viewModel.isDeleted.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isDeleted) {
        if (isDeleted) onNavigateUp()
    }

    val now = System.currentTimeMillis()
    val isOverdue = reminder?.let {
        it.scheduledAt < now && it.acknowledgedAt == null
    } ?: false

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onNavigateUp) {
                Text("Back", color = Color.Black, fontSize = 16.sp)
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "Reminder",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "Delete",
                fontSize = 16.sp,
                color = Color.Black,
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { showDeleteDialog = true },
                    )
                    .padding(8.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        if (isOverdue) {
            Text(
                text = "Overdue",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        reminder?.let { r ->
            val formattedDate = Instant.ofEpochMilli(r.scheduledAt)
                .atZone(ZoneId.systemDefault())
                .format(detailDateFormatter)

            Text(
                text = formattedDate,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = r.message,
                fontSize = 18.sp,
                color = Color.Black,
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this reminder?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.onDeleteConfirmed()
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
