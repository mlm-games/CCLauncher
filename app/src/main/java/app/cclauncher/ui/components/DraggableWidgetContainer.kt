package app.cclauncher.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize
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
    var draggedWidgetIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    // Track widget positions for drop calculation
    val widgetPositions = remember { mutableStateMapOf<Int, Pair<Offset, IntSize>>() }

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

        Box(modifier = Modifier.fillMaxWidth()) {
            // Widget row with improved draggable support
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(orderedWidgets) { index, widget ->
                    val isDragging = draggedWidgetIndex == index

                    val elevation by animateDpAsState(
                        if (isDragging) 8.dp else 0.dp,
                        label = "elevation"
                    )
                    val scale by animateFloatAsState(
                        if (isDragging) 1.1f else 1f,
                        label = "scale"
                    )
                    val alpha by animateFloatAsState(
                        if (isDragging) 0.7f else 1f,
                        label = "alpha"
                    )

                    Box(
                        modifier = Modifier
                            .onGloballyPositioned { coordinates ->
                                // Store position of each widget for drop target calculation
                                widgetPositions[index] = coordinates.positionInRoot() to coordinates.size
                            }
                            .zIndex(if (isDragging) 10f else 1f)
                            .scale(scale)
                            .alpha(alpha)
                            .shadow(elevation)
                            .then(
                                if (editMode) {
                                    Modifier.pointerInput(widget.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggedWidgetIndex = index
                                                dragOffset = Offset.Zero
                                            },
                                            onDrag = { change, dragAmount -> //TODO: Doesn't work
                                                change.consume()
                                                dragOffset += dragAmount

                                                // Find the widget under the current drag position
                                                val currentPosition = widgetPositions[index]?.first?.plus(dragOffset)
                                                if (currentPosition != null) {
                                                    // Find which widget we're hovering over
                                                    val targetIndex = widgetPositions.entries.firstOrNull { (i, posSize) ->
                                                        if (i != index) {
                                                            val (pos, size) = posSize
                                                            currentPosition.x >= pos.x &&
                                                                    currentPosition.x <= pos.x + size.width
                                                        } else false
                                                    }?.key

                                                    // If valid target found, swap widgets
                                                    if (targetIndex != null && targetIndex != index) {
                                                        val newList = orderedWidgets.toMutableList()
                                                        val temp = newList[index]
                                                        newList[index] = newList[targetIndex]
                                                        newList[targetIndex] = temp

                                                        // Update positions to match new order
                                                        val reposWidgets = newList.mapIndexed { i, w ->
                                                            w.copy(position = i)
                                                        }

                                                        orderedWidgets = reposWidgets
                                                        draggedWidgetIndex = targetIndex
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                // Apply the changes
                                                onWidgetsReordered(orderedWidgets)
                                                draggedWidgetIndex = null
                                            },
                                            onDragCancel = {
                                                draggedWidgetIndex = null
                                            }
                                        )
                                    }
                                } else Modifier
                            )
                    ) {
                        // Visual indicator when in edit mode
                        if (editMode) {
                            Card(
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {}
                        }

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
}