package app.cclauncher.ui.screens

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import app.cclauncher.MainViewModel
import app.cclauncher.data.Constants
import app.cclauncher.data.HomeItem
import app.cclauncher.data.HomeLayout
import app.cclauncher.data.settings.AppSettings
import app.cclauncher.helper.openAlarmApp
import app.cclauncher.helper.openCalendar
import app.cclauncher.ui.composables.HomeAppItem
import app.cclauncher.ui.composables.WidgetHostViewContainer
import app.cclauncher.ui.composables.WidgetSizeData
import app.cclauncher.ui.dialogs.ResizeWidgetDialog
import app.cclauncher.ui.util.detectSwipeGestures
import app.cclauncher.ui.viewmodels.SettingsViewModel
import java.util.Date
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    appWidgetHost: AppWidgetHost,
    onNavigateToAppDrawer: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToWidgetPicker: () -> Unit
) {
    val context = LocalContext.current
    val homeLayoutState by viewModel.homeLayoutState.collectAsState()
    val settings by settingsViewModel.settingsState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showWidgetContextMenu by remember { mutableStateOf<HomeItem.Widget?>(null) }
    var showResizeDialog by remember { mutableStateOf<HomeItem.Widget?>(null) }


    val currentDate = remember { mutableStateOf(Date()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .detectSwipeGestures(
                onSwipeUp = { onNavigateToAppDrawer() },
                onSwipeDown = {
                    when (settings.swipeDownAction) {
                        Constants.SwipeDownAction.NOTIFICATIONS -> app.cclauncher.helper.expandNotificationDrawer(context)
                        Constants.SwipeDownAction.SEARCH -> onNavigateToAppDrawer()
                        else -> app.cclauncher.helper.expandNotificationDrawer(context)
                    }
                },
                onSwipeLeft = { viewModel.launchSwipeLeftApp() },
                onSwipeRight = { viewModel.launchSwipeRightApp() }
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { if (settings.doubleTapToLock)
                                    { viewModel.lockScreen() }
                                  },
                    onLongPress = { offset ->
                        // Check if long press is on empty space to trigger widget contextbox to resize or remove
                        // This needs hit testing against item bounds, complex.
                        // Alternative: Add a dedicated button or settings option.
                        // For now, let's trigger via settings or a FAB maybe.
                        onNavigateToSettings() // Keep existing long press for settings for now
                    },
                    onTap = { /* ... */ }
                )
            }
    ) {
        HomeScreenContent(
            homeLayout = homeLayoutState,
            settings = settings,
            currentDate = currentDate.value,
            appWidgetHost = appWidgetHost,
            onAppClick = { item -> viewModel.launchApp(item.appModel) },
            onAppLongPress = { item -> /* Show app context menu? Remove from home? */
                viewModel.removeAppFromHomeScreen(item) // Example action
                Toast.makeText(context, "Removed ${item.appModel.appLabel}", Toast.LENGTH_SHORT).show()
            },
            onWidgetLongPress = { item -> showWidgetContextMenu = item },
            onTimeClick = { openAlarmApp(context) },
            onDateClick = { openCalendar(context) },
        )
    }

    WidgetContextMenu(
        widgetItem = showWidgetContextMenu,
        onDismiss = { showWidgetContextMenu = null },
        onRemove = { widget -> viewModel.removeWidget(widget) },
        onResize = { widget -> showResizeDialog = widget },
        onConfigure = { widget -> viewModel.requestWidgetReconfigure(widget) }
    )

    ResizeWidgetDialog(
        widgetItem = showResizeDialog,
        currentRows = homeLayoutState.rows,
        currentColumns = homeLayoutState.columns,
        onDismiss = { showResizeDialog = null },
        onResize = { widget, newRowSpan, newColSpan ->
            viewModel.resizeWidget(widget, newRowSpan, newColSpan)
        }
    )
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun HomeScreenContent(
    homeLayout: HomeLayout,
    settings: AppSettings,
    currentDate: Date,
    appWidgetHost: AppWidgetHost,
    onAppClick: (HomeItem.App) -> Unit,
    onAppLongPress: (HomeItem.App) -> Unit,
    onWidgetLongPress: (HomeItem.Widget) -> Unit,
    onTimeClick: () -> Unit,
    onDateClick: () -> Unit,
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val parentWidthDp = maxWidth
        val parentHeightDp = maxHeight

        // TODO: Calculate cell size based on available space and padding
        val horizontalPadding = 16.dp
        val verticalPadding = 16.dp
        val usableWidth = parentWidthDp - horizontalPadding * 2
        val usableHeight = parentHeightDp - verticalPadding * 2 // Adjust if date/time takes space

        val cellWidth = usableWidth / homeLayout.columns
        val cellHeight = usableHeight / homeLayout.rows
        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            val refs = homeLayout.items.associate { it.id to createRef() }
            val widgetManager = AppWidgetManager.getInstance(LocalContext.current)

            homeLayout.items.forEach { item ->
                val itemModifier = Modifier.constrainAs(refs.getValue(item.id)) {
                    // Position based on row/column
                    top.linkTo(parent.top, margin = item.row.dp * cellHeight.value)
                    start.linkTo(parent.start, margin = item.column.dp * cellWidth.value)
                    // Size based on span
                    width =
                        androidx.constraintlayout.compose.Dimension.value(item.columnSpan.dp * cellWidth.value)
                    height =
                        androidx.constraintlayout.compose.Dimension.value(item.rowSpan.dp * cellHeight.value)
                }

                when (item) {
                    is HomeItem.App -> {
                        HomeAppItem(
                            modifier = itemModifier.padding(4.dp),
                            app = item.appModel,
                            settings = settings,
                            onClick = { onAppClick(item) },
                            onLongClick = { onAppLongPress(item) }
                        )
                    }

                    is HomeItem.Widget -> {
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
                                    val wDp = item.columnSpan.dp * cellWidth.value
                                    val hDp = item.rowSpan.dp * cellHeight.value
                                    WidgetSizeData(
                                        width = wDp.toPx().roundToInt(),
                                        height = hDp.toPx().roundToInt(),
                                        minWidthDp = wDp, maxWidthDp = wDp, // Pass cell-based size
                                        minHeightDp = hDp, maxHeightDp = hDp
                                    )
                                }
                            }
                            WidgetHostViewContainer(
                                modifier = itemModifier.padding(2.dp),
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
                            // TODO: auto-removing this item from layout state
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
    onConfigure: (HomeItem.Widget) -> Unit
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
        title = { Text("Widget Options") }, // Use widget label? providerInfo?.loadLabel(context.packageManager)
        text = {
            Column {
                DropdownMenuItem(text = { Text("Remove") }, onClick = { onRemove(widgetItem); onDismiss() })
                DropdownMenuItem(text = { Text("Resize") }, onClick = { onResize(widgetItem); onDismiss() })
                if (canReconfigure) {
                    DropdownMenuItem(text = { Text("Configure") }, onClick = { onConfigure(widgetItem); onDismiss() })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

