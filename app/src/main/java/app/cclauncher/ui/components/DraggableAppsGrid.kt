package app.cclauncher.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
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
    var draggedItem by remember { mutableStateOf<Pair<Int, AppModel?>?>(null) }
    var draggedPosition by remember { mutableStateOf(Offset.Zero) }
    var targetPosition by remember { mutableStateOf<Int?>(null) }

    // Create indexed list of apps
    val indexedApps = remember(apps) { apps.mapIndexed { index, app -> index to app } }
    var reorderedApps by remember { mutableStateOf(indexedApps) }

    // Reset reordered apps when input changes
    LaunchedEffect(apps) {
        reorderedApps = apps.mapIndexed { index, app -> index to app }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
        modifier = Modifier.padding(16.dp)
    ) {
        itemsIndexed(reorderedApps) { gridIndex, (originalIndex, app) ->
            val isDragging = draggedItem?.first == originalIndex
            val elevation = animateDpAsState(
                if (isDragging) 8.dp else 0.dp,
                label = "elevation"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (isDragging) 1f else 0f)
                    .then(
                        if (editMode) {
                            Modifier.pointerInput(originalIndex) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        draggedItem = originalIndex to app
                                        draggedPosition = offset
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        draggedPosition += dragAmount

                                        // Calculate potential new position based on drag
                                        val cellWidth = size.width / columns
                                        val cellHeight = size.height / (reorderedApps.size / columns + 1)

                                        val newX = (draggedPosition.x / cellWidth).toInt()
                                        val newY = (draggedPosition.y / cellHeight).toInt()

                                        val newPosition = newY * columns + newX
                                        if (newPosition in reorderedApps.indices && newPosition != gridIndex) {
                                            targetPosition = newPosition
                                        }
                                    },
                                    onDragEnd = {
                                        // Swap positions if target is valid
                                        targetPosition?.let { target ->
                                            val newList = reorderedApps.toMutableList()
                                            val fromIndex = newList.indexOfFirst { it.first == originalIndex }
                                            val toIndex = target

                                            if (fromIndex != -1 && toIndex in newList.indices) {
                                                val item = newList.removeAt(fromIndex)
                                                newList.add(toIndex, item)
                                                reorderedApps = newList
                                                onAppsReordered(newList)
                                            }
                                        }

                                        draggedItem = null
                                        targetPosition = null
                                    },
                                    onDragCancel = {
                                        draggedItem = null
                                        targetPosition = null
                                    }
                                )
                            }
                        } else Modifier
                    )
            ) {
                app?.let { appModel ->
                    AppItem(
                        app = appModel,
                        showIcon = showAppIcons,
                        onClick = { onAppClick(appModel) },
                        onLongClick = { onAppLongPress(originalIndex) }
                    )
                } ?: run {
                    Text(
                        text = "•••",
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}