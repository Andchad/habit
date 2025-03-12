package com.andchad.habit.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun WheelTimePicker(
    initialHour: Int,
    initialMinute: Int,
    onCancel: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    // Store current selection
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }

    val hourItems = remember { List(24) { it } }
    val minuteItems = remember { List(60) { it } }

    // Create padding for top and bottom
    val visibleItems = 5
    val extraItems = remember { List(visibleItems / 2) { -1 } }

    // Create list states
    val hourState = rememberLazyListState()
    val minuteState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Initialize scroll positions
    LaunchedEffect(Unit) {
        hourState.scrollToItem(initialHour, 0)
        minuteState.scrollToItem(initialMinute, 0)
    }

    // Track hour selection changes
    LaunchedEffect(hourState) {
        snapshotFlow { hourState.firstVisibleItemIndex }
            .collectLatest { index ->
                if (index >= 0 && index < hourItems.size) {
                    selectedHour = hourItems[index]
                }
            }
    }

    // Track minute selection changes
    LaunchedEffect(minuteState) {
        snapshotFlow { minuteState.firstVisibleItemIndex }
            .collectLatest { index ->
                if (index >= 0 && index < minuteItems.size) {
                    selectedMinute = minuteItems[index]
                }
            }
    }

    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Time",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Current selection display
                Text(
                    text = String.format("%02d:%02d", selectedHour, selectedMinute),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Wheels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hours wheel
                    PickerWheel(
                        items = hourItems,
                        listState = hourState,
                        formatText = { String.format("%02d", it) },
                        modifier = Modifier.width(70.dp)
                    )

                    Text(
                        text = ":",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Minutes wheel
                    PickerWheel(
                        items = minuteItems,
                        listState = minuteState,
                        formatText = { String.format("%02d", it) },
                        modifier = Modifier.width(70.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = { onConfirm(selectedHour, selectedMinute) }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerWheel(
    items: List<Int>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    formatText: (Int) -> String,
    modifier: Modifier = Modifier
) {
    val itemHeight = 50.dp
    val visibleItems = 3 // Number of visible items (should be odd)

    Box(
        modifier = modifier.height(itemHeight * visibleItems),
        contentAlignment = Alignment.Center
    ) {
        // Background highlight for the selected item
        Box(
            modifier = Modifier
                .height(itemHeight)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                .align(Alignment.Center)
        )

        // The wheel itself
        LazyColumn(
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = true
        ) {
            // Add padding items at top
            items(visibleItems / 2) {
                Spacer(modifier = Modifier.height(itemHeight))
            }

            // Actual items
            items(items.size) { index ->
                val item = items[index]
                val isSelected = index == listState.firstVisibleItemIndex

                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatText(item),
                        fontSize = if (isSelected) 22.sp else 18.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Add padding items at bottom
            items(visibleItems / 2) {
                Spacer(modifier = Modifier.height(itemHeight))
            }
        }
    }
}