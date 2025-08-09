package app.cclauncher.ui.screens

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import app.cclauncher.MainViewModel
import app.cclauncher.data.Constants
import app.cclauncher.data.HomeItem
import app.cclauncher.data.HomeLayout
import app.cclauncher.data.settings.AppSettings
import app.cclauncher.helper.expandNotificationDrawer
import app.cclauncher.helper.getScreenDimensions
import app.cclauncher.helper.showToast
import app.cclauncher.ui.composables.HomeAppItem
import app.cclauncher.ui.composables.WidgetHostViewContainer
import app.cclauncher.ui.composables.WidgetSizeData
import app.cclauncher.ui.dialogs.ResizeDialog
import app.cclauncher.ui.util.detectSwipeGestures
import app.cclauncher.ui.viewmodels.SettingsViewModel
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    appWidgetHost: AppWidgetHost,
    onNavigateToAppDrawer: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val context = LocalContext.current
    val homeLayoutState by viewModel.homeLayoutState.collectAsState()
    val settings by settingsViewModel.settingsState.collectAsState()

    var showAppContextMenu by remember { mutableStateOf<HomeItem.App?>(null) }
    var appBeingMoved by remember { mutableStateOf<HomeItem.App?>(null) }

    var showWidgetContextMenu by remember { mutableStateOf<HomeItem.Widget?>(null) }
    var resizeDialogItem by remember { mutableStateOf<HomeItem?>(null) }


    // Add state for widget movement
    var widgetBeingMoved by remember { mutableStateOf<HomeItem.Widget?>(null) }

    // Store touch position for hit testing
    var lastTouchPosition by remember { mutableStateOf(Offset.Zero) }
    
    // Add a way to cancel movement mode
    var showCancelMovementButton by remember { mutableStateOf(false) }
    
    // Update this when any movement starts
    LaunchedEffect(widgetBeingMoved, appBeingMoved) {
        showCancelMovementButton = widgetBeingMoved != null || appBeingMoved != null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .detectSwipeGestures(
                onSwipeUp = { when (settings.swipeUpAction) {
                    Constants.SwipeAction.NOTIFICATIONS -> expandNotificationDrawer(context)
                    Constants.SwipeAction.SEARCH -> onNavigateToAppDrawer()
                    Constants.SwipeAction.APP -> viewModel.launchSwipeUpApp()
                    Constants.SwipeAction.NULL -> {}
                        else -> onNavigateToAppDrawer()
                } },
                onSwipeDown = {
                    when (settings.swipeDownAction) {
                        Constants.SwipeAction.NOTIFICATIONS -> expandNotificationDrawer(context)
                        Constants.SwipeAction.SEARCH -> onNavigateToAppDrawer()
                        Constants.SwipeAction.APP -> viewModel.launchSwipeDownApp()
                        Constants.SwipeAction.NULL -> {}
                        else -> expandNotificationDrawer(context)
                    }
                },
                onSwipeLeft = {
                    when (settings.swipeLeftAction) {
                        Constants.SwipeAction.NOTIFICATIONS -> expandNotificationDrawer(context)
                        Constants.SwipeAction.SEARCH -> onNavigateToAppDrawer()
                        Constants.SwipeAction.APP -> viewModel.launchSwipeLeftApp()
                        Constants.SwipeAction.NULL -> { /* Do nothing */ }
                        else -> { /* Do nothing by default */ }
                    }
                },
                onSwipeRight = {
                    when (settings.swipeRightAction) {
                        Constants.SwipeAction.NOTIFICATIONS -> expandNotificationDrawer(context)
                        Constants.SwipeAction.SEARCH -> onNavigateToAppDrawer()
                        Constants.SwipeAction.APP -> viewModel.launchSwipeRightApp()
                        Constants.SwipeAction.NULL -> {}
                        else -> {}
                    }
                }
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (settings.doubleTapToLock) {
                            viewModel.lockScreen()
                        }
                    },
                    onLongPress = { offset ->
                        // Store the touch position for hit testing
                        lastTouchPosition = offset

                        // If we're in movement mode, cancel it first
                        if (widgetBeingMoved != null || appBeingMoved != null) {
                            widgetBeingMoved = null
                            appBeingMoved = null
                            return@detectTapGestures
                        }

                        // Find which widget was long-pressed
                        val widget = findWidgetAtPosition(homeLayoutState, offset, this.size)
                        if (widget != null) {
                            // Show context menu for this widget
                            showWidgetContextMenu = widget
                        } else {
                            // Check if we long-pressed on an app
                            val app = findAppAtPosition(homeLayoutState, offset, this.size)
                            if (app != null) {
                                showAppContextMenu = app
                            } else {
                                // Long press on empty space, go to settings
                                onNavigateToSettings()
                            }
                        }
                    },
                    onTap = { offset ->
                        // If we're in widget movement mode, handle the tap as a move destination
                        if (widgetBeingMoved != null) {
                            // Calculate the grid position from the tap location
                            val gridPosition = calculateGridPosition(offset, homeLayoutState, this.size)
                            if (gridPosition != null) {
                                // Move the widget to this position
                                viewModel.moveWidget(widgetBeingMoved!!, gridPosition.first, gridPosition.second)
                                // Exit movement mode
                                widgetBeingMoved = null
                            }
                        } else if (appBeingMoved != null) {
                            val gridPosition = calculateGridPosition(offset, homeLayoutState, this.size)
                            if (gridPosition != null) {
                                viewModel.moveApp(appBeingMoved!!, gridPosition.first, gridPosition.second)
                                appBeingMoved = null
                            }
                        }
                    }
                )
            }
    ) {
        HomeScreenContent(
            homeLayout = homeLayoutState,
            settings = settings,
            appWidgetHost = appWidgetHost,
            onAppClick = { item -> viewModel.launchApp(item.appModel) },
            onAppLongPress = { item -> showAppContextMenu = item },
            onWidgetLongPress = { item -> showWidgetContextMenu = item },
            widgetBeingMoved = widgetBeingMoved,
            appBeingMoved = appBeingMoved
        )

        // Show visual indicator if a widget or app is being moved
        if (widgetBeingMoved != null || appBeingMoved != null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Tap where you want to move the ${if (widgetBeingMoved != null) "widget" else "app"}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(16.dp)
                    )
                }
                
                // Add a cancel button at the bottom
                TextButton(
                    onClick = {
                        widgetBeingMoved = null
                        appBeingMoved = null
                    },
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Cancel Movement",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        showAppContextMenu?.let { appItem ->
            // Find the current item from state
            val currentItem = homeLayoutState.items.find { it.id == appItem.id } as? HomeItem.App

            currentItem?.let {
                HomeAppContextMenu(
                    appItem = it,  // Use the current item from state
                    onDismiss = { showAppContextMenu = null },
                    onRemove = { app ->
                        viewModel.removeAppFromHomeScreen(app)
                        showAppContextMenu = null
                    },
                    onResize = { app ->
                        resizeDialogItem = app
                        showAppContextMenu = null
                    },
                    onMove = { app ->
                        appBeingMoved = app
                        showAppContextMenu = null
                        context.showToast("Tap where you want to move the app")
                    }
                )
            }
        }


    }

    showWidgetContextMenu?.let { widgetItem ->
        val currentItem = homeLayoutState.items.find { it.id == widgetItem.id } as? HomeItem.Widget

        currentItem?.let {
            WidgetContextMenu(
                widgetItem = it,
                onDismiss = { showWidgetContextMenu = null },
                onRemove = { widget ->
                    appWidgetHost.deleteAppWidgetId(widget.appWidgetId)
                    viewModel.removeWidget(widget)
                    showWidgetContextMenu = null
                },
                onResize = { widget ->
                    resizeDialogItem = widget
                    showWidgetContextMenu = null
                },
                onConfigure = { widget ->
                    viewModel.requestWidgetReconfigure(widget)
                    showWidgetContextMenu = null
                },
                onMove = { widget ->
                    widgetBeingMoved = widget
                    showWidgetContextMenu = null
                    context.showToast( "Tap where you want to move the widget", Toast.LENGTH_SHORT)
                }
            )
        }
    }
