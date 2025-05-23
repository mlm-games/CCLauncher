package app.cclauncher.ui.composables

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WidgetHostViewContainer(
    modifier: Modifier = Modifier,
    appWidgetId: Int,
    providerInfo: AppWidgetProviderInfo,
    appWidgetHost: AppWidgetHost,
    widgetSizeData: WidgetSizeData,
    onLongPress: () -> Unit = {},
) {
    val context = LocalContext.current
    val widgetManager = AppWidgetManager.getInstance(context)
    var errorLoading by remember { mutableStateOf(false) }

    // Track touch state for manual handling
    var touchStartTime by remember { mutableLongStateOf(0L) }
    var touchStartPosition by remember { mutableStateOf(Offset.Zero) }
    var longPressJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    if (errorLoading) {
        Box(modifier = modifier) { /* Error state */ }
        return
    }

    // Create the widget view
    val widgetView = remember(appWidgetId) {
        try {
            appWidgetHost.createView(context, appWidgetId, providerInfo)?.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // Apply size options
                val options = Bundle().apply {
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widgetSizeData.minWidthDp.value.toInt())
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widgetSizeData.maxWidthDp.value.toInt())
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, widgetSizeData.minHeightDp.value.toInt())
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, widgetSizeData.maxHeightDp.value.toInt())
                }
                widgetManager.updateAppWidgetOptions(appWidgetId, options)
            }
        } catch (e: Exception) {
            Log.e("WidgetHost", "Error creating widget view for ID $appWidgetId", e)
            errorLoading = true
            null
        }
    }

    if (widgetView != null) {
        // Box with rounded corners that will clip its content
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp)) // This will clip the widget view to rounded corners
        ) {
            // The widget view will be clipped to the rounded shape of its parent
            AndroidView(
                factory = { widgetView },
                modifier = Modifier
                    .fillMaxSize()
                    // Use raw pointer input to manually handle all touch events
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

                                // Process based on event type
                                when (event.type) {
                                    PointerEventType.Press -> {
                                        // Cancel any previous job
                                        longPressJob?.cancel()

                                        // Record start position and time
                                        touchStartPosition = event.changes.first().position
                                        touchStartTime = System.currentTimeMillis()

                                        // Start long press detection
                                        longPressJob = coroutineScope.launch {
                                            delay(ViewConfiguration.getLongPressTimeout().toLong())
                                            // If we reach here, it's a long press
                                            onLongPress()
                                        }

                                        // Forward touch down event to widget
                                        val motionEvent = MotionEvent.obtain(
                                            touchStartTime, touchStartTime,
                                            MotionEvent.ACTION_DOWN,
                                            touchStartPosition.x, touchStartPosition.y,
                                            0
                                        )
                                        widgetView.dispatchTouchEvent(motionEvent)
                                        motionEvent.recycle()
                                    }

                                    PointerEventType.Move -> {
                                        val currentPosition = event.changes.first().position
                                        val distance = (currentPosition - touchStartPosition).getDistance()

                                        // If moved more than touch slop, cancel long press and let widget handle scrolling
                                        if (distance > touchSlop) {
                                            longPressJob?.cancel()
                                            longPressJob = null
                                        }

                                        // Forward touch move event to widget
                                        val motionEvent = MotionEvent.obtain(
                                            touchStartTime, System.currentTimeMillis(),
                                            MotionEvent.ACTION_MOVE,
                                            currentPosition.x, currentPosition.y,
                                            0
                                        )
                                        widgetView.dispatchTouchEvent(motionEvent)
                                        motionEvent.recycle()
                                    }

                                    PointerEventType.Release, PointerEventType.Exit -> {
                                        // Cancel long press detection
                                        longPressJob?.cancel()
                                        longPressJob = null

                                        // Forward touch up event to widget
                                        val action = if (event.type == PointerEventType.Release)
                                            MotionEvent.ACTION_UP else MotionEvent.ACTION_CANCEL

                                        val currentPosition = event.changes.first().position
                                        val motionEvent = MotionEvent.obtain(
                                            touchStartTime, System.currentTimeMillis(),
                                            action,
                                            currentPosition.x, currentPosition.y,
                                            0
                                        )
                                        widgetView.dispatchTouchEvent(motionEvent)
                                        motionEvent.recycle()
                                    }
                                }
                            }
                        }
                    }
            )
        }
    } else {
        Box(modifier = modifier) { /* Error placeholder */ }
    }

    // Clean up coroutines when the composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            longPressJob?.cancel()
            // Ensure widget view is properly cleaned up
            widgetView?.apply {
                if (parent != null) {
                    (parent as? ViewGroup)?.removeView(this)
                }
            }
        }
    }
}

// Helper class to bundle widget size info
data class WidgetSizeData(
    val width: Int, // Size in pixels for layout
    val height: Int,
    val minWidthDp: Dp,
    val maxWidthDp: Dp,
    val minHeightDp: Dp,
    val maxHeightDp: Dp
)