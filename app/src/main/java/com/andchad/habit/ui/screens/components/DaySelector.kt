package com.andchad.habit.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DaySelector(
    selectedDays: List<DayOfWeek>,
    onDaySelected: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Repeat on days",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DayOfWeek.values().forEach { day ->
                DayButton(
                    day = day,
                    selected = selectedDays.contains(day),
                    onClick = { onDaySelected(day) }
                )
            }
        }

        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun DayButton(
    day: DayOfWeek,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dayName = day.getDisplayName(TextStyle.SHORT, Locale.getDefault())

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
            )
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayName.first().toString(),
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}