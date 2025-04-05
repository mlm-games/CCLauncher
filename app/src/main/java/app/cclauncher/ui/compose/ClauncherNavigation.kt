package app.cclauncher.ui.compose

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.cclauncher.MainViewModel
import app.cclauncher.data.Constants
import app.cclauncher.data.Navigation
import app.cclauncher.ui.compose.screens.AppDrawerScreen
import app.cclauncher.ui.compose.screens.HiddenAppsScreen
import app.cclauncher.ui.compose.screens.HomeScreen
import app.cclauncher.ui.compose.screens.SettingsScreen
import app.cclauncher.ui.compose.util.SystemUIController
import app.cclauncher.ui.events.AppSelectionType
import app.cclauncher.ui.events.UiEvent
import kotlinx.coroutines.flow.collectLatest

@Composable
fun CLauncherNavigation(
    viewModel: MainViewModel,
    currentScreen: String,
    onScreenChange: (String) -> Unit
) {
    val context = LocalContext.current
    val preferences by viewModel.prefsDataStore.preferences.collectAsState(initial = null)

    preferences?.let {
        SystemUIController(showStatusBar = it.statusBar)
    }

    var showAppSelectionDialog by remember { mutableStateOf(false) }
    var currentSelectionType by remember { mutableStateOf<AppSelectionType?>(null) }

    // for UI events
    LaunchedEffect(key1 = viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is UiEvent.NavigateToAppDrawer -> {
                    onScreenChange(Navigation.APP_DRAWER)
                }
                is UiEvent.NavigateToSettings -> {
                    onScreenChange(Navigation.SETTINGS)
                }
                is UiEvent.NavigateToHiddenApps -> {
                    onScreenChange(Navigation.HIDDEN_APPS)
                }
                is UiEvent.NavigateBack -> {
                    onScreenChange(Navigation.HOME)
                }
                is UiEvent.ShowToast -> {
                    // Show toast message
                    android.widget.Toast.makeText(context, event.message, android.widget.Toast.LENGTH_SHORT).show()
                }
                is UiEvent.NavigateToAppSelection -> {
                    // Store selection type and show dialog
                    currentSelectionType = event.selectionType
                    showAppSelectionDialog = true
                    // Navigate to app drawer with selection mode
                    onScreenChange(Navigation.APP_DRAWER)
                }
                else -> {
                    // Handle other events, presently nothing.
                }
            }
        }
    }

    when (currentScreen) {
        Navigation.HOME -> {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToAppDrawer = {
                    onScreenChange(Navigation.APP_DRAWER)
                },
                onNavigateToSettings = {
                    onScreenChange(Navigation.SETTINGS)
                }
            )
        }
        Navigation.APP_DRAWER -> {
            AppDrawerScreen(
                viewModel = viewModel,
                onAppClick = { app ->
                    // Check if we're in app selection mode
                    if (currentSelectionType != null) {
                        when (currentSelectionType) {
                            AppSelectionType.CLOCK_APP -> viewModel.selectedApp(app, Constants.FLAG_SET_CLOCK_APP)
                            AppSelectionType.CALENDAR_APP -> viewModel.selectedApp(app, Constants.FLAG_SET_CALENDAR_APP)
                            AppSelectionType.HOME_APP_1 -> viewModel.selectedApp(app, Constants.FLAG_SET_HOME_APP_1)
                            AppSelectionType.HOME_APP_2 -> viewModel.selectedApp(app, Constants.FLAG_SET_HOME_APP_2)
                            AppSelectionType.HOME_APP_3 -> viewModel.selectedApp(app, Constants.FLAG_SET_HOME_APP_3)
                            AppSelectionType.HOME_APP_4 -> viewModel.selectedApp(app, Constants.FLAG_SET_HOME_APP_4)
                            AppSelectionType.HOME_APP_5 -> viewModel.selectedApp(app, Constants.FLAG_SET_HOME_APP_5)
                            AppSelectionType.HOME_APP_6 -> viewModel.selectedApp(app, Constants.FLAG_SET_HOME_APP_6)
                            AppSelectionType.HOME_APP_7 -> viewModel.selectedApp(app, Constants.FLAG_SET_HOME_APP_7)
                            AppSelectionType.HOME_APP_8 -> viewModel.selectedApp(app, Constants.FLAG_SET_HOME_APP_8)
                            AppSelectionType.SWIPE_LEFT_APP -> viewModel.selectedApp(app, Constants.FLAG_SET_SWIPE_LEFT_APP)
                            AppSelectionType.SWIPE_RIGHT_APP -> viewModel.selectedApp(app, Constants.FLAG_SET_SWIPE_RIGHT_APP)
                            else -> {}
                        }
                        currentSelectionType = null
                        onScreenChange(Navigation.HOME)
                    } else {
                        viewModel.launchApp(app)
                        onScreenChange(Navigation.HOME)
                    }
                },
                onSwipeDown = { onScreenChange(Navigation.HOME) },
                selectionMode = currentSelectionType != null,
                selectionTitle = when (currentSelectionType) {
                    AppSelectionType.CLOCK_APP -> "Select Clock App"
                    AppSelectionType.CALENDAR_APP -> "Select Calendar App"
                    AppSelectionType.HOME_APP_1,
                    AppSelectionType.HOME_APP_2,
                    AppSelectionType.HOME_APP_3,
                    AppSelectionType.HOME_APP_4,
                    AppSelectionType.HOME_APP_5,
                    AppSelectionType.HOME_APP_6,
                    AppSelectionType.HOME_APP_7,
                    AppSelectionType.HOME_APP_8 -> "Select Home App"
                    AppSelectionType.SWIPE_LEFT_APP -> "Select Swipe Left App"
                    AppSelectionType.SWIPE_RIGHT_APP -> "Select Swipe Right App"
                    null -> ""
                }
            )
        }
        Navigation.SETTINGS -> {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    onScreenChange(Navigation.HOME)
                },
                onNavigateToHiddenApps = {
                    onScreenChange(Navigation.HIDDEN_APPS)
                }
            )
        }
        Navigation.HIDDEN_APPS -> {
            HiddenAppsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    onScreenChange(Navigation.SETTINGS)
                }
            )
        }
    }
}

@Composable
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    // Safely update the current `onBack` lambda when a new one is provided
    val currentOnBack by rememberUpdatedState(onBack)
    // Remember in Composition a back callback that calls the `onBack` lambda
    val backCallback = remember {
        object : OnBackPressedCallback(enabled) {
            override fun handleOnBackPressed() {
                currentOnBack()
            }
        }
    }
    // On every successful composition, update the callback with the `enabled` value
    SideEffect {
        backCallback.isEnabled = enabled
    }
    val backDispatcher = checkNotNull(LocalOnBackPressedDispatcherOwner.current) {
        "No OnBackPressedDispatcherOwner was provided via LocalOnBackPressedDispatcherOwner"
    }.onBackPressedDispatcher
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, backDispatcher) {
        // Add callback to the backDispatcher
        backDispatcher.addCallback(lifecycleOwner, backCallback)
        // When the effect leaves the Composition, remove the callback
        onDispose {
            backCallback.remove()
        }
    }
}
