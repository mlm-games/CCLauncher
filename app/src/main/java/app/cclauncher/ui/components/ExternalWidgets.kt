package app.cclauncher.ui.components

import android.appwidget.AppWidgetHostView
import android.util.Log
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.cclauncher.data.ExternalWidgetModel
import app.cclauncher.helper.WidgetHelper

private const val TAG = "ExternalWidget"

@Composable
fun ExternalWidget(
    widget: ExternalWidgetModel,
    onConfigureWidget: (ExternalWidgetModel) -> Unit = {},
    onRemoveWidget: (String) -> Unit = {},
    editMode: Boolean = false
) {
    val context = LocalContext.current
    val widgetHelper = remember { WidgetHelper(context) }
    val config = widget.config
    val density = LocalDensity.current

    // Calculate widget dimensions based on grid size
    val widthMultiplier = widget.width.coerceIn(1, 4)
    val heightMultiplier = widget.height.coerceIn(1, 4)

    // Base unit is 80dp
    val baseUnit = 80.dp
    val widgetWidth = baseUnit * widthMultiplier
    val widgetHeight = baseUnit * heightMultiplier

    // Track widget creation errors
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Log widget details
    LaunchedEffect(widget.id) {
        Log.d(TAG, "Rendering widget: id=${widget.id}, appWidgetId=${widget.appWidgetId}, size=${widget.width}x${widget.height}")
    }

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
        // Widget content using AndroidView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                try {
                    Log.d(TAG, "Creating widget view for appWidgetId: ${widget.appWidgetId}")

                    // Check if the widget info exists
                    val widgetInfo = widgetHelper.getWidgetInfo(widget.appWidgetId)
                    if (widgetInfo == null) {
                        Log.e(TAG, "Widget info not found for appWidgetId: ${widget.appWidgetId}")
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
                        Log.d(TAG, "Updating widget options to size: ${widthPx}x${heightPx}")
                        widgetHelper.updateWidgetOptions(widget.appWidgetId, widthPx, heightPx)
                    }

                    widgetView
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating widget view: ${e.message}", e)
                    hasError = true
                    errorMessage = e.message ?: "Unknown error"

                    // Fallback view if widget creation fails
                    View(ctx).apply {
                        setBackgroundColor(android.graphics.Color.RED)
                    }
                }
            },
            update = { view ->
                // Update widget if needed
                if (view is AppWidgetHostView) {
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
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating widget size: ${e.message}", e)
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
                    onClick = { onConfigureWidget(widget) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configure Widget",
                        tint = Color.White
                    )
                }

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

            // Size indicator
            Text(
                text = "${widget.width}x${widget.height}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}