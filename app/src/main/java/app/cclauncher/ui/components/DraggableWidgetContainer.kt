package app.cclauncher.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.cclauncher.data.ExternalWidgetModel


@Composable
fun DraggableWidgetContainer(
    widgets: List<ExternalWidgetModel>,
    editMode: Boolean,
    onWidgetsReordered: (List<ExternalWidgetModel>) -> Unit,
    onConfigureWidget: (ExternalWidgetModel) -> Unit = {},
    onRemoveWidget: (String) -> Unit = {},
    onResizeWidget: (ExternalWidgetModel, Int, Int) -> Unit = { _, _, _ -> }
) {
    if (widgets.isEmpty()) return

    // Track widget order
    var orderedWidgets by remember { mutableStateOf(widgets.sortedBy { it.position }) }
    var draggedWidget by remember { mutableStateOf<ExternalWidgetModel?>(null) }
    var draggedPosition by remember { mutableStateOf(Offset.Zero) }
    var targetIndex by remember { mutableStateOf<Int?>(null) }

    // Update ordered widgets when input changes
    LaunchedEffect(widgets) {
        orderedWidgets = widgets.sortedBy { it.position }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Display edit mode indicator
        if (editMode) {
            Text(
                text = "Tap and hold a widget to drag, configure or remove it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }

        // Widget row with draggable support
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(orderedWidgets) { index, widget ->
                val isDragging = draggedWidget?.id == widget.id
                val elevation = animateDpAsState(
                    if (isDragging) 8.dp else 0.dp,
                    label = "elevation"
                )

                Box(
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .shadow(elevation.value)
                        .then(
                            if (editMode) {
                                Modifier.pointerInput(widget.id) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggedWidget = widget
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            draggedPosition += dragAmount

                                            // Calculate potential new position
                                            val itemWidth = size.width
                                            val newPosition = (draggedPosition.x / itemWidth).toInt()

                                            if (newPosition in orderedWidgets.indices && newPosition != index) {
                                                targetIndex = newPosition
                                            }
                                        },
                                        onDragEnd = {
                                            // Reorder widgets if target is valid
                                            targetIndex?.let { target ->
                                                val newList = orderedWidgets.toMutableList()
                                                val fromIndex = newList.indexOfFirst { it.id == widget.id }
                                                val toIndex = target

                                                if (fromIndex != -1 && toIndex in newList.indices) {
                                                    val item = newList.removeAt(fromIndex)
                                                    newList.add(toIndex, item)

                                                    // Update positions
                                                    val repositioned = newList.mapIndexed { i, w ->
                                                        w.copy(position = i)
                                                    }

                                                    orderedWidgets = repositioned
                                                    onWidgetsReordered(repositioned)
                                                }
                                            }

                                            draggedWidget = null
                                            targetIndex = null
                                        },
                                        onDragCancel = {
                                            draggedWidget = null
                                            targetIndex = null
                                        }
                                    )
                                }
                            } else Modifier
                        )
                ) {
                    ExternalWidget(
                        widget = widget,
                        editMode = editMode,
                        onConfigureWidget = onConfigureWidget,
                        onRemoveWidget = onRemoveWidget,
                        onResizeWidget = onResizeWidget
                    )
                }
            }
        }
    }
}