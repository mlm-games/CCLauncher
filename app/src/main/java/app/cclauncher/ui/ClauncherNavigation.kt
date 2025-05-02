package app.cclauncher.ui

import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.cclauncher.MainViewModel
import app.cclauncher.data.Constants
import app.cclauncher.data.Navigation
import app.cclauncher.ui.screens.AppDrawerScreen
import app.cclauncher.ui.screens.HiddenAppsScreen
import app.cclauncher.ui.screens.HomeScreen
import app.cclauncher.ui.screens.SettingsScreen
import app.cclauncher.ui.util.SystemUIController
import app.cclauncher.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CLauncherNavigation(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,  // Use the new SettingsViewModel
    currentScreen: String,
    onScreenChange: (String) -> Unit
) {
    val context = LocalContext.current
    val settings by settingsViewModel.settingsState.collectAsState()  // Get settings from SettingsViewModel

    // Apply system UI settings
    SystemUIController(showStatusBar = settings.statusBar)

    // Force landscape if setting is enabled
    LaunchedEffect(settings.forceLandscapeMode) {
        (context as? android.app.Activity)?.let { activity ->
            if (settings.forceLandscapeMode) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
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
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
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

    // Main animation container
    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            // Define different animations based on navigation direction
            when (targetState) {
                Navigation.HOME -> {
                    when (initialState) {
                        Navigation.APP_DRAWER -> {
                            // App drawer to home: slide down
                            slideInVertically(
                                initialOffsetY = { -it },
                                animationSpec = tween(300)
                            ).togetherWith(
                                slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = tween(300)
                                )
                            )
                        }
                        else -> {
                            // Settings/Hidden apps to home: slide right
                            slideInHorizontally(
                                initialOffsetX = { -it },
                                animationSpec = tween(300)
                            ).togetherWith(
                                slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(300)
                                )
                            )
                        }
                    }
                }
                Navigation.APP_DRAWER -> {
                    // Home to app drawer: slide up
                    slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(300)
                    ).togetherWith(
                        slideOutVertically(
                            targetOffsetY = { -it },
                            animationSpec = tween(300)
                        )
                    )
                }
                Navigation.SETTINGS -> {
                    // Home to settings: slide left
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { -it },
                            animationSpec = tween(300)
                        )
                    )
                }
                Navigation.HIDDEN_APPS -> {
                    // Settings to hidden apps: slide left
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { -it },
                            animationSpec = tween(300)
                        )
                    )
                }
                else -> {
                    // Default animation
                    fadeIn(animationSpec = tween(300)).togetherWith(fadeOut(animationSpec = tween(300)))
                }
            }
        }
    ) { screen ->
        // Render the appropriate screen based on current navigation state
        Box(modifier = Modifier.fillMaxSize()) {
            when (screen) {
                Navigation.HOME -> {
                    HomeScreen(
                        viewModel = viewModel,
                        settingsViewModel = settingsViewModel,  // Pass the settings view model
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
                        settingsViewModel = settingsViewModel,  // Pass the settings view model
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
                                    AppSelectionType.HOME_APP_9 -> viewModel.selectedApp(app, Constants.FLAG_SET_HOME_APP_9)
                                    AppSelectionType.HOME_APP_10 -> viewModel.selectedApp(app, Constants.FLAG_SET_HOME_APP_10)
                                    AppSelectionType.HOME_APP_11 -> viewModel.selectedApp(app, Constants.FLAG_SET_HOME_APP_11)
                                    AppSelectionType.HOME_APP_12 -> viewModel.selectedApp(app, Constants.FLAG_SET_HOME_APP_12)
                                    AppSelectionType.HOME_APP_13 -> viewModel.selectedApp(app, Constants.FLAG_SET_HOME_APP_13)
                                    AppSelectionType.HOME_APP_14 -> viewModel.selectedApp(app, Constants.FLAG_SET_HOME_APP_14)
                                    AppSelectionType.HOME_APP_15 -> viewModel.selectedApp(app, Constants.FLAG_SET_HOME_APP_15)
                                    AppSelectionType.HOME_APP_16 -> viewModel.selectedApp(app, Constants.FLAG_SET_HOME_APP_16)
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
                            AppSelectionType.HOME_APP_8,
                            AppSelectionType.HOME_APP_9,
                            AppSelectionType.HOME_APP_10,
                            AppSelectionType.HOME_APP_11,
                            AppSelectionType.HOME_APP_12,
                            AppSelectionType.HOME_APP_13,
                            AppSelectionType.HOME_APP_14,
                            AppSelectionType.HOME_APP_15,
                            AppSelectionType.HOME_APP_16 -> "Select Home App"
                            AppSelectionType.SWIPE_LEFT_APP -> "Select Swipe Left App"
                            AppSelectionType.SWIPE_RIGHT_APP -> "Select Swipe Right App"
                            null -> ""
                        }
                    )
                }
                Navigation.SETTINGS -> {
                    SettingsScreen(
                        viewModel = settingsViewModel,
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