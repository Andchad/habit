package com.andchad.habit.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andchad.habit.data.model.Habit
import com.andchad.habit.ui.screens.components.HabitItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitListScreen(
    habits: List<Habit>,
    isDarkMode: Boolean,
    showTodayHabitsOnly: Boolean,
    adCounter: Int,
    onToggleDarkMode: () -> Unit,
    onToggleHabitsFilter: () -> Unit,
    onAddHabit: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    onDeleteHabit: (Habit) -> Unit,
    onDeleteCompletedHabits: (List<Habit>) -> Unit,
    onToggleCompleteHabit: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Get completed habits
    val completedHabits = habits.filter { it.isCompleted }
    val hasCompletedHabits = completedHabits.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hab-it!") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // Today/All Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Today only",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Switch(
                            checked = showTodayHabitsOnly,
                            onCheckedChange = { onToggleHabitsFilter() }
                        )
                    }

                    // Delete completed habits button
                    if (hasCompletedHabits) {
                        IconButton(
                            onClick = { showDeleteConfirmation = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Selected Habits"
                            )
                        }
                    }

                    IconButton(onClick = onToggleDarkMode) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkMode) "Switch to Light Mode" else "Switch to Dark Mode"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddHabit,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Habit"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (habits.isEmpty()) {
                Text(
                    text = if (showTodayHabitsOnly)
                        "No habits scheduled for today. Tap + to create a habit!"
                    else
                        "No habits yet. Tap + to create your first habit!",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (hasCompletedHabits) {
                        Button(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            enabled = hasCompletedHabits
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "Delete ${completedHabits.size} selected ${if (completedHabits.size == 1) "Habit" else "Habits"}"
                            )
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = habits,
                            key = { it.id }
                        ) { habit ->
                            HabitItem(
                                habit = habit,
                                onEdit = onEditHabit,
                                onDelete = onDeleteHabit,
                                onToggleComplete = onToggleCompleteHabit
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Selected Habits") },
            text = {
                Text(
                    "Are you sure you want to delete ${completedHabits.size} selected ${if (completedHabits.size == 1) "habit" else "habits"}?" +
                            " This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCompletedHabits(completedHabits)
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}