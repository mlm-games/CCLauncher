package app.cclauncher.ui.components.widgets

import android.appwidget.AppWidgetHostView
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.cclauncher.data.ExternalWidgetModel
import app.cclauncher.helper.WidgetHelper

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

    // Calculate widget dimensions based on grid size
    val widthMultiplier = widget.width.coerceIn(1, 4)
    val heightMultiplier = widget.height.coerceIn(1, 4)

    // Base unit is 80dp
    val baseUnit = 80.dp
    val widgetWidth = baseUnit * widthMultiplier
    val widgetHeight = baseUnit * heightMultiplier

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
                        color = Color.White.copy(alpha = 0.5f),
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
                    // Create widget view
                    val widgetView = widgetHelper.createWidget(widget.appWidgetId)

                    // Update widget dimensions
                    val widthPx = (widgetWidth.value * ctx.resources.displayMetrics.density).toInt()
                    val heightPx = (widgetHeight.value * ctx.resources.displayMetrics.density).toInt()
                    widgetHelper.updateWidgetOptions(widget.appWidgetId, widthPx, heightPx)

                    widgetView
                } catch (e: Exception) {
                    // Fallback view if widget creation fails
                    View(ctx).apply {
                        setBackgroundColor(android.graphics.Color.RED)
                    }
                }
            },
            update = { view ->
                // Update widget if needed
                if (view is AppWidgetHostView) {
                    view.updateAppWidgetSize(
                        null,
                        view.width,
                        view.height,
                        view.width,
                        view.height
                    )
                }
            }
        )

        // Edit controls (only visible in edit mode)
        if (editMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                IconButton(
                    onClick = { onConfigureWidget(widget) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configure Widget",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = { onRemoveWidget(widget.id) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove Widget",
                        tint = Color.White
                    )
                }
            }
        }
    }
}