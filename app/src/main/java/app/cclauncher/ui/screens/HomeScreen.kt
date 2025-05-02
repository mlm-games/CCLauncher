package app.cclauncher.ui.screens

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.Gravity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cclauncher.MainViewModel
import app.cclauncher.data.AppModel
import app.cclauncher.data.Constants
import app.cclauncher.data.repository.SettingsRepository
import app.cclauncher.helper.IconCache
import app.cclauncher.helper.WidgetHelper
import app.cclauncher.helper.expandNotificationDrawer
import app.cclauncher.helper.isPackageInstalled
import app.cclauncher.helper.isTablet
import app.cclauncher.helper.openAlarmApp
import app.cclauncher.helper.openCalendar
import app.cclauncher.ui.util.detectSwipeGestures
import app.cclauncher.ui.AppSelectionType
import app.cclauncher.ui.UiEvent
import app.cclauncher.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.coroutineScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import app.cclauncher.data.HomeAppPreference
import app.cclauncher.ui.components.DraggableAppsGrid
import app.cclauncher.ui.components.DraggableWidgetContainer
import app.cclauncher.ui.components.ExternalWidget
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel = viewModel(),
    onNavigateToAppDrawer: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.homeScreenState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val settings by settingsViewModel.settingsState.collectAsState()

    val scope = rememberCoroutineScope()

    var widgetEditMode by remember { mutableStateOf(false) }

    val currentDate = remember { mutableStateOf(Date()) }
    val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
    val dateText = dateFormat.format(currentDate.value).replace(".,", ",")

    // Get current orientation to decide whether to show icons
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE


    // Determine if icons should be shown based on orientation settings
    val shouldShowIcons = if (settings.showHomeScreenIcons) {
        if (isLandscape) settings.showIconsInLandscape else settings.showIconsInPortrait
    } else {
        false
    }

    // Get font weight
    val fontWeight = when (settings.fontWeight) {
        0 -> FontWeight.Thin
        1 -> FontWeight.Light
        2 -> FontWeight.Normal
        3 -> FontWeight.Medium
        4 -> FontWeight.Bold
        5 -> FontWeight.Black
        else -> FontWeight.Normal
    }

    // Get item spacing
    val itemSpacing = when (settings.itemSpacing) {
        0 -> 0.dp
        1 -> 4.dp
        2 -> 8.dp
        3 -> 16.dp
        else -> 8.dp
    }

    LaunchedEffect(settings.forceLandscapeMode) {
        (context as? android.app.Activity)?.let { activity ->
            if (settings.forceLandscapeMode) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    LaunchedEffect(key1 = Unit) {
        while(true) {
            currentDate.value = Date()
            kotlinx.coroutines.delay(60000)
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            viewModel.emitEvent(UiEvent.ShowToast(it))
            viewModel.clearError()
        }
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (widgetEditMode) {
        FloatingActionButton(
            onClick = {
                viewModel.emitEvent(UiEvent.NavigateToWidgetPicker)
            },
            modifier = Modifier
//                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Widget"
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .detectSwipeGestures(
                onSwipeUp = { onNavigateToAppDrawer() },
                onSwipeDown = {
                    when (settings.swipeDownAction) {
                        Constants.SwipeDownAction.NOTIFICATIONS -> expandNotificationDrawer(context)
                        Constants.SwipeDownAction.SEARCH -> onNavigateToAppDrawer()
                        else -> expandNotificationDrawer(context)
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
                    onLongPress = { onNavigateToSettings() },
                    onTap = { /* Check for messages */ }
                )
            }
    ) {
        val preferences by viewModel.prefsDataStore.preferences.collectAsState(initial = null)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = when (uiState.homeAlignment) {
                Gravity.START -> Alignment.Start
                Gravity.END -> Alignment.End
                else -> Alignment.CenterHorizontally
            }
        ) {
            // widgets at the top if there are any
            preferences?.externalWidgets?.let { widgets ->
                if (widgets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Widget section header with edit toggle
                    if (widgetEditMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Edit Widgets",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Button(
                                onClick = { widgetEditMode = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Text("Done")
                            }
                        }
                    } else {
                        // Show edit button when not in edit mode
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 16.dp)
                        ) {
                            Text(
                                text = "Edit",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .clickable { widgetEditMode = true }
                                    .padding(8.dp)
                            )
                        }
                    }

                    // Widget container with enhanced drag-and-drop
                    DraggableWidgetContainer(
                        widgets = widgets,
                        editMode = widgetEditMode,
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
                                        // Find the widget to get its appWidgetId
                                        val widget = widgets.find { it.id == widgetId }
                                        widget?.let {
                                            // Delete the actual widget
                                            val widgetHelper = WidgetHelper(context)
                                            widgetHelper.deleteWidget(it.appWidgetId)
                                            // Remove from preferences
                                            viewModel.removeExternalWidget(widgetId)
                                        }
                                    }
                                },
                                onResizeWidget = { widget, newWidth, newHeight ->
                                    scope.launch {
                                        viewModel.resizeWidget(widget, newWidth, newHeight)
                                    }
                                }
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }


            // Flexible space that pushes content down or centers it
            if (uiState.homeBottomAlignment) {
                Spacer(modifier = Modifier.weight(1f))
            } else {
                Spacer(modifier = Modifier.weight(0.5f))
            }
        }

        // date/time and homeApps column
        Column(
            modifier = Modifier
                .align(
                    when {
                        // First determine vertical alignment based on homeBottomAlignment
                        settings.homeBottomAlignment -> when (settings.homeAlignment) {
                            Gravity.START -> Alignment.BottomStart
                            Gravity.END -> Alignment.BottomEnd
                            else -> Alignment.BottomCenter
                        }
                        else -> when (settings.homeAlignment) {
                            Gravity.START -> Alignment.CenterStart
                            Gravity.END -> Alignment.CenterEnd
                            else -> Alignment.Center
                        }
                    }
                )
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = when (settings.homeAlignment) {
                Gravity.START -> Alignment.Start
                Gravity.END -> Alignment.End
                else -> Alignment.CenterHorizontally
            },
        ) {
            if (settings.dateTimeVisibility != Constants.DateTime.OFF) {
                DateTimeSection(
                    showTime = Constants.DateTime.isTimeVisible(settings.dateTimeVisibility),
                    showDate = Constants.DateTime.isDateVisible(settings.dateTimeVisibility),
                    currentDate = currentDate.value,
                    dateText = dateText,
                    homeAlignment = settings.homeAlignment,
                    fontScale = settings.textSizeScale,
                    fontWeight = fontWeight,
                    onTimeClick = { openAlarmApp(context) },
                    onDateClick = { openCalendar(context) },
                    onDateLongPress = {
                        viewModel.emitEvent(
                            UiEvent.NavigateToAppSelection(
                                AppSelectionType.CALENDAR_APP
                            )
                        )
                    },
                    onTimeLongPress = {
                        viewModel.emitEvent(
                            UiEvent.NavigateToAppSelection(
                                AppSelectionType.CLOCK_APP
                            )
                        )
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            var homeEditMode by remember { mutableStateOf(false) }

            if (settings.homeAppsNum > 0) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (homeEditMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Edit Home Apps",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Button(onClick = { homeEditMode = false }) {
                                Text("Done")
                            }
                        }
                    } else {
                        // Show edit button when not in edit mode
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 16.dp)
                        ) {
                            Text(
                                text = "Edit",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .clickable { homeEditMode = true }
                                    .padding(8.dp)
                            )
                        }
                    } //TODO: Edit mode should preferably be accessed from settings instead of having a button all the time

                    // Replace HomeApps with DraggableAppsGrid
                    DraggableAppsGrid(
                        apps = uiState.homeApps,
                        columns = settings.homeScreenColumns,
                        showAppIcons = shouldShowIcons,
                        itemSpacing = itemSpacing,
                        onAppClick = { app -> viewModel.launchApp(app) },
                        onAppLongPress = { position ->
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
                        },
                        onAppsReordered = { reorderedApps ->
                            // Update app order in ViewModel
                            viewModel.updateHomeAppOrder(reorderedApps.mapIndexed { index, pair ->
                                pair.second?.let { app ->
                                    HomeAppPreference(
                                        label = app.appLabel,
                                        packageName = app.appPackage,
                                        activityClassName = app.activityClassName,
                                        userString = app.user.toString(),
//                                        position = index
                                    )
                                }
                            }.filterNotNull())
                        },
                        editMode = homeEditMode
                    )
                }
            }
        }
    }
}

@Composable
fun DateTimeSection(
    showTime: Boolean,
    showDate: Boolean,
    currentDate: Date,
    dateText: String,
    homeAlignment: Int,
    fontScale: Float,
    fontWeight: FontWeight,
    onTimeClick: () -> Unit,
    onDateClick: () -> Unit,
    onTimeLongPress: () -> Unit,
    onDateLongPress: () -> Unit
) {
    Column(
//        horizontalAlignment = when (homeAlignment) {
//            Gravity.START -> Alignment.Start
//            Gravity.END -> Alignment.End
//            else -> Alignment.CenterHorizontally
//        }
    ) {
        if (showTime) {
            Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(currentDate),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = MaterialTheme.typography.headlineLarge.fontSize * fontScale,
                    fontWeight = fontWeight
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onTimeClick() },
                        onLongPress = { onTimeLongPress() }
                    )
                }
            )
        }

        if (showDate) {
            Text(
                text = dateText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = MaterialTheme.typography.titleMedium.fontSize * fontScale,
                    fontWeight = fontWeight
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onDateClick() },
                        onLongPress = { onDateLongPress() }
                    )
                }
            )
        }
    }
}

