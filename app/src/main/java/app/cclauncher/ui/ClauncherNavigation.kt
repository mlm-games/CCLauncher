package app.cclauncher.ui

import android.app.Activity
import android.app.ActivityOptions
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.cclauncher.MainViewModel
import app.cclauncher.data.Constants
import app.cclauncher.data.Navigation
import app.cclauncher.ui.screens.*
import app.cclauncher.ui.util.SystemUIController
import app.cclauncher.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.flow.collectLatest
import app.cclauncher.ui.screens.WidgetPickerScreen
import android.appwidget.AppWidgetHost
import android.content.Intent
import android.os.Build
import android.util.Log
import app.cclauncher.MainActivity
import app.cclauncher.helper.WidgetHelper

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CLauncherNavigation(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    currentScreen: String,
    onScreenChange: (String) -> Unit,
    appWidgetHost: AppWidgetHost,
    widgetHelper: WidgetHelper
) {
    val context = LocalContext.current
    val settings by settingsViewModel.settingsState.collectAsState()


    // Apply system UI settings
    SystemUIController(showStatusBar = settings.statusBar)

    var currentSelectionType by remember { mutableStateOf<AppSelectionType?>(null) }

    val handleEvent: (UiEvent) -> Unit = { event ->
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
                settingsViewModel.resetUnlockState()
            }
            is UiEvent.NavigateToWidgetPicker -> {
                onScreenChange(Navigation.WIDGET_PICKER)
            }
            is UiEvent.LaunchWidgetBindIntent -> {
                try {
                    (context as? MainActivity)?.widgetRequestLauncher?.launch(event.intent)
                } catch (e: Exception) {
                    Log.e("Navigation", "Failed to launch widget bind intent", e)
                    Toast.makeText(
                        context,
                        "Failed to request widget permission: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            is UiEvent.ConfigureWidget -> {
                val activity = context as? Activity
                if (activity != null) {
                    val REQUEST_CODE_CONFIGURE_WIDGET = 1001
                    widgetHelper.startWidgetConfiguration(activity, event.widgetId, REQUEST_CODE_CONFIGURE_WIDGET)
                }
            }


            is UiEvent.StartActivityForResult -> {
                try {
                    val activity = context as? Activity
                    if (activity != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            // For Android 14+ with background activity restrictions
                            val options = ActivityOptions.makeBasic().apply {
                                pendingIntentBackgroundActivityStartMode = ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                            }
                            activity.startActivityForResult(event.intent, event.requestCode, options.toBundle())
                        } else {
                            // Standard approach for older versions
                            activity.startActivityForResult(event.intent, event.requestCode)
                        }
                    }
                } catch (e: SecurityException) {
                    // Handle cases where the activity isn't exported
                    Log.e("Navigation", "Security exception starting activity", e)
                    try {
                        // Try again with different flags
                        event.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        context.startActivity(event.intent)
                    } catch (e2: Exception) {
                        Log.e("Navigation", "Fallback failed too", e2)
                        Toast.makeText(
                            context,
                            "Failed to configure widget. Please check app permissions.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.e("Navigation", "Failed to start activity for result", e)
                    Toast.makeText(
                        context,
                        "Failed to start widget configuration: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            is UiEvent.ShowToast -> {
                // Show toast message
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
            is UiEvent.NavigateToAppSelection -> {
                // Store selection type and show dialog
                currentSelectionType = event.selectionType
                // Navigate to app drawer with selection mode
                onScreenChange(Navigation.APP_DRAWER)
            }
            else -> {
                // Handle other events, presently nothing.
            }
        }
    }

    // Collect events from MainViewModel
    LaunchedEffect(key1 = viewModel) {
        viewModel.events.collectLatest { event ->
            handleEvent(event)
        }
    }

    // from SettingsViewModel
    LaunchedEffect(key1 = settingsViewModel) {
        settingsViewModel.events.collectLatest { event ->
            handleEvent(event)
        }
    }


    // Main animation container with content alignment for proper scaling
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
                // Widget screens animations
                Navigation.WIDGET_PICKER -> {
                    // Settings to widget screens: slide left with fade and scale
                    (slideInHorizontally(
                        initialOffsetX = { it/5 },  // Reduced slide distance for subtlety
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300)) +
                            scaleIn(initialScale = 0.95f, animationSpec = tween(300))).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { -it/5 },  // Reduced slide distance for subtlety
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300)) +
                                scaleOut(targetScale = 0.95f, animationSpec = tween(300))
                    )
                }
                else -> {
                    // Default animation with fade and scale
                    (fadeIn(animationSpec = tween(300)) +
                            scaleIn(initialScale = 0.95f, animationSpec = tween(300))).togetherWith(
                        fadeOut(animationSpec = tween(300)) +
                                scaleOut(targetScale = 0.95f, animationSpec = tween(300))
                    )
                }
            }
        },
        contentAlignment = Alignment.Center  // Important for proper scaling
    ) { screen ->
        // Render the appropriate screen based on current navigation state
        Box(modifier = Modifier.fillMaxSize()) {
            when (screen) {
                Navigation.HOME -> {
                    HomeScreen(
                        viewModel = viewModel,
                        settingsViewModel = settingsViewModel,
                        appWidgetHost = appWidgetHost,
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
                        settingsViewModel = settingsViewModel,
                        onAppClick = { app ->
                            // Check if we're in app selection mode
                            if (currentSelectionType != null) {
                                when (currentSelectionType) {
                                    AppSelectionType.SWIPE_UP_APP -> viewModel.selectedApp(app, Constants.FLAG_SET_SWIPE_UP_APP)
                                    AppSelectionType.SWIPE_DOWN_APP -> viewModel.selectedApp(app, Constants.FLAG_SET_SWIPE_DOWN_APP)
                                    AppSelectionType.SWIPE_LEFT_APP -> viewModel.selectedApp(app, Constants.FLAG_SET_SWIPE_LEFT_APP)
                                    AppSelectionType.SWIPE_RIGHT_APP -> viewModel.selectedApp(app, Constants.FLAG_SET_SWIPE_RIGHT_APP)
                                    else -> {}
                                }
                                // After selection, reset and go back to settings
                                currentSelectionType = null
                                onScreenChange(Navigation.SETTINGS)
                            } else {
                                // Normal app launch
                                viewModel.launchApp(app)
                            }
                        },
                        onSwipeDown = { onScreenChange(Navigation.HOME) },
                        selectionMode = currentSelectionType != null,
                        selectionTitle = when (currentSelectionType) {
                            AppSelectionType.SWIPE_UP_APP -> "Select Swipe Up Action App"
                            AppSelectionType.SWIPE_DOWN_APP -> "Select Swipe Down Action App"
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
                Navigation.WIDGET_PICKER -> {
                    WidgetPickerScreen(
                        onWidgetSelected = { providerInfo ->
                            viewModel.startWidgetConfiguration(providerInfo)
                            onScreenChange(Navigation.HOME)
                        },
                        onDismiss = { onScreenChange(Navigation.HOME) }
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