package app.cclauncher.ui.screens

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import app.cclauncher.MainViewModel
import app.cclauncher.data.Constants
import app.cclauncher.data.Constants.MAX_PAGES
import app.cclauncher.data.HomeItem
import app.cclauncher.data.HomeLayout
import app.cclauncher.helper.expandNotificationDrawer
import app.cclauncher.helper.getScreenDimensions
import app.cclauncher.helper.showToast
import app.cclauncher.helper.withResolvedUser
import app.cclauncher.settings.AppSettings
import app.cclauncher.ui.composables.HomeAppItem
import app.cclauncher.ui.composables.WidgetHostViewContainer
import app.cclauncher.ui.composables.WidgetSizeData
import app.cclauncher.ui.dialogs.ResizeDialog
import app.cclauncher.ui.util.detectSwipeGestures
import app.cclauncher.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    appWidgetHost: AppWidgetHost,
    onNavigateToAppDrawer: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val homeLayoutState by viewModel.homeLayoutState.collectAsState()
    val settings by settingsViewModel.settingsState.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()

    val pagerState = rememberPagerState(
        initialPage = currentPage,
        pageCount = { homeLayoutState.pageCount }
    )

    LaunchedEffect(pagerState.currentPage) {
        viewModel.setCurrentPage(pagerState.currentPage)
    }

    LaunchedEffect(currentPage) {
        if (pagerState.currentPage != currentPage) {
            pagerState.animateScrollToPage(currentPage)
        }
    }

    fun goToNextPage() {
        if (pagerState.currentPage < homeLayoutState.pageCount - 1) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    fun goToPreviousPage() {
        if (pagerState.currentPage > 0) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage - 1)
            }
        }
    }

    fun handleSwipeAction(action: Int, appLauncher: () -> Unit) {
        when (action) {
            Constants.SwipeAction.NOTIFICATIONS -> expandNotificationDrawer(context)
            Constants.SwipeAction.SEARCH -> onNavigateToAppDrawer()
            Constants.SwipeAction.APP -> appLauncher()
            Constants.SwipeAction.NEXT_PAGE -> goToNextPage()
            Constants.SwipeAction.PREVIOUS_PAGE -> goToPreviousPage()
            Constants.SwipeAction.NULL -> {}
        }
    }

    val onSwipeUp: () -> Unit = {
        handleSwipeAction(settings.swipeUpAction) { viewModel.launchSwipeUpApp() }
    }
    val onSwipeDown: () -> Unit = {
        handleSwipeAction(settings.swipeDownAction) { viewModel.launchSwipeDownApp() }
    }
    val onSwipeLeft: () -> Unit = {
        handleSwipeAction(settings.swipeLeftAction) { viewModel.launchSwipeLeftApp() }
    }
    val onSwipeRight: () -> Unit = {
        handleSwipeAction(settings.swipeRightAction) { viewModel.launchSwipeRightApp() }
    }

    var showAppContextMenu by remember { mutableStateOf<HomeItem.App?>(null) }
    var showWidgetContextMenu by remember { mutableStateOf<HomeItem.Widget?>(null) }
    var resizeDialogItem by remember { mutableStateOf<HomeItem?>(null) }

    // Simple movement tracking - no overlay needed
    var widgetBeingMoved by remember { mutableStateOf<HomeItem.Widget?>(null) }
    var appBeingMoved by remember { mutableStateOf<HomeItem.App?>(null) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionUp -> {
                            onSwipeDown()
                            true
                        }
                        Key.DirectionDown -> {
                            onSwipeUp()
                            true
                        }
                        Key.DirectionLeft -> {
                            onSwipeLeft()
                            true
                        }
                        Key.DirectionRight -> {
                            onSwipeRight()
                            true
                        }
                        Key.DirectionCenter, Key.Enter -> {
                            onNavigateToSettings()
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            userScrollEnabled = false,
            key = { page -> "page_$page" }
        ) { page ->
            HomeScreenPage(
                homeLayout = homeLayoutState,
                page = page,
                settings = settings,
                appWidgetHost = appWidgetHost,
                widgetBeingMoved = widgetBeingMoved,
                appBeingMoved = appBeingMoved,
                onAppClick = { item ->
                    viewModel.launchApp(item.appModel.withResolvedUser(context))
                },
                onAppLongPress = { item -> showAppContextMenu = item },
                onWidgetLongPress = { item -> showWidgetContextMenu = item },
                onEmptyLongPress = { onNavigateToSettings() },
                onDoubleTap = {
                    if (settings.doubleTapToLock) {
                        viewModel.lockScreen()
                    }
                },
                onSwipeUp = onSwipeUp,
                onSwipeDown = onSwipeDown,
                onSwipeLeft = onSwipeLeft,
                onSwipeRight = onSwipeRight,
                gestureSensitivity = settings.gestureSensitivity,
                onMoveToPosition = { item, row, col ->
                    when (item) {
                        is HomeItem.Widget -> {
                            viewModel.moveWidget(item, row, col)
                            widgetBeingMoved = null
                        }
                        is HomeItem.App -> {
                            viewModel.moveApp(item, row, col)
                            appBeingMoved = null
                        }
                    }
                },
                onCancelMovement = {
                    widgetBeingMoved = null
                    appBeingMoved = null
                }
            )
        }

        // Page indicators
        if (homeLayoutState.pageCount > 1 && settings.showPageIndicator) {
            PageIndicator(
                pageCount = homeLayoutState.pageCount,
                currentPage = pagerState.currentPage,
                onPageSelected = { page ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(page)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }

        // App context menu
        showAppContextMenu?.let { appItem ->
            val currentItem = homeLayoutState.items.find { it.id == appItem.id } as? HomeItem.App
            currentItem?.let {
                HomeAppContextMenu(
                    appItem = it,
                    pageCount = homeLayoutState.pageCount,
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
                    },
                    onMoveToPage = { app, targetPage ->
                        viewModel.moveItemToPage(app, targetPage)
                        showAppContextMenu = null
                    }
                )
            }
        }

        showWidgetContextMenu?.let { widgetItem ->
            val currentItem = homeLayoutState.items.find { it.id == widgetItem.id } as? HomeItem.Widget
            currentItem?.let {
                WidgetContextMenu(
                    widgetItem = it,
                    pageCount = homeLayoutState.pageCount,
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
                        context.showToast("Tap where you want to move the widget", Toast.LENGTH_SHORT)
                    },
                    onMoveToPage = { widget, targetPage ->
                        viewModel.moveItemToPage(widget, targetPage)
                        showWidgetContextMenu = null
                    }
                )
            }
        }

        // Resize dialog
        ResizeDialog(
            item = resizeDialogItem,
            currentRows = homeLayoutState.rows,
            currentColumns = homeLayoutState.columns,
            onDismiss = { resizeDialogItem = null },
            onResize = { item, newRowSpan, newColSpan ->
                when (item) {
                    is HomeItem.Widget -> {
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
}

@Composable
private fun HomeScreenPage(
    homeLayout: HomeLayout,
    page: Int,
    settings: AppSettings,
    appWidgetHost: AppWidgetHost,
    widgetBeingMoved: HomeItem.Widget?,
    appBeingMoved: HomeItem.App?,
    onAppClick: (HomeItem.App) -> Unit,
    onAppLongPress: (HomeItem.App) -> Unit,
    onWidgetLongPress: (HomeItem.Widget) -> Unit,
    onEmptyLongPress: () -> Unit,
    onDoubleTap: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    gestureSensitivity: Float,
    onMoveToPosition: (HomeItem, Int, Int) -> Unit,
    onCancelMovement: () -> Unit
) {
    val pageItems = remember(homeLayout.items, page) {
        homeLayout.itemsForPage(page)
    }

    val isMoving = widgetBeingMoved != null || appBeingMoved != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .detectSwipeGestures(
                sensitivity = gestureSensitivity,
                onSwipeUp = onSwipeUp,
                onSwipeDown = onSwipeDown,
                onSwipeLeft = onSwipeLeft,
                onSwipeRight = onSwipeRight
            )
            .pointerInput(widgetBeingMoved, appBeingMoved) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTap() },
                    onLongPress = { offset ->
                        if (isMoving) {
                            onCancelMovement()
                            return@detectTapGestures
                        }

                        val widget = findWidgetAtPosition(homeLayout, offset, size, page)
                        if (widget != null) {
                            onWidgetLongPress(widget)
                            return@detectTapGestures
                        }

                        val app = findAppAtPosition(homeLayout, offset, size, page)
                        if (app != null) {
                            onAppLongPress(app)
                            return@detectTapGestures
                        }

                        onEmptyLongPress()
                    },
                    onTap = { offset ->
                        if (isMoving) {
                            val gridPosition = calculateGridPosition(offset, homeLayout, size)
                            if (gridPosition != null) {
                                val item: HomeItem? = widgetBeingMoved ?: appBeingMoved
                                item?.let { onMoveToPosition(it, gridPosition.first, gridPosition.second) }
                            }
                        }
                    }
                )
            }
    ) {
        HomeScreenContent(
            homeLayout = homeLayout,
            pageItems = pageItems,
            settings = settings,
            appWidgetHost = appWidgetHost,
            widgetBeingMoved = widgetBeingMoved,
            appBeingMoved = appBeingMoved,
            onAppClick = onAppClick,
            onAppLongPress = onAppLongPress,
            onWidgetLongPress = onWidgetLongPress
        )
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun HomeScreenContent(
    homeLayout: HomeLayout,
    pageItems: List<HomeItem>,
    settings: AppSettings,
    appWidgetHost: AppWidgetHost,
    widgetBeingMoved: HomeItem.Widget?,
    appBeingMoved: HomeItem.App?,
    onAppClick: (HomeItem.App) -> Unit,
    onAppLongPress: (HomeItem.App) -> Unit,
    onWidgetLongPress: (HomeItem.Widget) -> Unit
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val widgetManager = AppWidgetManager.getInstance(context)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val parentWidthDp = maxWidth
        val parentHeightDp = maxHeight

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
            val refs = pageItems.associate { it.id to createRef() }

            pageItems.forEach { item ->
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
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
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
                        val isBeingMoved = widgetBeingMoved?.id == item.id

                        val widgetModifier = if (isBeingMoved) {
                            itemModifier
                                .padding(2.dp)
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                .alpha(0.7f)
                        } else {
                            itemModifier.padding(2.dp)
                        }

                        val providerInfo = remember(item.packageName, item.providerClassName) {
                            widgetManager.installedProviders.find {
                                it.provider.packageName == item.packageName &&
                                        it.provider.className == item.providerClassName
                            }
                        }

                        if (providerInfo != null) {
                            val sizeData = remember(item.columnSpan, item.rowSpan, cellWidth, cellHeight, density) {
                                with(density) {
                                    val wDp = cellWidth * item.columnSpan
                                    val hDp = cellHeight * item.rowSpan
                                    WidgetSizeData(
                                        width = wDp.toPx().roundToInt(),
                                        height = hDp.toPx().roundToInt(),
                                        minWidthDp = wDp,
                                        maxWidthDp = wDp,
                                        minHeightDp = hDp,
                                        maxHeightDp = hDp
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
                            Box(itemModifier)
                            Log.w("HomeScreen", "Provider not found for widget ID ${item.appWidgetId}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { page ->
            val isSelected = page == currentPage
            Box(
                modifier = Modifier
                    .size(if (isSelected) 12.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onPageSelected(page) }
            )
        }
    }
}

@Composable
fun WidgetContextMenu(
    widgetItem: HomeItem.Widget?,
    pageCount: Int = 1,
    onDismiss: () -> Unit,
    onRemove: (HomeItem.Widget) -> Unit,
    onResize: (HomeItem.Widget) -> Unit,
    onConfigure: (HomeItem.Widget) -> Unit,
    onMove: (HomeItem.Widget) -> Unit,
    onMoveToPage: (HomeItem.Widget, Int) -> Unit
) {
    if (widgetItem == null) return

    val context = LocalContext.current
    val widgetManager = AppWidgetManager.getInstance(context)
    val providerInfo = remember(widgetItem) {
        widgetManager.installedProviders.find {
            it.provider.packageName == widgetItem.packageName &&
                    it.provider.className == widgetItem.providerClassName
        }
    }
    val canReconfigure = providerInfo?.configure != null

    var showPageSelector by remember { mutableStateOf(false) }

    if (showPageSelector) {
        PageSelectorDialog(
            currentItemPage = widgetItem.page,
            pageCount = pageCount,
            onDismiss = { showPageSelector = false },
            onPageSelected = { targetPage ->
                onMoveToPage(widgetItem, targetPage)
                showPageSelector = false
                onDismiss()
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Widget Options") },
            text = {
                Column {
                    DropdownMenuItem(
                        text = { Text("Move") },
                        onClick = { onMove(widgetItem); onDismiss() }
                    )
                    if (pageCount > 1 || pageCount < MAX_PAGES) {
                        DropdownMenuItem(
                            text = { Text("Move to page...") },
                            onClick = { showPageSelector = true }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Resize") },
                        onClick = { onResize(widgetItem); onDismiss() }
                    )
                    if (canReconfigure) {
                        DropdownMenuItem(
                            text = { Text("Configure") },
                            onClick = { onConfigure(widgetItem); onDismiss() }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Remove") },
                        onClick = { onRemove(widgetItem); onDismiss() }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        )
    }
}

@Composable
fun HomeAppContextMenu(
    appItem: HomeItem.App,
    pageCount: Int = 1,
    onDismiss: () -> Unit,
    onRemove: (HomeItem.App) -> Unit,
    onResize: (HomeItem.App) -> Unit,
    onMove: (HomeItem.App) -> Unit,
    onMoveToPage: (HomeItem.App, Int) -> Unit
) {
    var showPageSelector by remember { mutableStateOf(false) }

    if (showPageSelector) {
        PageSelectorDialog(
            currentItemPage = appItem.page,
            pageCount = pageCount,
            onDismiss = { showPageSelector = false },
            onPageSelected = { targetPage ->
                onMoveToPage(appItem, targetPage)
                showPageSelector = false
                onDismiss()
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("App Options") },
            text = {
                Column {
                    DropdownMenuItem(
                        text = { Text("Move") },
                        onClick = { onMove(appItem); onDismiss() }
                    )
                    if (pageCount > 1 || pageCount < MAX_PAGES) {
                        DropdownMenuItem(
                            text = { Text("Move to page...") },
                            onClick = { showPageSelector = true }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Resize") },
                        onClick = { onResize(appItem); onDismiss() }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove") },
                        onClick = { onRemove(appItem); onDismiss() }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        )
    }
}

@Composable
private fun PageSelectorDialog(
    currentItemPage: Int,
    pageCount: Int,
    onDismiss: () -> Unit,
    onPageSelected: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to Page") },
        text = {
            Column {
                repeat(pageCount) { page ->
                    val isCurrentPage = page == currentItemPage
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Page ${page + 1}${if (isCurrentPage) " (current)" else ""}",
                                color = if (isCurrentPage) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = { if (!isCurrentPage) onPageSelected(page) },
                        enabled = !isCurrentPage
                    )
                }
                if (pageCount < MAX_PAGES) {
                    DropdownMenuItem(
                        text = { Text("+ New Page") },
                        onClick = { onPageSelected(pageCount) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// Helper functions
private fun findAppAtPosition(
    homeLayout: HomeLayout,
    position: Offset,
    size: IntSize,
    page: Int
): HomeItem.App? {
    val cellWidth = size.width.toFloat() / homeLayout.columns
    val cellHeight = size.height.toFloat() / homeLayout.rows
    val column = (position.x / cellWidth).toInt()
    val row = (position.y / cellHeight).toInt()

    return homeLayout.itemsForPage(page).filterIsInstance<HomeItem.App>().find { app ->
        row >= app.row && row < app.row + app.rowSpan &&
                column >= app.column && column < app.column + app.columnSpan
    }
}

private fun findWidgetAtPosition(
    homeLayout: HomeLayout,
    position: Offset,
    size: IntSize,
    page: Int
): HomeItem.Widget? {
    val cellWidth = size.width.toFloat() / homeLayout.columns
    val cellHeight = size.height.toFloat() / homeLayout.rows
    val column = (position.x / cellWidth).toInt()
    val row = (position.y / cellHeight).toInt()

    return homeLayout.itemsForPage(page).filterIsInstance<HomeItem.Widget>().find { widget ->
        row >= widget.row && row < widget.row + widget.rowSpan &&
                column >= widget.column && column < widget.column + widget.columnSpan
    }
}

private fun calculateGridPosition(
    position: Offset,
    homeLayout: HomeLayout,
    size: IntSize
): Pair<Int, Int>? {
    val cellWidth = size.width.toFloat() / homeLayout.columns
    val cellHeight = size.height.toFloat() / homeLayout.rows
    val column = (position.x / cellWidth).toInt()
    val row = (position.y / cellHeight).toInt()

    return if (row in 0 until homeLayout.rows && column in 0 until homeLayout.columns) {
        Pair(row, column)
    } else null
}