package com.andchad.habit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andchad.habit.data.model.HabitHistory
import com.andchad.habit.data.model.HabitStatus
import com.andchad.habit.ui.HabitViewModel
import com.andchad.habit.ui.screens.components.DeleteConfirmationDialog
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    habitHistory: List<HabitHistory>,
    habitNameMap: Map<String, String>, // Map of habit IDs to names
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier
) {
    // State for date selection - Default to today but never allow future dates
    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }
    var showCalendar by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Group history items by date
    val groupedHistory by remember(habitHistory, selectedDate) {
        derivedStateOf {
            // Convert selected date to start and end of day in milliseconds
            val startOfDay = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endOfDay = selectedDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

            // Filter history for selected date and group
            habitHistory
                .filter { it.date in startOfDay..endOfDay }
                .groupBy {
                    // Convert timestamp to LocalDate for display
                    Instant.ofEpochMilli(it.date)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                }
        }
    }

    if (showDeleteConfirmation) {
        DeleteConfirmationDialog(
            title = "Delete All History",
            message = "Are you sure you want to delete all habit history? This action cannot be undone.",
            onConfirm = {
                viewModel.clearAllHabitHistory()
                showDeleteConfirmation = false
            },
            onDismiss = {
                showDeleteConfirmation = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Habit History") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // Add Delete All button
                    IconButton(
                        onClick = { showDeleteConfirmation = true },
                        enabled = habitHistory.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete All History",
                            tint = if (habitHistory.isNotEmpty())
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Date selection header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = selectedDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Button(
                        onClick = { showCalendar = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Select Date",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Date")
                    }
                }
            }

            // Delete all history button
            if (habitHistory.isNotEmpty()) {
                FilledTonalButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete All History",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Clear All History")
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Quick date navigation - only show "Yesterday" and "Today" options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Previous Day Button
                OutlinedCard(
                    modifier = Modifier
                        .padding(4.dp)
                        .clickable {
                            // Only allow navigating to past dates, not future
                            val prevDay = selectedDate.minusDays(1)
                            selectedDate = prevDay
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Previous Day",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Previous Day",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Today button (only enabled if not already viewing today)
                OutlinedCard(
                    modifier = Modifier
                        .padding(4.dp)
                        .clickable(enabled = selectedDate != today) { selectedDate = today }
                ) {
                    Text(
                        text = "Today",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedDate == today)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // History content
            if (groupedHistory.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No habit records for this date",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedHistory.forEach { (date, historyItems) ->
                        // Date header
                        item {
                            Text(
                                text = date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        // Completed habits
                        val completedItems = historyItems.filter { it.status == HabitStatus.COMPLETED }
                        if (completedItems.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Completed",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }

                            items(completedItems) { historyItem ->
                                HabitHistoryItem(
                                    habitName = habitNameMap[historyItem.habitId] ?: "Unknown Habit",
                                    status = HabitStatus.COMPLETED,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }

                        // Missed habits
                        val missedItems = historyItems.filter { it.status == HabitStatus.MISSED }
                        if (missedItems.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Missed",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }

                            items(missedItems) { historyItem ->
                                HabitHistoryItem(
                                    habitName = habitNameMap[historyItem.habitId] ?: "Unknown Habit",
                                    status = HabitStatus.MISSED,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Calendar dialog
    if (showCalendar) {
        PastDatePickerDialog(
            initialDate = selectedDate,
            maxDate = today, // Don't allow selecting future dates
            onDateSelected = {
                selectedDate = it
                showCalendar = false
            },
            onDismiss = { showCalendar = false }
        )
    }
}

@Composable
fun HabitHistoryItem(
    habitName: String,
    status: HabitStatus,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when (status) {
                            HabitStatus.COMPLETED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            HabitStatus.MISSED -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (status) {
                        HabitStatus.COMPLETED -> Icons.Default.Check
                        HabitStatus.MISSED -> Icons.Default.Close
                    },
                    contentDescription = status.name,
                    tint = when (status) {
                        HabitStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                        HabitStatus.MISSED -> MaterialTheme.colorScheme.error
                    }
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = habitName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PastDatePickerDialog(
    initialDate: LocalDate,
    maxDate: LocalDate, // Maximum date (today)
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    var selectedDate by remember { mutableStateOf(initialDate) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Select Date")

                // Month navigation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous month button
                    IconButton(
                        onClick = { currentMonth = currentMonth.minusMonths(1) }
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Previous Month")
                    }

                    // Current month display
                    Text(
                        text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        style = MaterialTheme.typography.titleMedium
                    )

                    // Next month button (disabled if would go past today)
                    IconButton(
                        onClick = {
                            val nextMonth = currentMonth.plusMonths(1)
                            if (!nextMonth.isAfter(YearMonth.from(maxDate))) {
                                currentMonth = nextMonth
                            }
                        },
                        enabled = !currentMonth.plusMonths(1).isAfter(YearMonth.from(maxDate))
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Next Month",
                            tint = if (!currentMonth.plusMonths(1).isAfter(YearMonth.from(maxDate)))
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            }
        },
        text = {
            Column {
                // Day of week headers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val daysOfWeek = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
                    daysOfWeek.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Calendar grid
                val firstDayOfMonth = currentMonth.atDay(1)
                val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // 0 = Sunday, 6 = Saturday
                val daysInMonth = currentMonth.lengthOfMonth()

                // Calculate rows needed (maximum of 6 rows)
                val rows = ((firstDayOfWeek + daysInMonth - 1) / 7) + 1

                Column {
                    for (row in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (column in 0..6) {
                                val day = (row * 7 + column) - firstDayOfWeek + 1

                                if (day in 1..daysInMonth) {
                                    val date = currentMonth.atDay(day)
                                    val isSelected = date == selectedDate
                                    val isToday = date == maxDate
                                    val isPast = !date.isAfter(maxDate)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(4.dp)
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable(enabled = isPast) {
                                                selectedDate = date
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day.toString(),
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                                !isPast -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                } else {
                                    // Empty space for days not in this month
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onDateSelected(selectedDate) }
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun IconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}