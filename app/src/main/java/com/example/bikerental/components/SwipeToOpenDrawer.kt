package com.example.bikerental.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope

/**
 * Modifier extension to handle swipe gesture to open drawer from the left edge
 */
fun Modifier.swipeToOpenDrawer(
    onSwipeToOpen: () -> Unit
): Modifier = this.pointerInput(Unit) {
    coroutineScope {
        detectHorizontalDragGestures(
            onDragStart = { startPosition ->
                // Store the start position of the drag
                val startX = startPosition.x
                val screenWidth = this@pointerInput.size.width
                
                // If drag starts from left edge (first 20% of screen width)
                if (startX < screenWidth * 0.2f) {
                    // Prepare for potential swipe detection
                }
            },
            onDragEnd = {},
            onDragCancel = {},
            onHorizontalDrag = { _, dragAmount ->
                // If drag motion is to the right (positive dragAmount) and substantial enough
                if (dragAmount > 10) {
                    onSwipeToOpen()
                }
            }
        )
    }
} 