package app.cclauncher.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.cclauncher.data.ExternalWidgetModel
import app.cclauncher.ui.components.widgets.ExternalWidget

@Composable
fun DraggableWidgetContainer(
    widgets: List<ExternalWidgetModel>,
    editMode: Boolean,
    onWidgetsReordered: (List<ExternalWidgetModel>) -> Unit,
    onConfigureWidget: (ExternalWidgetModel) -> Unit = {},
    onRemoveWidget: (String) -> Unit = {}
) {
    if (widgets.isEmpty()) return

    // Track the ordered list of widgets
    val orderedWidgets = remember {
        mutableStateOf(widgets.sortedBy { it.position })
    }

    // Track which widget is being dragged
    var draggedWidgetId by remember { mutableStateOf<String?>(null) }

    // Track widget positions
    val widgetPositions = remember { mutableStateMapOf<String, Offset>() }
    val widgetSizes = remember { mutableStateMapOf<String, IntSize>() }

    // Track the current drag position
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    // Update the ordered list when widgets change
    LaunchedEffect(widgets) {
        orderedWidgets.value = widgets.sortedBy { it.position }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(orderedWidgets.value) { index, widget ->
                val isBeingDragged = widget.id == draggedWidgetId

                // Animate elevation and scale when dragging
                val elevation by animateDpAsState(
                    targetValue = if (isBeingDragged) 8.dp else 0.dp,
                    label = "elevation"
                )

                Box(
                    modifier = Modifier
                        .zIndex(if (isBeingDragged) 1f else 0f)
                        .onGloballyPositioned { coordinates ->
                            // Store the widget position and size
                            widgetPositions[widget.id] = coordinates.positionInRoot()
                            widgetSizes[widget.id] = coordinates.size
                        }
                        .then(
                            if (editMode) {
                                Modifier.pointerInput(widget.id) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggedWidgetId = widget.id
                                            dragOffset = Offset.Zero
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragOffset += amount

                                            // Find the new position based on drag location
                                            val dragPosition = widgetPositions[widget.id]?.plus(dragOffset)
                                            if (dragPosition != null) {
                                                // Check if we should swap with another widget
                                                for ((otherId, otherPos) in widgetPositions) {
                                                    if (otherId != widget.id) {
                                                        val otherSize = widgetSizes[otherId] ?: continue

                                                        // Check if dragged widget overlaps with this one
                                                        if (dragPosition.x > otherPos.x &&
                                                            dragPosition.x < otherPos.x + otherSize.width) {

                                                            // Swap the widgets
                                                            val currentList = orderedWidgets.value.toMutableList()
                                                            val draggedIndex = currentList.indexOfFirst { it.id == widget.id }
                                                            val targetIndex = currentList.indexOfFirst { it.id == otherId }

                                                            if (draggedIndex != -1 && targetIndex != -1) {
                                                                val temp = currentList[draggedIndex]
                                                                currentList[draggedIndex] = currentList[targetIndex].copy(
                                                                    position = draggedIndex
                                                                )
                                                                currentList[targetIndex] = temp.copy(
                                                                    position = targetIndex
                                                                )
                                                                orderedWidgets.value = currentList
                                                                break
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            // Save the new ordering
                                            if (draggedWidgetId != null) {
                                                val updatedWidgets = orderedWidgets.value.mapIndexed { index, widget ->
                                                    widget.copy(position = index)
                                                }
                                                onWidgetsReordered(updatedWidgets)
                                            }
                                            draggedWidgetId = null
                                            dragOffset = Offset.Zero
                                        },
                                        onDragCancel = {
                                            draggedWidgetId = null
                                            dragOffset = Offset.Zero
                                        }
                                    )
                                }
                            } else {
                                Modifier
                            }
                        )
                ) {
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
}