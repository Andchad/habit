package com.andchad.habit.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
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
    val hourListState = rememberLazyListState()
    val minuteListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val selectedHour by remember {
        derivedStateOf {
            val index = hourListState.firstVisibleItemIndex
            // Handle wraparound for hours
            index.mod(24)
        }
    }

    val selectedMinute by remember {
        derivedStateOf {
            val index = minuteListState.firstVisibleItemIndex
            // Handle wraparound for minutes
            index.mod(60)
        }
    }

    // Set initial scroll position
    LaunchedEffect(Unit) {
        // For hours, we multiply by a large number to simulate infinite scrolling
        hourListState.scrollToItem(initialHour + 24 * 1000)
        // For minutes, we do the same
        minuteListState.scrollToItem(initialMinute + 60 * 1000)
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
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Display the currently selected time prominently
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hours wheel
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Selection indicator
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .align(Alignment.Center)
                        )

                        // The scrollable list of hours
                        LazyColumn(
                            state = hourListState,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth(),
                            userScrollEnabled = true
                        ) {
                            // We create a large list to simulate infinite scrolling
                            items(count = 24 * 2000) { index ->
                                val hour = index.mod(24)
                                val isSelected = selectedHour == hour

                                Box(
                                    modifier = Modifier
                                        .height(50.dp)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = String.format("%02d", hour),
                                        fontSize = if (isSelected) 32.sp else 20.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .alpha(if (isSelected) 1f else 0.6f)
                                    )
                                }
                            }
                        }
                    }

                    // Separator
                    Text(
                        text = ":",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Minutes wheel
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Selection indicator
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .align(Alignment.Center)
                        )

                        // The scrollable list of minutes
                        LazyColumn(
                            state = minuteListState,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth(),
                            userScrollEnabled = true
                        ) {
                            // We create a large list to simulate infinite scrolling
                            items(count = 60 * 2000) { index ->
                                val minute = index.mod(60)
                                val isSelected = selectedMinute == minute

                                Box(
                                    modifier = Modifier
                                        .height(50.dp)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = String.format("%02d", minute),
                                        fontSize = if (isSelected) 32.sp else 20.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .alpha(if (isSelected) 1f else 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            "Cancel",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    TextButton(
                        onClick = { onConfirm(selectedHour, selectedMinute) }
                    ) {
                        Text(
                            "OK",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}