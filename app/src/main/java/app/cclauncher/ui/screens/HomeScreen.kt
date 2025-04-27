package app.cclauncher.ui.screens

import android.view.Gravity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cclauncher.MainViewModel
import app.cclauncher.data.AppModel
import app.cclauncher.data.Constants
import app.cclauncher.helper.expandNotificationDrawer
import app.cclauncher.helper.isPackageInstalled
import app.cclauncher.helper.openAlarmApp
import app.cclauncher.helper.openCalendar
import app.cclauncher.ui.util.detectSwipeGestures
import app.cclauncher.ui.AppSelectionType
import app.cclauncher.ui.UiEvent
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToAppDrawer: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.homeScreenState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()


    val currentDate = remember { mutableStateOf(Date()) }
    val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
    val dateText = dateFormat.format(currentDate.value).replace(".,", ",")


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
                    when (viewModel.settingsScreenState.value.swipeDownAction) {
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
                    onDoubleTap = {if (viewModel.settingsScreenState.value.doubleTapToLock) {
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
                        uiState.homeBottomAlignment -> when (uiState.homeAlignment) {
                            Gravity.START -> Alignment.BottomStart
                            Gravity.END -> Alignment.BottomEnd
                            else -> Alignment.BottomCenter
                        }
                        else -> when (uiState.homeAlignment) {
                            Gravity.START -> Alignment.CenterStart
                            Gravity.END -> Alignment.CenterEnd
                            else -> Alignment.Center
                        }
                    }
                )
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = when (uiState.homeAlignment) {
                Gravity.START -> Alignment.Start
                Gravity.END -> Alignment.End
                else -> Alignment.CenterHorizontally
            },
            ) {
            if (uiState.showDateTime) {
                DateTimeSection(
                    showTime = uiState.showTime,
                    showDate = uiState.showDate,
                    currentDate = currentDate.value,
                    dateText = dateText,
                    homeAlignment = uiState.homeAlignment,
                    onTimeClick = { openAlarmApp(context) },
                    onDateClick = { openCalendar(context) },
                    onDateLongPress = { viewModel.emitEvent(UiEvent.NavigateToAppSelection(AppSelectionType.CALENDAR_APP)) },
                    onTimeLongPress = { viewModel.emitEvent(UiEvent.NavigateToAppSelection(AppSelectionType.CLOCK_APP)) }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            HomeApps(
                homeAppsNum = uiState.homeAppsNum,
                homeApps = uiState.homeApps,
                alignment = uiState.homeAlignment,
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
                }

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
    onTimeClick: () -> Unit,
    onDateClick: () -> Unit,
    onTimeLongPress: () -> Unit,
    onDateLongPress: () -> Unit
) {
    Column(
        horizontalAlignment = when (homeAlignment) {
            Gravity.START -> Alignment.Start
            Gravity.END -> Alignment.End
            else -> Alignment.CenterHorizontally
        }
    ) {
        if (showTime) {
            Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(currentDate),
                style = MaterialTheme.typography.headlineLarge,
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
                style = MaterialTheme.typography.titleMedium,
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
    onAppClick: (AppModel) -> Unit,
    onAppLongPress: (Int) -> Unit
) {
    val context = LocalContext.current

    Column(
        horizontalAlignment = when (alignment) {
            Gravity.START -> Alignment.Start
            Gravity.END -> Alignment.End
            else -> Alignment.CenterHorizontally
        }
    ) {
        var nonNullAppIndex = 0
        val nonNullHomeApps = homeApps.filterNotNull()

        // Generate app items based on homeAppsNum
        for (i in 0 until homeAppsNum) {
            if (!homeApps[i]?.appPackage.isNullOrEmpty()) {
                val app = nonNullHomeApps[nonNullAppIndex]
                nonNullAppIndex++

                val isInstalled = remember(app.appPackage, app.user) {
                    isPackageInstalled(
                        context,
                        app.appPackage,
                        app.user.toString()
                    )
                }

                if (isInstalled) {
                    Text(
                        text = app.appLabel,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = when (alignment) {
                            Gravity.START -> TextAlign.Start
                            Gravity.END -> TextAlign.End
                            else -> TextAlign.Center
                        },
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .pointerInput(app) {
                                detectTapGestures(
                                    onTap = { onAppClick(app) },
                                    onLongPress = { onAppLongPress(i) }
                                )
                            }
                    )
                }
            } else {
                Text(
                    text = "•••",
                    style = MaterialTheme.typography.titleLarge,
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
//}