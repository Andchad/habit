package com.andchad.habit.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andchad.habit.data.model.Habit

@Composable
fun PastDueHabitList(
    pastDueHabits: List<Habit>,
    onToggleComplete: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Need Your Attention",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )

            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
            )

            Text(
                text = "These habits are past due for today:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            pastDueHabits.forEach { habit ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = habit.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Scheduled for ${habit.reminderTime}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onToggleComplete(habit.id, true) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(text = "Mark Done")
                    }
                }

                if (pastDueHabits.indexOf(habit) < pastDueHabits.size - 1) {
                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}