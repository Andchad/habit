package com.andchad.habit.ui.screens.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissState
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andchad.habit.data.model.Habit
import kotlinx.coroutines.delay
import androidx.compose.foundation.isSystemInDarkTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitItemCard(
    habit: Habit,
    onEdit: (Habit) -> Unit,
    onDelete: (Habit) -> Unit,
    modifier: Modifier = Modifier
) {
    var show by remember { mutableStateOf(true) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val dismissState = rememberDismissState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                DismissValue.DismissedToStart -> {
                    showDeleteConfirmation = true
                    false
                }
                DismissValue.DismissedToEnd -> false
                DismissValue.Default -> false
            }
        }
    )

    if (showDeleteConfirmation) {
        ConfirmationDialog(
            title = "Delete Habit",
            message = "Are you sure you want to delete this habit?",
            onConfirm = {
                show = false
                showDeleteConfirmation = false
            },
            onDismiss = {
                showDeleteConfirmation = false
            }
        )
    }

    AnimatedVisibility(
        visible = show,
        exit = shrinkHorizontally(
            animationSpec = tween(durationMillis = 500)
        ) + fadeOut()
    ) {
        SwipeToDismiss(
            state = dismissState,
            background = { DismissBackground(dismissState) },
            dismissContent = {
                HabitCard(
                    habit = habit,
                    onEdit = onEdit,
                    modifier = modifier
                )
            },
            directions = setOf(DismissDirection.EndToStart)
        )
    }

    LaunchedEffect(show) {
        if (!show) {
            delay(500)
            onDelete(habit)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DismissBackground(dismissState: DismissState) {
    val isInDarkTheme = isSystemInDarkTheme()

    val color = when (dismissState.dismissDirection) {
        DismissDirection.EndToStart -> {
            // Use a softer red color for dark theme to reduce contrast
            if (isInDarkTheme) {
                Color(0xFF703232) // Darker, less saturated red
            } else {
                Color.Red
            }
        }
        DismissDirection.StartToEnd -> {
            if (isInDarkTheme) {
                Color(0xFF2E5B2E) // Darker, less saturated green
            } else {
                Color.Green
            }
        }
        null -> Color.Transparent
    }

    val direction = dismissState.dismissDirection

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(12.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.weight(1f))
        if (direction == DismissDirection.EndToStart) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = if (isInDarkTheme) Color(0xFFE0E0E0) else Color.White // Softer white for dark theme
            )
        }
    }
}

@Composable
private fun HabitCard(
    habit: Habit,
    onEdit: (Habit) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timer icon for upcoming
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Upcoming",
                    modifier = Modifier
                        .padding(8.dp)
                        .size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Reminder: ${habit.reminderTime}",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (habit.scheduledDays.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Every: ${habit.getFormattedScheduledDays()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onEdit(habit) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}