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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andchad.habit.data.model.Habit
import com.andchad.habit.ui.components.ModernTopAppBar
import com.andchad.habit.ui.screens.components.HabitItemCard
import com.andchad.habit.ui.screens.components.PastDueHabitList
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitListScreen(
    upcomingHabits: List<Habit>,
    pastDueHabits: List<Habit>,
    isDarkMode: Boolean,
    showTodayHabitsOnly: Boolean,
    adCounter: Int,
    onToggleDarkMode: () -> Unit,
    onToggleHabitsFilter: () -> Unit,
    onAddHabit: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    onDeleteHabit: (Habit) -> Unit,
    onToggleCompleteHabit: (String, Boolean) -> Unit,
    onDismissHabit: (String) -> Unit,
    onManageHabits: () -> Unit, // Kept for compatibility but not used
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            ModernTopAppBar(
                title = "Hab-it!",
                actions = {
                    IconButton(onClick = onToggleDarkMode) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkMode) "Switch to Light Mode" else "Switch to Dark Mode",
                            tint = Color.White
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
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (upcomingHabits.isEmpty() && pastDueHabits.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Text(
                        text = if (showTodayHabitsOnly)
                            "No upcoming habits scheduled for today."
                        else
                            "No upcoming habits scheduled.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "Tap + to create a habit!",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // First show any past due habits with a completion opportunity
                    if (pastDueHabits.isNotEmpty()) {
                        PastDueHabitList(
                            pastDueHabits = pastDueHabits,
                            onToggleComplete = onToggleCompleteHabit,
                            onDismissHabit = onDismissHabit,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Then show upcoming habits
                    if (upcomingHabits.isNotEmpty()) {
                        Text(
                            text = "Upcoming Habits",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = upcomingHabits,
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
                }
            }
        }
    }
}