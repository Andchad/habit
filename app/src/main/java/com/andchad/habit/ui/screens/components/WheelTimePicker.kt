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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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

    // Create list states
    val hourState = rememberLazyListState(initialFirstVisibleItemIndex = initialHour)
    val minuteState = rememberLazyListState(initialFirstVisibleItemIndex = initialMinute)
    val scope = rememberCoroutineScope()

    // Initialize scroll position to center the initial values
    LaunchedEffect(key1 = Unit) {
        kotlinx.coroutines.delay(50)
        scope.launch {
            hourState.scrollToItem(initialHour)
        }
        scope.launch {
            minuteState.scrollToItem(initialMinute)
        }
    }

    // Update selected values when visible items change
    LaunchedEffect(hourState.firstVisibleItemIndex) {
        val centerIndex = hourState.firstVisibleItemIndex
        if (centerIndex in hourItems.indices) {
            selectedHour = hourItems[centerIndex]
        }
    }

    LaunchedEffect(minuteState.firstVisibleItemIndex) {
        val centerIndex = minuteState.firstVisibleItemIndex
        if (centerIndex in minuteItems.indices) {
            selectedMinute = minuteItems[centerIndex]
        }
    }

    // Snap to center when scrolling stops
    LaunchedEffect(hourState.isScrollInProgress) {
        if (!hourState.isScrollInProgress) {
            val currentIndex = hourState.firstVisibleItemIndex
            if (currentIndex in hourItems.indices) {
                scope.launch {
                    hourState.animateScrollToItem(currentIndex)
                }
            }
        }
    }

    LaunchedEffect(minuteState.isScrollInProgress) {
        if (!minuteState.isScrollInProgress) {
            val currentIndex = minuteState.firstVisibleItemIndex
            if (currentIndex in minuteItems.indices) {
                scope.launch {
                    minuteState.animateScrollToItem(currentIndex)
                }
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
                    Box(
                        modifier = Modifier.width(70.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Selection highlight
                        Box(
                            modifier = Modifier
                                .height(50.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                .align(Alignment.Center)
                        )

                        PickerWheel(
                            items = hourItems,
                            listState = hourState,
                            formatText = { String.format("%02d", it) }
                        )
                    }

                    Text(
                        text = ":",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Minutes wheel
                    Box(
                        modifier = Modifier.width(70.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Selection highlight
                        Box(
                            modifier = Modifier
                                .height(50.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                .align(Alignment.Center)
                        )

                        PickerWheel(
                            items = minuteItems,
                            listState = minuteState,
                            formatText = { String.format("%02d", it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    TextButton(
                        onClick = { onConfirm(selectedHour, selectedMinute) },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "OK",
                            color = MaterialTheme.colorScheme.primary
                        )
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
    val visibleItems = 5 // Number of visible items (should be odd)

    Box(
        modifier = modifier.height(itemHeight * visibleItems),
        contentAlignment = Alignment.Center
    ) {
        // The wheel itself
        LazyColumn(
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            userScrollEnabled = true,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Add padding items at top
            items(visibleItems / 2) {
                Spacer(modifier = Modifier.height(itemHeight))
            }

            // Actual items
            items(items.size) { index ->
                val item = items[index]
                val isSelected = index == listState.firstVisibleItemIndex

                // Calculate appearance based on distance from selection
                val distanceFromCurrent = kotlin.math.abs(index - listState.firstVisibleItemIndex)
                val alpha = 1f - (distanceFromCurrent * 0.2f).coerceIn(0f, 0.8f)
                val fontSize = if (isSelected) 20.sp else 18.sp

                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatText(item),
                        fontSize = fontSize,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(4.dp),
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
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