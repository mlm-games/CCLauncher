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
                    onDateLongPress = { viewModel.emitEvent(UiEvent.NavigateToAppSelection(AppSelectionType.CALENDAR_APP)) },
                    onTimeLongPress = { viewModel.emitEvent(UiEvent.NavigateToAppSelection(AppSelectionType.CLOCK_APP)) }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            HomeApps(
                homeAppsNum = settings.homeAppsNum,
                homeApps = uiState.homeApps,
                alignment = settings.homeAlignment,
                showAppIcons = shouldShowIcons,
                fontScale = settings.textSizeScale,
                fontWeight = fontWeight,
                iconCornerRadius = settings.iconCornerRadius.dp,
                itemSpacing = itemSpacing,
                onAppClick = { app -> viewModel.launchApp(app) },
                onAppLongPress = { position ->
                    // Convert position to selection type
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
                columns = settings.homeScreenColumns
            )
        }
    }
}

@Composable
private fun DateTimeSection(
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

@Composable
private fun HomeApps(
    homeAppsNum: Int,
    homeApps: List<AppModel?>,
    alignment: Int,
    showAppIcons: Boolean,
    fontScale: Float,
    fontWeight: FontWeight,
    iconCornerRadius: androidx.compose.ui.unit.Dp,
    itemSpacing: androidx.compose.ui.unit.Dp,
    onAppClick: (AppModel) -> Unit,
    onAppLongPress: (Int) -> Unit,
    columns: Int
) {
    val context = LocalContext.current
    val iconCache = remember { IconCache(context) }
    val coroutineScope = rememberCoroutineScope()

    val loadedIcons = remember { mutableStateMapOf<String, ImageBitmap?>() }

    LaunchedEffect(homeApps) {
        if (showAppIcons) {
            homeApps.filterNotNull().forEach { app ->
                if (app.appIcon == null) {
                    val key = app.getKey()

                    if (!loadedIcons.containsKey(key)) {
                        coroutineScope.launch {
                            val userHandle = app.user
                            val icon = iconCache.getIcon(
                                app.appPackage,
                                app.activityClassName,
                                userHandle
                            )
                            loadedIcons[key] = icon
                        }
                    }
                }
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
//            .wrapContentWidth(
//                when (alignment) {
//                    Gravity.START -> Alignment.Start
//                    Gravity.END -> Alignment.End
//                    else -> Alignment.CenterHorizontally
//                }
//            )
    ) {
        items(homeAppsNum) { i ->
            val app = homeApps.getOrNull(i)
            if (app != null) {
                val isInstalled = remember(app.appPackage, app.user) {
                    isPackageInstalled(
                        context,
                        app.appPackage,
                        app.user.toString()
                    )
                }

                if (isInstalled) {
                        Row(
//                            horizontalArrangement = when (alignment) {
//                                Gravity.START -> Arrangement.Start
//                                Gravity.END -> Arrangement.End
//                                else -> Arrangement.Center
//                            },
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth() // Make the row fill the grid cell width
                                .padding(vertical = 8.dp)
                                .pointerInput(app) {
                                    detectTapGestures(
                                        onTap = { onAppClick(app) },
                                        onLongPress = { onAppLongPress(i) }
                                    )
                                }
                        ) {
                            if (showAppIcons) {

                                val appIcon = app.appIcon ?: loadedIcons[app.getKey()]

                                if (appIcon != null) {

                                    Surface(
                                        shape = RoundedCornerShape(iconCornerRadius),
                                        modifier = Modifier.padding(end = 8.dp)
//                                        modifier = Modifier.align(
//                                            when (alignment) {
//                                                Gravity.START -> Alignment.Start
//                                                Gravity.END -> Alignment.End
//                                                else -> Alignment.CenterHorizontally
//                                            }
//                                        )
                                    ) {
                                        Image(
                                            bitmap = appIcon,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }

                            Text(
                                text = app.appLabel,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = MaterialTheme.typography.titleLarge.fontSize * fontScale,
                                    fontWeight = fontWeight
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
//                                textAlign = when (alignment) {
//                                    Gravity.START -> TextAlign.Start
//                                    Gravity.END -> TextAlign.End
//                                    else -> TextAlign.Center
//                                }
                            )
                    }
                }
            } else {
                Text(
                    text = "•••",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = MaterialTheme.typography.titleLarge.fontSize * fontScale,
                        fontWeight = fontWeight
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = when (alignment) {
                        Gravity.START -> TextAlign.Start
                        Gravity.END -> TextAlign.End
                        else -> TextAlign.Center
                    },
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .clickable { onAppLongPress(i) }
                )
            }
        }
    }
}