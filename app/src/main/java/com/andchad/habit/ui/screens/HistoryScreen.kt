package com.andchad.habit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andchad.habit.data.model.HabitHistory
import com.andchad.habit.data.model.HabitStatus
import com.andchad.habit.ui.HabitViewModel
import com.andchad.habit.ui.components.ModernTopAppBar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    habitHistory: List<HabitHistory>,
    habitNameMap: Map<String, String>,
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier
) {
    var showFilterSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Filter states
    var searchQuery by remember { mutableStateOf("") }
    var selectedDateRange by remember { mutableStateOf(DateRange.TODAY) }
    var selectedStatus by remember { mutableStateOf<HabitStatus?>(null) }

    // Date range for filtering
    val today = LocalDate.now()
    val startDate = when (selectedDateRange) {
        DateRange.TODAY -> today
        DateRange.YESTERDAY -> today.minusDays(1)
        DateRange.LAST_7_DAYS -> today.minusDays(6)
        DateRange.THIS_MONTH -> today.withDayOfMonth(1)
        DateRange.CUSTOM -> today.minusDays(7) // Default to last 7 days for custom
    }
    val endDate = when (selectedDateRange) {
        DateRange.TODAY -> today
        DateRange.YESTERDAY -> today.minusDays(1)
        DateRange.LAST_7_DAYS -> today
        DateRange.THIS_MONTH -> today
        DateRange.CUSTOM -> today
    }

    // Apply filters
    val filteredHistory = habitHistory.filter { history ->
        val historyDate = Instant.ofEpochMilli(history.date)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        val matchesDateRange = !historyDate.isBefore(startDate) && !historyDate.isAfter(endDate)
        val matchesStatus = selectedStatus == null || history.status == selectedStatus

        val habitName = habitNameMap[history.habitId] ?: ""
        val matchesSearch = searchQuery.isEmpty() ||
                habitName.contains(searchQuery, ignoreCase = true)

        matchesDateRange && matchesStatus && matchesSearch
    }

    // Group by date for display
    val groupedHistory = filteredHistory.groupBy { history ->
        Instant.ofEpochMilli(history.date)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }.entries.sortedByDescending { it.key }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Clear History") },
            text = { Text("Are you sure you want to delete all habit history? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHabitHistory()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            ModernTopAppBar(
                title = "Habit History"
                // No delete button in top bar
            )
        },
        floatingActionButton = {
            // Filter FAB on the right
            FloatingActionButton(
                onClick = { showFilterSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter History"
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("Search habits...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // Filters chips row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedStatus == HabitStatus.COMPLETED,
                        onClick = {
                            selectedStatus = if (selectedStatus == HabitStatus.COMPLETED)
                                null else HabitStatus.COMPLETED
                        },
                        label = { Text("Completed") },
                        leadingIcon = if (selectedStatus == HabitStatus.COMPLETED) {
                            { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                        } else null
                    )

                    FilterChip(
                        selected = selectedStatus == HabitStatus.MISSED,
                        onClick = {
                            selectedStatus = if (selectedStatus == HabitStatus.MISSED)
                                null else HabitStatus.MISSED
                        },
                        label = { Text("Missed") },
                        leadingIcon = if (selectedStatus == HabitStatus.MISSED) {
                            { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                        } else null
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    AssistChip(
                        onClick = { showFilterSheet = true },
                        label = { Text(selectedDateRange.label) },
                        leadingIcon = { Icon(Icons.Default.DateRange, null, Modifier.size(16.dp)) }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                if (groupedHistory.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No habit records found",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp, start = 16.dp, end = 16.dp)
                    ) {
                        groupedHistory.forEach { (date, historyItems) ->
                            item {
                                Text(
                                    text = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }

                            items(historyItems) { item ->
                                HabitHistoryCard(
                                    habitName = habitNameMap[item.habitId] ?: "Unknown Habit",
                                    status = item.status,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Delete FAB (positioned on the left side)
            if (habitHistory.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showDeleteDialog = true },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear All History"
                    )
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Filter by Date",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                DateRangeOption(
                    title = "Today",
                    selected = selectedDateRange == DateRange.TODAY,
                    onClick = {
                        selectedDateRange = DateRange.TODAY
                        showFilterSheet = false
                    }
                )

                DateRangeOption(
                    title = "Yesterday",
                    selected = selectedDateRange == DateRange.YESTERDAY,
                    onClick = {
                        selectedDateRange = DateRange.YESTERDAY
                        showFilterSheet = false
                    }
                )

                DateRangeOption(
                    title = "Last 7 days",
                    selected = selectedDateRange == DateRange.LAST_7_DAYS,
                    onClick = {
                        selectedDateRange = DateRange.LAST_7_DAYS
                        showFilterSheet = false
                    }
                )

                DateRangeOption(
                    title = "This month",
                    selected = selectedDateRange == DateRange.THIS_MONTH,
                    onClick = {
                        selectedDateRange = DateRange.THIS_MONTH
                        showFilterSheet = false
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun HabitHistoryCard(
    habitName: String,
    status: HabitStatus,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
fun DateRangeOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

enum class DateRange(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_7_DAYS("Last 7 days"),
    THIS_MONTH("This month"),
    CUSTOM("Custom range")
}