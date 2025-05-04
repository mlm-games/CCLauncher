package app.cclauncher.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import app.cclauncher.data.AppModel
import app.cclauncher.data.HomeAppPreference

@Composable
fun DraggableAppsGrid(
    apps: List<AppModel?>,
    columns: Int,
    showAppIcons: Boolean,
    itemSpacing: androidx.compose.ui.unit.Dp,
    onAppClick: (AppModel) -> Unit,
    onAppLongPress: (Int) -> Unit,
    onAppsReordered: (List<Pair<Int, AppModel?>>) -> Unit,
    editMode: Boolean
) {
    // Create indexed list of apps
    val indexedApps = remember(apps) { apps.mapIndexed { index, app -> index to app } }
    var reorderedApps by remember { mutableStateOf(indexedApps) }

    // Track dragged item and positions
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var draggedItem by remember { mutableStateOf<Pair<Int, AppModel?>?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    // Track grid item positions for drop calculation
    val itemPositions = remember { mutableStateMapOf<Int, Pair<Offset, IntSize>>() }

    // Reset reordered apps when input changes
    LaunchedEffect(apps) {
        reorderedApps = apps.mapIndexed { index, app -> index to app }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            modifier = Modifier.padding(16.dp)
        ) {
            itemsIndexed(reorderedApps) { index, (originalIndex, app) ->
                val isDragging = draggedItemIndex == index

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
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            // Store position of each item for drop target calculation
                            itemPositions[index] = coordinates.positionInRoot() to coordinates.size
                        }
                        .zIndex(if (isDragging) 10f else 1f)
                        .scale(scale)
                        .alpha(alpha)
                        .shadow(elevation)
                        .then(
                            if (editMode) {
                                Modifier.pointerInput(Unit) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { offset ->
                                            draggedItemIndex = index
                                            draggedItem = reorderedApps[index]
                                            dragOffset = Offset.Zero
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffset += dragAmount

                                            // Find the item under the current drag position
                                            val currentPosition = itemPositions[index]?.first?.plus(dragOffset)
                                            if (currentPosition != null) {
                                                // Find which item we're hovering over
                                                val targetIndex = itemPositions.entries.firstOrNull { (i, posSize) ->
                                                    if (i != index) {
                                                        val (pos, size) = posSize
                                                        currentPosition.x >= pos.x &&
                                                                currentPosition.x <= pos.x + size.width &&
                                                                currentPosition.y >= pos.y &&
                                                                currentPosition.y <= pos.y + size.height
                                                    } else false
                                                }?.key

                                                // If valid target found, swap items
                                                if (targetIndex != null && targetIndex != index) {
                                                    val newList = reorderedApps.toMutableList()
                                                    val temp = newList[index]
                                                    newList[index] = newList[targetIndex]
                                                    newList[targetIndex] = temp
                                                    reorderedApps = newList
                                                    draggedItemIndex = targetIndex
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            // Apply the changes
                                            onAppsReordered(reorderedApps)
                                            draggedItemIndex = null
                                            draggedItem = null
                                        },
                                        onDragCancel = {
                                            draggedItemIndex = null
                                            draggedItem = null
                                        }
                                    )
                                }
                            } else Modifier
                        )
                ) {
                    // Background indicator when dragging
                    if (editMode) {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {}
                    }

                    app?.let { appModel ->
                        AppItem(
                            app = appModel,
                            showApps = showAppIcons,
                            onClick = { onAppClick(appModel) },
                            onLongClick = { onAppLongPress(originalIndex) }
                        )
                    } ?: run {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "•••",
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}