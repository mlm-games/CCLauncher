package app.cclauncher.ui.components

import android.appwidget.AppWidgetHostView
import android.util.Log
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.cclauncher.data.ExternalWidgetModel
import app.cclauncher.helper.WidgetHelper
import kotlin.math.abs

@Composable
fun ExternalWidget(
    widget: ExternalWidgetModel,
    editMode: Boolean,
    onConfigureWidget: (ExternalWidgetModel) -> Unit = {},
    onRemoveWidget: (String) -> Unit = {},
    onResizeWidget: (ExternalWidgetModel, Int, Int) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val widgetHelper = remember { WidgetHelper(context) }
    val config = widget.config
    val density = LocalDensity.current

    // Calculate widget dimensions based on grid size
    var widthMultiplier by remember { mutableIntStateOf(widget.width.coerceIn(1, 4)) }
    var heightMultiplier by remember { mutableIntStateOf(widget.height.coerceIn(1, 4)) }

    // Base unit is 80dp
    val displayMetrics = LocalContext.current.resources.displayMetrics
    val screenWidth = with(LocalDensity.current) { displayMetrics.widthPixels.toDp() }
    val baseUnit = (screenWidth / 5).coerceAtMost(80.dp)
    val widgetWidth = baseUnit * widthMultiplier
    val widgetHeight = baseUnit * heightMultiplier

    // Track widget creation errors
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Reset resizing state when edit mode changes

    Box(
        modifier = Modifier
            .width(widgetWidth)
            .height(widgetHeight)
            .padding(4.dp)
            .clip(RoundedCornerShape(config.cornerRadius.dp))
            .background(Color(config.backgroundColor))
            .then(
                if (editMode) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(config.cornerRadius.dp)
                    )
                } else Modifier
            )
            .padding(config.padding.dp)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                try {
                    // Check if the widget info exists
                    val widgetInfo = widgetHelper.getWidgetInfo(widget.appWidgetId)
                    if (widgetInfo == null) {
                        hasError = true
                        errorMessage = "Widget not found"
                        return@AndroidView View(ctx).apply {
                            setBackgroundColor(android.graphics.Color.RED)
                        }
                    }

                    // Create widget view
                    val widgetView = widgetHelper.createWidget(widget.appWidgetId)

                    // Update widget dimensions
                    with(density) {
                        val widthPx = widgetWidth.toPx().toInt()
                        val heightPx = widgetHeight.toPx().toInt()
                        widgetHelper.updateWidgetOptions(widget.appWidgetId, widthPx, heightPx)
                    }

                    widgetView
                } catch (e: Exception) {
                    hasError = true
                    errorMessage = e.message ?: "Unknown error"

                    // Fallback view if widget creation fails
                    View(ctx).apply {
                        setBackgroundColor(android.graphics.Color.RED)
                    }
                }
            },
            update = { view ->
                // Only update when necessary dimensions change
                if (view is AppWidgetHostView &&
                    (view.tag as? Pair<Int, Int>)?.let { it.first != widthMultiplier || it.second != heightMultiplier } != false) {

                    try {
                        with(density) {
                            val widthPx = widgetWidth.toPx().toInt()
                            val heightPx = widgetHeight.toPx().toInt()
                            view.updateAppWidgetSize(
                                null,
                                widthPx,
                                heightPx,
                                widthPx,
                                heightPx
                            )
                            // Store current dimensions as tag
                            view.tag = Pair(widthMultiplier, heightMultiplier)
                        }
                    } catch (e: Exception) {
                        Log.e("ExternalWidget", "Error updating widget size: ${e.message}")
                    }
                }
            }
        )


        // Show error state if widget creation failed
        if (hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = Color.White
                    )
                    Text(
                        text = "Widget Error",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Edit controls (only visible in edit mode)
        if (editMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                IconButton(
                    onClick = { onRemoveWidget(widget.id) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove Widget",
                        tint = Color.White
                    )
                }
            }

            val cornerSize = 20.dp

            Box(
                modifier = Modifier
                    .size(cornerSize)
                    .align(Alignment.BottomEnd)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    .pointerInput(widget.id) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            // Increase width/height based on drag
                            val dragX = dragAmount.x / density.density
                            val dragY = dragAmount.y / density.density

                            // Update width multiplier (within bounds 1-4)
                            if (abs(dragX) > 2f) {
                                val direction = if (dragX > 0) 1 else -1
                                widthMultiplier = (widthMultiplier + direction).coerceIn(1, 4)
                            }

                            // Update height multiplier (within bounds 1-4)
                            if (abs(dragY) > 2f) {
                                val direction = if (dragY > 0) 1 else -1
                                heightMultiplier = (heightMultiplier + direction).coerceIn(1, 4)
                            }

                            // Apply resize immediately
                            onResizeWidget(widget, widthMultiplier, heightMultiplier)
                        }
                    }
            )

            Box(
                modifier = Modifier
                    .size(cornerSize)
                    .align(Alignment.BottomStart)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    .pointerInput(widget.id) {
                        // Similar drag detection but affecting width differently
                    }
            )


//TODO: Need better draggable corners?

        }
    }
}