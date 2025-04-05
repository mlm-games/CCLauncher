package app.cclauncher.ui.compose.util

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

/**
 * Adds swipe gesture detection to a composable
 */
fun Modifier.detectSwipeGestures(
    onSwipeUp: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
): Modifier = this.then(
    Modifier.pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
            change.consume()
            val (x, y) = dragAmount

            // Use a minimum threshold to avoid accidental swipes
            val minSwipeDistance = 50f

            // Determine which direction has the highest magnitude
            when {
                abs(x) > abs(y) && abs(x) > minSwipeDistance -> {
                    // Horizontal swipe
                    if (x > 0) {
                        onSwipeRight()
                    } else {
                        onSwipeLeft()
                    }
                }
                abs(y) > abs(x) && abs(y) > minSwipeDistance -> {
                    // Vertical swipe
                    if (y > 0) {
                        onSwipeDown()
                    } else {
                        onSwipeUp()
                    }
                }
            }
        }
    }
)