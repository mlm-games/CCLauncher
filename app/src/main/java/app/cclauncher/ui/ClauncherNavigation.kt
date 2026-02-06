package app.cclauncher.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import app.cclauncher.MainActivity
import app.cclauncher.MainViewModel
import app.cclauncher.data.Constants
import app.cclauncher.data.WidgetConstants
import app.cclauncher.helper.WidgetHelper
import app.cclauncher.helper.showToast
import app.cclauncher.ui.components.snackbar.LauncherSnackbarHost
import app.cclauncher.ui.components.snackbar.SnackbarManager
import app.cclauncher.ui.screens.AppDrawerScreen
import app.cclauncher.ui.screens.HiddenAppsScreen
import app.cclauncher.ui.screens.HomeScreen
import app.cclauncher.ui.screens.SettingsScreen
import app.cclauncher.ui.screens.WidgetPickerScreen
import app.cclauncher.ui.theme.AnimationConfig
import app.cclauncher.ui.util.SystemUIController
import app.cclauncher.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.koinInject
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun CLauncherNavigation(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    appWidgetHost: android.appwidget.AppWidgetHost,
    widgetHelper: WidgetHelper
) {
    val context = LocalContext.current
    val settings by settingsViewModel.settingsState.collectAsState()

    SystemUIController(showStatusBar = settings.statusBar)

    val snackbarManager: SnackbarManager = koinInject()
    val snackbarHostState = remember { SnackbarHostState() }

    val backStack = rememberNavBackStack(LauncherDestination.Home) // can't set initial screen here since settings is async

    var currentSelectionType by remember { mutableStateOf<AppSelectionType?>(null) }

    fun popToHome(clearSelection: Boolean = true) {
        while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        settingsViewModel.resetUnlockState()
        if (clearSelection) currentSelectionType = null
    }

    fun navigateTo(dest: LauncherDestination, clearSelection: Boolean = true) {
        popToHome(clearSelection)
        if (dest != LauncherDestination.Home) backStack.add(dest)
    }

    fun pushOnTop(dest: LauncherDestination) {
        if (backStack.lastOrNull() != dest) backStack.add(dest)
    }

    val handleEvent: (UiEvent) -> Unit = { event ->
        when (event) {
            UiEvent.NavigateToAppDrawer -> navigateTo(LauncherDestination.AppDrawer)
            UiEvent.NavigateToSettings -> navigateTo(LauncherDestination.Settings)

            UiEvent.NavigateToHiddenApps -> {
                if (backStack.lastOrNull() != LauncherDestination.Settings) {
                    navigateTo(LauncherDestination.Settings)
                }
                pushOnTop(LauncherDestination.HiddenApps)
            }

            UiEvent.NavigateToWidgetPicker -> {
                navigateTo(LauncherDestination.WidgetPicker)
            }

            UiEvent.NavigateBack -> {
                if (backStack.lastOrNull() != LauncherDestination.Home) {
                    popToHome()
                }
            }

            is UiEvent.NavigateToAppSelection -> {
                currentSelectionType = event.selectionType
                navigateTo(LauncherDestination.AppDrawer, false)
            }

            is UiEvent.ShowToast -> {
                //  prefer snackbar only for in-app feedback
                snackbarManager.show(event.message)
            }

            is UiEvent.LaunchWidgetBindIntent -> {
                try {
                    (context as? MainActivity)?.widgetRequestLauncher?.launch(event.intent)
                } catch (e: Exception) {
                    Log.e("Navigation", "Failed to launch widget bind intent", e)
                    snackbarManager.show("Failed to request widget permission: ${e.message}")
                }
            }

            is UiEvent.ConfigureWidget -> {
                val activity = context as? Activity
                if (activity != null) {
                    val requestCode = WidgetConstants.REQUEST_CONFIGURE_WIDGET
                    widgetHelper.startWidgetConfiguration(activity, event.widgetId, requestCode)
                }
            }

            is UiEvent.StartActivityForResult -> {
                try {
                    val activity = context as? Activity
                    if (activity != null) {
                        @Suppress("DEPRECATION")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            val options = ActivityOptions.makeBasic().apply {
                                pendingIntentBackgroundActivityStartMode =
                                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                            }
                            activity.startActivityForResult(event.intent, event.requestCode, options.toBundle())
                        } else {
                            activity.startActivityForResult(event.intent, event.requestCode)
                        }
                    }
                } catch (e: SecurityException) {
                    Log.e("Navigation", "Security exception starting activity", e)
                    try {
                        event.intent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        )
                        context.startActivity(event.intent)
                    } catch (e2: Exception) {
                        Log.e("Navigation", "Fallback failed too", e2)
                        context.showToast(
                            "Failed to configure widget. Please check app permissions.",
                            Toast.LENGTH_LONG
                        )
                    }
                } catch (e: Exception) {
                    Log.e("Navigation", "Failed to start activity for result", e)
                    snackbarManager.show("Failed to start widget configuration: ${e.localizedMessage}")
                }
            }

            else -> Unit
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest(handleEvent)
    }
    LaunchedEffect(settingsViewModel) {
        settingsViewModel.events.collectLatest(handleEvent)
    }

    val settingsTransitions =
        NavDisplay.transitionSpec { AnimationConfig.Navigation.slideLeftTransition() } +
                NavDisplay.popTransitionSpec { AnimationConfig.Navigation.slideRightTransition() } +
                NavDisplay.predictivePopTransitionSpec { _ -> AnimationConfig.Navigation.slideRightTransition() }

    val appDrawerTransitions =
        NavDisplay.transitionSpec { AnimationConfig.Navigation.slideUpTransition() } +
                NavDisplay.popTransitionSpec { AnimationConfig.Navigation.slideDownTransition() } +
                NavDisplay.predictivePopTransitionSpec { _ -> AnimationConfig.Navigation.slideDownTransition() }

    val hiddenAppsTransitions =
        NavDisplay.transitionSpec { AnimationConfig.Navigation.slideLeftTransition() } +
                NavDisplay.popTransitionSpec { AnimationConfig.Navigation.slideRightTransition() } +
                NavDisplay.predictivePopTransitionSpec { _ -> AnimationConfig.Navigation.slideRightTransition() }

    val widgetPickerTransitions =
        NavDisplay.transitionSpec { AnimationConfig.Navigation.widgetPickerTransition() } +
                NavDisplay.popTransitionSpec { AnimationConfig.Navigation.widgetPickerTransition() } +
                NavDisplay.predictivePopTransitionSpec { _ -> AnimationConfig.Navigation.widgetPickerTransition() }

    val provider: (NavKey) -> NavEntry<NavKey> =
        entryProvider {
            entry<LauncherDestination.Home> {
                HomeScreen(
                    viewModel = viewModel,
                    settingsViewModel = settingsViewModel,
                    appWidgetHost = appWidgetHost,
                    onNavigateToAppDrawer = { navigateTo(LauncherDestination.AppDrawer) },
                    onNavigateToSettings = { navigateTo(LauncherDestination.Settings) }
                )
            }

            entry<LauncherDestination.AppDrawer>(metadata = appDrawerTransitions) {
                AppDrawerScreen(
                    viewModel = viewModel,
                    settingsViewModel = settingsViewModel,
                    selectionMode = currentSelectionType != null,
                    selectionTitle = when (currentSelectionType) {
                        AppSelectionType.SWIPE_UP_APP -> "Select Swipe Up Action App"
                        AppSelectionType.SWIPE_DOWN_APP -> "Select Swipe Down Action App"
                        AppSelectionType.SWIPE_LEFT_APP -> "Select Swipe Left App"
                        AppSelectionType.SWIPE_RIGHT_APP -> "Select Swipe Right App"
                        null -> ""
                    },
                    onSwipeDown = { popToHome() },
                    onAppClick = { app ->
                        if (currentSelectionType != null) {
                            when (currentSelectionType) {
                                AppSelectionType.SWIPE_UP_APP -> viewModel.selectedApp(app, Constants.FLAG_SET_SWIPE_UP_APP)
                                AppSelectionType.SWIPE_DOWN_APP -> viewModel.selectedApp(app, Constants.FLAG_SET_SWIPE_DOWN_APP)
                                AppSelectionType.SWIPE_LEFT_APP -> viewModel.selectedApp(app, Constants.FLAG_SET_SWIPE_LEFT_APP)
                                AppSelectionType.SWIPE_RIGHT_APP -> viewModel.selectedApp(app, Constants.FLAG_SET_SWIPE_RIGHT_APP)
                                else -> {}
                            }
                            currentSelectionType = null
                            navigateTo(LauncherDestination.Settings)
                        } else {
                            viewModel.launchApp(app)
                        }
                    }
                )
            }

            entry<LauncherDestination.Settings>(metadata = settingsTransitions) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    mainViewModel = viewModel,
                    onNavigateBack = { popToHome() },
                    onNavigateToHiddenApps = {
                        if (backStack.lastOrNull() != LauncherDestination.Settings) {
                            navigateTo(LauncherDestination.Settings)
                        }
                        pushOnTop(LauncherDestination.HiddenApps)
                    }
                )
            }

            entry<LauncherDestination.HiddenApps>(metadata = hiddenAppsTransitions) {
                HiddenAppsScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        if (backStack.lastOrNull() == LauncherDestination.HiddenApps) {
                            backStack.removeAt(backStack.lastIndex)
                        } else {
                            navigateTo(LauncherDestination.Settings)
                        }
                    }
                )
            }

            entry<LauncherDestination.WidgetPicker>(metadata = widgetPickerTransitions) {
                WidgetPickerScreen(
                    onWidgetSelected = { providerInfo ->
                        viewModel.startWidgetConfiguration(providerInfo)
                        popToHome()
                    },
                    onDismiss = { popToHome() }
                )
        }
    }

    Scaffold(
        snackbarHost = {
            LauncherSnackbarHost(
                hostState = snackbarHostState,
                manager = snackbarManager
            )
        },
        containerColor = Color.Transparent
    ) {
        Box(Modifier.fillMaxSize()) {
            NavDisplay(
                backStack = backStack,
                onBack = {
                    popToHome()
                },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = provider
            )
        }
    }
}

@Composable
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    val currentOnBack by rememberUpdatedState(onBack)
    val backCallback = remember {
        object : OnBackPressedCallback(enabled) {
            override fun handleOnBackPressed() {
                currentOnBack()
            }
        }
    }
    SideEffect {
        backCallback.isEnabled = enabled
    }
    val backDispatcher = checkNotNull(LocalOnBackPressedDispatcherOwner.current) {
        "No OnBackPressedDispatcherOwner was provided via LocalOnBackPressedDispatcherOwner"
    }.onBackPressedDispatcher
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, backDispatcher) {
        backDispatcher.addCallback(lifecycleOwner, backCallback)
        onDispose { backCallback.remove() }
    }
}

@Serializable
sealed interface LauncherDestination : NavKey {
    @Serializable
    data object Home : LauncherDestination

    @Serializable
    data object AppDrawer : LauncherDestination

    @Serializable
    data object Settings : LauncherDestination

    @Serializable
    data object HiddenApps : LauncherDestination

    @Serializable
    data object WidgetPicker : LauncherDestination
}