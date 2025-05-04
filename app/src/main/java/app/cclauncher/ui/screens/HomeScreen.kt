package app.cclauncher.ui.screens

import android.content.res.Configuration
import android.view.Gravity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cclauncher.MainViewModel
import app.cclauncher.data.Constants
import app.cclauncher.helper.openAlarmApp
import app.cclauncher.helper.openCalendar
import app.cclauncher.ui.AppSelectionType
import app.cclauncher.ui.UiEvent
import app.cclauncher.ui.components.DraggableAppsGrid
import app.cclauncher.ui.components.DraggableWidgetContainer
import app.cclauncher.ui.util.detectSwipeGestures
import app.cclauncher.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.input.pointer.pointerInput
import app.cclauncher.data.HomeAppPreference

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateToAppDrawer: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.homeScreenState.collectAsState()
    val settings by settingsViewModel.settingsState.collectAsState()
    val scope = rememberCoroutineScope()

    // Date/time state
    val currentDate = remember { mutableStateOf(Date()) }
    val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
    val dateText = dateFormat.format(currentDate.value).replace(".,", ",")

    // Orientation and display settings
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val shouldShowIcons = settings.showHomeScreenIcons &&
            (if (isLandscape) settings.showIconsInLandscape else settings.showIconsInPortrait)

    // Font settings
    val fontWeight = when (settings.fontWeight) {
        0 -> FontWeight.Thin
        1 -> FontWeight.Light
        2 -> FontWeight.Normal
        3 -> FontWeight.Medium
        4 -> FontWeight.Bold
        5 -> FontWeight.Black
        else -> FontWeight.Normal
    }

    // Item spacing
    val itemSpacing = when (settings.itemSpacing) {
        0 -> 0.dp
        1 -> 4.dp
        2 -> 8.dp
        3 -> 16.dp
        else -> 8.dp
    }

    // Update time every minute
    LaunchedEffect(key1 = Unit) {
        while(true) {
            currentDate.value = Date()
            kotlinx.coroutines.delay(60000)
        }
    }

    // Loading state
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Main content
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
                    onDoubleTap = {
                        if (settings.doubleTapToLock) {
                            viewModel.lockScreen()
                        }
                    },
                    onLongPress = { onNavigateToSettings() }
                )
            }
    ) {
        // Main column with all content
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = when (settings.homeAlignment) {
                Gravity.START -> Alignment.Start
                Gravity.END -> Alignment.End
                else -> Alignment.CenterHorizontally
            }
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Widget section - only show if widgets exist
            val preferences by viewModel.prefsDataStore.preferences.collectAsState(initial = null)
            preferences?.externalWidgets?.let { widgets ->
                if (widgets.isNotEmpty()) {

                    // Widget container
                    DraggableWidgetContainer(
                        widgets = widgets,
                        editMode = settings.editWidgets,
                        onWidgetsReordered = { reorderedWidgets ->
                            scope.launch {
                                viewModel.updateWidgetOrder(reorderedWidgets)
                            }
                        },
                        onConfigureWidget = { widget ->
                            viewModel.configureExistingWidget(widget)
                        },
                        onRemoveWidget = { widgetId ->
                            scope.launch {
                                val widget = widgets.find { it.id == widgetId }
                                widget?.let {
                                    val widgetHelper = app.cclauncher.helper.WidgetHelper(context)
                                    widgetHelper.deleteWidget(it.appWidgetId)
                                    viewModel.removeExternalWidget(widgetId)
                                }
                            }
                        },
                        onResizeWidget = { widget, newWidth, newHeight ->
                            scope.launch {
                                viewModel.resizeWidget(widget, newWidth, newHeight)
                            }
                        },
                        settingsViewModel = settingsViewModel,
                        mainViewModel = viewModel
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Spacer that pushes content down or centers it
            if (settings.homeBottomAlignment) {
                Spacer(modifier = Modifier.weight(1f))
            } else {
                Spacer(modifier = Modifier.weight(0.5f))
            }

            // Date/time section
            if (settings.dateTimeVisibility != Constants.DateTime.OFF) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = when (settings.homeAlignment) {
                        Gravity.START -> Alignment.Start
                        Gravity.END -> Alignment.End
                        else -> Alignment.CenterHorizontally
                    }
                ) {
                    if (Constants.DateTime.isTimeVisible(settings.dateTimeVisibility)) {
                        Text(
                            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(currentDate.value),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = MaterialTheme.typography.headlineLarge.fontSize * settings.textSizeScale,
                                fontWeight = fontWeight
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.clickable { openAlarmApp(context) }
                        )
                    }

                    if (Constants.DateTime.isDateVisible(settings.dateTimeVisibility)) {
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = MaterialTheme.typography.titleMedium.fontSize * settings.textSizeScale,
                                fontWeight = fontWeight
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.clickable { openCalendar(context) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Home apps section
            if (settings.homeAppsNum > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {

                    Spacer(modifier = Modifier.height(8.dp))

                    // Simple apps grid with clear separation
                    DraggableAppsGrid(
                        apps = uiState.homeApps,
                        columns = settings.homeScreenColumns,
                        showAppIcons = shouldShowIcons,
                        itemSpacing = itemSpacing,
                        onAppClick = { app ->
                            viewModel.launchApp(app)
                        },
                        onAppLongPress = { position ->

                            val existingApp = uiState.homeApps.getOrNull(position)

                            if (settings.editHomeApps || existingApp == null) {
                                val selectionType = when (position) {
                                0 -> AppSelectionType.HOME_APP_1
                                1 -> AppSelectionType.HOME_APP_2
                                2 -> AppSelectionType.HOME_APP_3
                                3 -> AppSelectionType.HOME_APP_4
                                4 -> AppSelectionType.HOME_APP_5
                                5 -> AppSelectionType.HOME_APP_6
                                6 -> AppSelectionType.HOME_APP_7
                                7 -> AppSelectionType.HOME_APP_8
                                8 -> AppSelectionType.HOME_APP_9
                                9 -> AppSelectionType.HOME_APP_10
                                10 -> AppSelectionType.HOME_APP_11
                                11 -> AppSelectionType.HOME_APP_12
                                12 -> AppSelectionType.HOME_APP_13
                                13 -> AppSelectionType.HOME_APP_14
                                14 -> AppSelectionType.HOME_APP_15
                                15 -> AppSelectionType.HOME_APP_16
                                else -> AppSelectionType.HOME_APP_1
                                }
                                viewModel.emitEvent(UiEvent.NavigateToAppSelection(selectionType))
                            }
                        },
                        onAppsReordered = { reorderedApps ->
                            viewModel.updateHomeAppOrder(reorderedApps.mapIndexed { index, pair ->
                                pair.second?.let { app ->
                                    HomeAppPreference(
                                        label = app.appLabel,
                                        packageName = app.appPackage,
                                        activityClassName = app.activityClassName,
                                        userString = app.user.toString(),
                                        position = index
                                    )
                                }
                            }.filterNotNull())
                        },
                        editMode = settings.editHomeApps
                    )
                }
            }
        }

        // Add widget FAB - only show in widget edit mode
        if (settings.editWidgets) {
            FloatingActionButton(
                onClick = { viewModel.emitEvent(UiEvent.NavigateToWidgetPicker) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Widget"
                )
            }
        }
    }
}