// Not needed, sliders work as intended without this
//    resizeDialogItem?.let { item ->
//        // Get the fresh item from current state
//        val currentItem = homeLayoutState.items.find { it.id == item.id }
//
//        currentItem?.let {
//            ResizeDialog(
//                item = it,  // Use the current item from state
//                currentRows = homeLayoutState.rows,
//                currentColumns = homeLayoutState.columns,
//                onDismiss = { resizeDialogItem = null },
//                onResize = { item, newRowSpan, newColSpan ->
//                    when (item) {
//                        is HomeItem.Widget -> {
//                            viewModel.resizeWidget(item, newRowSpan, newColSpan)
//                        }
//                        is HomeItem.App -> {
//                            viewModel.resizeApp(item, newRowSpan, newColSpan)
//                        }
//                    }
//                }
//            )
//        }
//    }

    ResizeDialog(
        item = resizeDialogItem,
        currentRows = homeLayoutState.rows,
        currentColumns = homeLayoutState.columns,
        onDismiss = { resizeDialogItem = null },
        onResize = { item, newRowSpan, newColSpan ->
            when (item) {
                is HomeItem.Widget -> {
                    // Calculate actual pixel sizes based on grid
                    val screenDimensions = getScreenDimensions(context)
                    val cellWidth = screenDimensions.first / homeLayoutState.columns
                    val cellHeight = screenDimensions.second / homeLayoutState.rows

                    val widgetWidthDp = (cellWidth * newColSpan)
                    val widgetHeightDp = (cellHeight * newRowSpan)

                    val options = Bundle().apply {
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widgetWidthDp)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widgetWidthDp)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, widgetHeightDp)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, widgetHeightDp)
                    }
                    viewModel.appWidgetManager.updateAppWidgetOptions(item.appWidgetId, options)
                    viewModel.resizeWidget(item, newRowSpan, newColSpan)
                }
                is HomeItem.App -> {
                    viewModel.resizeApp(item, newRowSpan, newColSpan)
                }
            }
        }
    )
}

    // helper function to find app at position
    private fun findAppAtPosition(
        homeLayout: HomeLayout,
        position: Offset,
        size: IntSize
    ): HomeItem.App? {
        // Calculate cell size
        val cellWidth = size.width.toFloat() / homeLayout.columns
        val cellHeight = size.height.toFloat() / homeLayout.rows

        // Calculate which grid cell was clicked
        val column = (position.x / cellWidth).toInt()
        val row = (position.y / cellHeight).toInt()

        // Find an app that contains this cell
        return homeLayout.items.filterIsInstance<HomeItem.App>().find { app ->
            row >= app.row &&
                    row < app.row + app.rowSpan &&
                    column >= app.column &&
                    column < app.column + app.columnSpan
        }
    }

