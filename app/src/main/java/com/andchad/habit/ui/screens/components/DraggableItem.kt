package com.andchad.habit.ui.screens.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun <T> DraggableList(
    items: List<T>,
    onMove: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    itemContent: @Composable (item: T, isDragging: Boolean, dragModifier: Modifier) -> Unit
) {
    val listState = rememberLazyListState()
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = modifier
    ) {
        itemsIndexed(items) { index, item ->
            val isDragging = draggedItemIndex == index

            // Create a modifier for the drag handle
            val dragModifier = Modifier.pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        draggedItemIndex = index
                    },
                    onDragEnd = {
                        draggedItemIndex = null
                    },
                    onDragCancel = {
                        draggedItemIndex = null
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()

                        if (draggedItemIndex == null) return@detectDragGesturesAfterLongPress

                        // Determine if dragging up or down
                        if (dragAmount.y < -10) {
                            // Dragging up
                            if (index > 0) {
                                onMove(index, index - 1)
                                draggedItemIndex = index - 1
                            }
                        } else if (dragAmount.y > 10) {
                            // Dragging down
                            if (index < items.size - 1) {
                                onMove(index, index + 1)
                                draggedItemIndex = index + 1
                            }
                        }
                    }
                )
            }

            // Apply visual effects for dragged item
            Box(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .shadow(if (isDragging) 8.dp else 1.dp)
                    .scale(if (isDragging) 1.02f else 1f)
                    .zIndex(if (isDragging) 1f else 0f)
            ) {
                itemContent(item, isDragging, dragModifier)
            }
        }
    }
}