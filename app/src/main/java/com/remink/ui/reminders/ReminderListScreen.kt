package com.remink.ui.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.FloatingActionButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.remink.data.model.Reminder
import com.remink.ui.list.ReminderListViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val rowDateFormatter = DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", Locale.getDefault())

@Composable
fun ReminderListScreen(
    onAddClick: () -> Unit,
    onRowClick: (Long) -> Unit,
    viewModel: ReminderListViewModel = hiltViewModel(),
) {
    val reminders by viewModel.reminders.collectAsState()
    val now = System.currentTimeMillis()

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Reminders",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            )

            if (reminders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No reminders set",
                        fontSize = 18.sp,
                        color = Color.Black,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(reminders, key = { it.id }) { reminder ->
                        ReminderRow(
                            reminder = reminder,
                            isOverdue = reminder.scheduledAt < now && reminder.acknowledgedAt == null,
                            onClick = { onRowClick(reminder.id) },
                        )
                    }
                }
            }
        }

        FloatingActionButtonMMD(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
        ) {
            Text(text = "+", fontSize = 28.sp)
        }
    }
}

@Composable
private fun ReminderRow(
    reminder: Reminder,
    isOverdue: Boolean,
    onClick: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val bgColor = if (isOverdue) Color.Black else Color.White
    val textColor = if (isOverdue) Color.White else Color.Black

    val dateTime = Instant.ofEpochMilli(reminder.scheduledAt)
        .atZone(ZoneId.systemDefault())
    val formattedDate = dateTime.format(rowDateFormatter)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(bgColor)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = formattedDate,
                fontSize = 14.sp,
                color = textColor,
                fontWeight = FontWeight.Normal,
            )
            Text(
                text = reminder.message,
                fontSize = 16.sp,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete reminder?") },
            confirmButton = {
                ButtonMMD(onClick = { showDeleteDialog = false }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButtonMMD(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
