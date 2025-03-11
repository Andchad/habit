package com.andchad.habit.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
    adCounter: Int,
    onToggleDarkMode: () -> Unit,
    onAddHabit: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    onDeleteHabit: (Habit) -> Unit,
    onToggleCompleteHabit: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hab-it!") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // Ad counter indicator
                    Text(
                        text = "Ad in: ${3 - adCounter}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(end = 8.dp)
                    )

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
                    text = "No habits yet. Tap + to create your first habit!",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
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