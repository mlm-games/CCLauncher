package app.cclauncher.ui.util

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.abs
import kotlin.math.max

enum class SwipeDirection { Up, Down, Left, Right }

data class SwipeGesture(
    val direction: SwipeDirection,
    val fingers: Int,
)

fun Modifier.detectSwipeGestures(
    sensitivity: Float = 1.0f,
    onSwipe: (SwipeGesture) -> Unit,
): Modifier = this.then(
    Modifier.pointerInput(sensitivity) {
        val minSwipeDistance = 50f * sensitivity

        awaitEachGesture {
            val firstDown = awaitFirstDown(requireUnconsumed = false)
            var lastCentroid = firstDown.position
            var totalDrag = Offset.Zero
            var lastPointerCount = 1
            var maxPointerCount = 1
            var pastTouchSlop = false
            var triggered = false

            while (true) {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }
                if (pressed.isEmpty()) break

                if (event.changes.any { it.isConsumed && it.positionChanged() }) {
                    return@awaitEachGesture
                }

                if (triggered) {
                    event.changes.forEach { change ->
                        if (change.pressed) change.consume()
                    }
                    continue
                }

                val pointerCount = pressed.size
                val centroid = pressed.fold(Offset.Zero) { acc, change ->
                    acc + change.position
                } / pointerCount.toFloat()

                if (pointerCount != lastPointerCount) {
                    lastPointerCount = pointerCount
                    lastCentroid = centroid
                    totalDrag = Offset.Zero
                    maxPointerCount = max(maxPointerCount, pointerCount)
                    continue
                }

                maxPointerCount = max(maxPointerCount, pointerCount)
                totalDrag += centroid - lastCentroid
                lastCentroid = centroid

                if (!pastTouchSlop) {
                    if (totalDrag.getDistance() > viewConfiguration.touchSlop) {
                        pastTouchSlop = true
                        totalDrag = Offset.Zero
                    } else {
                        continue
                    }
                }

                event.changes.forEach { change ->
                    if (change.pressed) change.consume()
                }

                val absX = abs(totalDrag.x)
                val absY = abs(totalDrag.y)
                if (absX <= minSwipeDistance && absY <= minSwipeDistance) continue

                triggered = true
                val direction = if (absX > absY) {
                    if (totalDrag.x > 0) SwipeDirection.Right else SwipeDirection.Left
                } else {
                    if (totalDrag.y > 0) SwipeDirection.Down else SwipeDirection.Up
                }
                onSwipe(
                    SwipeGesture(
                        direction = direction,
                        fingers = if (maxPointerCount >= 2) 2 else 1,
                    )
                )
            }
        }
    }
)
