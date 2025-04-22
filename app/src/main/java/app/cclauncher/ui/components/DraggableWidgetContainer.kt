package app.cclauncher.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.cclauncher.data.ExternalWidgetModel
import app.cclauncher.ui.components.ExternalWidget
import android.util.Log

private const val TAG = "DraggableWidgetContainer"

@Composable
fun DraggableWidgetContainer(
    widgets: List<ExternalWidgetModel>,
    editMode: Boolean,
    onWidgetsReordered: (List<ExternalWidgetModel>) -> Unit,
    onConfigureWidget: (ExternalWidgetModel) -> Unit = {},
    onRemoveWidget: (String) -> Unit = {}
) {
    if (widgets.isEmpty()) return

    // Track widget order
    var orderedWidgets by remember { mutableStateOf(widgets.sortedBy { it.position }) }

    // Update ordered widgets when input changes
    LaunchedEffect(widgets) {
        Log.d(TAG, "Widgets updated: ${widgets.size} widgets")
        orderedWidgets = widgets.sortedBy { it.position }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Display edit mode indicator
        if (editMode) {
            Text(
                text = "Tap and hold a widget to configure or remove it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }

        // Widget row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(orderedWidgets) { index, widget ->
                // Use our ExternalWidget component
                ExternalWidget(
                    widget = widget,
                    editMode = editMode,
                    onConfigureWidget = onConfigureWidget,
                    onRemoveWidget = onRemoveWidget
                )
            }
        }
    }
}