private fun findWidgetAtPosition(
    homeLayout: HomeLayout,
    position: Offset,
    size: IntSize
): HomeItem.Widget? {
    // Calculate cell size
    val cellWidth = size.width.toFloat() / homeLayout.columns
    val cellHeight = size.height.toFloat() / homeLayout.rows

    // Calculate which grid cell was clicked
    val column = (position.x / cellWidth).toInt()
    val row = (position.y / cellHeight).toInt()

    // Find a widget that contains this cell
    return homeLayout.items.filterIsInstance<HomeItem.Widget>().find { widget ->
        row >= widget.row &&
                row < widget.row + widget.rowSpan &&
                column >= widget.column &&
                column < widget.column + widget.columnSpan
    }
}

// Helper function to calculate grid position from screen position
private fun calculateGridPosition(
    position: Offset,
    homeLayout: HomeLayout,
    size: IntSize
): Pair<Int, Int>? {
    // Calculate cell size
    val cellWidth = size.width.toFloat() / homeLayout.columns
    val cellHeight = size.height.toFloat() / homeLayout.rows

    // Calculate which grid cell was clicked
    val column = (position.x / cellWidth).toInt()
    val row = (position.y / cellHeight).toInt()

    // Ensure the position is within grid bounds
    if (row >= 0 && row < homeLayout.rows && column >= 0 && column < homeLayout.columns) {
        return Pair(row, column)
    }

    return null
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun HomeScreenContent(
    homeLayout: HomeLayout,
    settings: AppSettings,
    appWidgetHost: AppWidgetHost,
    onAppClick: (HomeItem.App) -> Unit,
    onAppLongPress: (HomeItem.App) -> Unit,
    onWidgetLongPress: (HomeItem.Widget) -> Unit,
    widgetBeingMoved: HomeItem.Widget? = null,
    appBeingMoved: HomeItem.App? = null
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val parentWidthDp = maxWidth
        val parentHeightDp = maxHeight

        // Define consistent padding
        val horizontalPadding = 16.dp
        val verticalPadding = 16.dp
        val usableWidth = parentWidthDp - horizontalPadding * 2
        val usableHeight = parentHeightDp - verticalPadding * 2

        val cellWidth = usableWidth / homeLayout.columns
        val cellHeight = usableHeight / homeLayout.rows

        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
            val refs = homeLayout.items.associate { it.id to createRef() }
            val widgetManager = AppWidgetManager.getInstance(LocalContext.current)

            homeLayout.items.forEach { item ->
                val itemModifier = Modifier.constrainAs(refs.getValue(item.id)) {

                    top.linkTo(parent.top, margin = cellHeight * item.row)
                    start.linkTo(parent.start, margin = cellWidth * item.column)

                    width = androidx.constraintlayout.compose.Dimension.value(cellWidth * item.columnSpan)
                    height = androidx.constraintlayout.compose.Dimension.value(cellHeight * item.rowSpan)
                }

                when (item) {
                    is HomeItem.App -> {

                        val isBeingMoved = appBeingMoved?.id == item.id
                
                        val appModifier = if (isBeingMoved) {
                            itemModifier
                                .padding(2.dp)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .alpha(0.7f)
                        } else {
                            itemModifier.padding(2.dp)
                        }
                        
                        HomeAppItem(
                            modifier = appModifier,
                            app = item.appModel,
                            settings = settings,
                            appWidth = cellWidth * item.columnSpan,
                            appHeight = cellHeight * item.rowSpan,
                            onClick = { onAppClick(item) },
                            onLongClick = { onAppLongPress(item) }
                        )
                    }

                    is HomeItem.Widget -> {
                        // Add visual indicator if this widget is being moved
                        val isBeingMoved = widgetBeingMoved?.id == item.id

                        val widgetModifier = if (isBeingMoved) {
                            itemModifier
                                .padding(2.dp)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .alpha(0.7f)
                        } else {
                            itemModifier.padding(2.dp)
                        }

                        val providerInfo = remember(item.packageName, item.providerClassName) {
                            // Lookup provider info at runtime
                            widgetManager.installedProviders.find {
                                it.provider.packageName == item.packageName && it.provider.className == item.providerClassName
                            }
                        }

                        if (providerInfo != null) {
                            val sizeData = remember(
                                item.columnSpan,
                                item.rowSpan,
                                cellWidth,
                                cellHeight,
                                density
                            ) {
                                with(density) {
                                    // Calculate sizes based on determined cell dimensions
                                    val wDp = cellWidth * item.columnSpan
                                    val hDp = cellHeight * item.rowSpan
                                    WidgetSizeData(
                                        width = wDp.toPx().roundToInt(),
                                        height = hDp.toPx().roundToInt(),
                                        minWidthDp = wDp, maxWidthDp = wDp,
                                        minHeightDp = hDp, maxHeightDp = hDp
                                    )
                                }
                            }
                            WidgetHostViewContainer(
                                modifier = widgetModifier,
                                appWidgetId = item.appWidgetId,
                                providerInfo = providerInfo,
                                appWidgetHost = appWidgetHost,
                                widgetSizeData = sizeData,
                                onLongPress = { onWidgetLongPress(item) }
                            )
                        } else {
                            Box(itemModifier) { /* missing widget so...? */ }
                            Log.w(
                                "HomeScreen",
                                "Provider not found for widget ID ${item.appWidgetId}"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetContextMenu(
    widgetItem: HomeItem.Widget?,
    onDismiss: () -> Unit,
    onRemove: (HomeItem.Widget) -> Unit,
    onResize: (HomeItem.Widget) -> Unit,
    onConfigure: (HomeItem.Widget) -> Unit,
    onMove: (HomeItem.Widget) -> Unit
) {
    if (widgetItem == null) return

    val context = LocalContext.current
    val widgetManager = AppWidgetManager.getInstance(context)
    val providerInfo = remember(widgetItem) {
        widgetManager.installedProviders.find {
            it.provider.packageName == widgetItem.packageName && it.provider.className == widgetItem.providerClassName
        }
    }
    val canReconfigure = providerInfo?.configure != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Widget Options") },
        text = {
            Column {
                DropdownMenuItem(text = { Text("Move") }, onClick = { onMove(widgetItem); onDismiss() })
                DropdownMenuItem(text = { Text("Resize") }, onClick = { onResize(widgetItem); onDismiss() })
                if (canReconfigure) {
                    DropdownMenuItem(text = { Text("Configure") }, onClick = { onConfigure(widgetItem); onDismiss() })
                }
                DropdownMenuItem(text = { Text("Remove") }, onClick = { onRemove(widgetItem); onDismiss() })
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun HomeAppContextMenu(
    appItem: HomeItem.App,
    onDismiss: () -> Unit,
    onRemove: (HomeItem.App) -> Unit,
    onResize: (HomeItem.App) -> Unit,
    onMove: (HomeItem.App) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("App Options") },
        text = {
            Column {
                DropdownMenuItem(text = { Text("Move") }, onClick = { onMove(appItem); onDismiss() })
                DropdownMenuItem(text = { Text("Resize") }, onClick = { onResize(appItem); onDismiss() })
                DropdownMenuItem(text = { Text("Remove") }, onClick = { onRemove(appItem); onDismiss() })
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}