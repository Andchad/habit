package com.andchad.habit.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andchad.habit.data.model.Habit
import com.andchad.habit.ui.components.ModernTopAppBar
import com.andchad.habit.ui.screens.components.HabitItemCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageHabitsScreen(
    habits: List<Habit>,
    onBack: () -> Unit, // Kept for compatibility but not used
    onEditHabit: (Habit) -> Unit,
    onDeleteHabit: (Habit) -> Unit,
    onDeleteAllHabits: () -> Unit,
    onAddHabit: () -> Unit, // New parameter for add habit functionality
    modifier: Modifier = Modifier
) {
    var showDeleteAllConfirmation by remember { mutableStateOf(false) }

    if (showDeleteAllConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirmation = false },
            title = { Text("Delete All Habits") },
            text = { Text("Are you sure you want to delete all habits? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAllHabits()
                        showDeleteAllConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteAllConfirmation = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            ModernTopAppBar(
                title = "Manage Habits"
                // No actions for top bar - delete icon removed
            )
        },
        floatingActionButton = {
            // Add FAB on the right
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
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (habits.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Text(
                            text = "No habits found.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = "Create a habit to get started!",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "All Habits (${habits.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 88.dp), // Extra padding for FAB
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = habits,
                            key = { it.id }
                        ) { habit ->
                            HabitItemCard(
                                habit = habit,
                                onEdit = onEditHabit,
                                onDelete = onDeleteHabit,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Delete FAB (positioned on the left side)
                if (habits.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = { showDeleteAllConfirmation = true },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete All Habits"
                        )
                    }
                }
            }
        }
    }
}