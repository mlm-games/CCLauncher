package app.cclauncher

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.cclauncher.data.*
import app.cclauncher.data.repository.AppRepository
import app.cclauncher.helper.MyAccessibilityService
import app.cclauncher.helper.PermissionManager
import app.cclauncher.helper.getUserHandleFromString
import app.cclauncher.ui.UiEvent
import app.cclauncher.ui.AppDrawerUiState
import app.cclauncher.ui.HomeScreenUiState
import app.cclauncher.ui.SettingsScreenUiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * MainViewModel is the primary ViewModel for CCLauncher that manages app state and user interactions.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    internal val appContext = application.applicationContext
    val prefsDataStore = PrefsDataStore(appContext)
    private val appRepository = AppRepository(appContext, prefsDataStore)
    private val permissionManager = PermissionManager(appContext)

    // Events manager for UI events
    private val _eventsFlow = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _eventsFlow.asSharedFlow()

    // UI States
    private val _homeScreenState = MutableStateFlow(HomeScreenUiState())
    val homeScreenState: StateFlow<HomeScreenUiState> = _homeScreenState.asStateFlow()

    private val _appDrawerState = MutableStateFlow(AppDrawerUiState())
    val appDrawerState: StateFlow<AppDrawerUiState> = _appDrawerState.asStateFlow()

    private val _settingsScreenState = MutableStateFlow(SettingsScreenUiState())
    val settingsScreenState: StateFlow<SettingsScreenUiState> = _settingsScreenState.asStateFlow()

    // App list state
    private val _appList = MutableStateFlow<List<AppModel>>(emptyList())
    val appList: StateFlow<List<AppModel>> = _appList.asStateFlow()

    private val _appListAll = MutableStateFlow<List<AppModel>>(emptyList())
    val appListAll: StateFlow<List<AppModel>> = _appListAll.asStateFlow()

    private val _hiddenApps = MutableStateFlow<List<AppModel>>(emptyList())
    val hiddenApps: StateFlow<List<AppModel>> = _hiddenApps.asStateFlow()

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Reset launcher state
    private val _launcherResetFailed = MutableStateFlow(false)
    val launcherResetFailed: StateFlow<Boolean> = _launcherResetFailed.asStateFlow()

    init {
        // Initialize UI states from preferences
        viewModelScope.launch {
            prefsDataStore.preferences.collect { prefs ->
                updateHomeScreenState(prefs)
                updateSettingsScreenState(prefs)
            }
        }
        viewModelScope.launch {
            appRepository.appListAll.collect { apps ->
                _appListAll.value = apps
                updateAppDrawerState()
            }
        }


        // Observe app list changes
        viewModelScope.launch {
            appRepository.appList.collect { apps ->
                _appList.value = apps
                updateAppDrawerState()
            }
        }

        // Observe hidden apps changes
        viewModelScope.launch {
            appRepository.hiddenApps.collect { apps ->
                _hiddenApps.value = apps
            }
        }
    }

    private fun updateHomeScreenState(prefs: LauncherPreferences) {
        _homeScreenState.value = HomeScreenUiState(
            homeAppsNum = prefs.homeAppsNum,
            dateTimeVisibility = prefs.dateTimeVisibility,
            homeAlignment = prefs.homeAlignment,
            homeBottomAlignment = prefs.homeBottomAlignment,
            homeApps = prefs.homeApps.map { app ->
                getAppModelFromPreference(app)
            }
        )
    }

    private fun updateSettingsScreenState(prefs: LauncherPreferences) {
        _settingsScreenState.value = SettingsScreenUiState(
            homeAppsNum = prefs.homeAppsNum,
            showAppNames = prefs.showAppNames,
            autoShowKeyboard = prefs.autoShowKeyboard,
            appTheme = prefs.appTheme,
            textSizeScale = prefs.textSizeScale,
            useSystemFont = prefs.useSystemFont,
            useDynamicTheme = prefs.useDynamicTheme,
            homeAlignment = prefs.homeAlignment,
            homeBottomAlignment = prefs.homeBottomAlignment,
            statusBar = prefs.statusBar,
            dateTimeVisibility = prefs.dateTimeVisibility,
            swipeLeftEnabled = prefs.swipeLeftEnabled,
            swipeRightEnabled = prefs.swipeRightEnabled,
            swipeLeftAppName = prefs.swipeLeftApp.label,
            swipeRightAppName = prefs.swipeRightApp.label,
            swipeDownAction = prefs.swipeDownAction,
            doubleTapToLock = prefs.doubleTapToLock,
        )
    }

    private fun updateAppDrawerState() {
        _appDrawerState.value = _appDrawerState.value.copy(
            apps = _appList.value,
            isLoading = false
        )
    }

    // Helper to convert preference to AppModel
    private fun getAppModelFromPreference(pref: HomeAppPreference): AppModel? {
        if (pref.packageName.isEmpty()) return null

        val userHandle = getUserHandleFromString(appContext, pref.userString)
        return AppModel(
            appLabel = pref.label,
            key = null,
            appPackage = pref.packageName,
            activityClassName = pref.activityClassName,
            user = userHandle
        )
    }

    /**
     * Handle first open of the app
     */
    fun firstOpen(value: Boolean) {
        viewModelScope.launch {
            prefsDataStore.setFirstOpen(value)
        }
    }

    /**
     * Update settings screen state
     */
    fun updateSettingsState() {
        viewModelScope.launch {
            try {
                val prefs = prefsDataStore.preferences.first()
                _settingsScreenState.value = SettingsScreenUiState(
                    homeAppsNum = prefs.homeAppsNum,
                    showAppNames = prefs.showAppNames,
                    autoShowKeyboard = prefs.autoShowKeyboard,
                    appTheme = prefs.appTheme,
                    textSizeScale = prefs.textSizeScale,
                    useSystemFont = prefs.useSystemFont,
                    homeAlignment = prefs.homeAlignment,
                    homeBottomAlignment = prefs.homeBottomAlignment,
                    statusBar = prefs.statusBar,
                    dateTimeVisibility = prefs.dateTimeVisibility,
                    swipeLeftEnabled = prefs.swipeLeftEnabled,
                    swipeRightEnabled = prefs.swipeRightEnabled,
                    swipeLeftAppName = prefs.swipeLeftApp.label,
                    swipeRightAppName = prefs.swipeRightApp.label,
                    swipeDownAction = prefs.swipeDownAction,
                    showHiddenAppsOnSearch = prefs.showHiddenAppsOnSearch,
                    autoOpenFilteredApp = prefs.autoOpenFilteredApp,
                    useDynamicTheme = prefs.useDynamicTheme,
                    doubleTapToLock = prefs.doubleTapToLock
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update settings: ${e.message}"
            }
        }
    }

    /**
     * Load all apps and visible apps
     */
    fun loadApps() {
        viewModelScope.launch {
            try {
                _appDrawerState.value = _appDrawerState.value.copy(isLoading = true)
                appRepository.loadApps()
                appRepository.loadAllApps()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load apps: ${e.message}"
                _appDrawerState.value = _appDrawerState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    /**
     * Load hidden apps
     */
    fun getHiddenApps() {
        viewModelScope.launch {
            try {
                _appDrawerState.value = _appDrawerState.value.copy(isLoading = true)
                appRepository.loadHiddenApps()
                _appDrawerState.value = _appDrawerState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load hidden apps: ${e.message}"
                _appDrawerState.value = _appDrawerState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    /**
     * Toggle app hidden state
     */
    fun toggleAppHidden(app: AppModel) {
        viewModelScope.launch {
            try {
                appRepository.toggleAppHidden(app)
                // Reload the app lists to reflect changes
                loadApps()
                getHiddenApps()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to toggle app visibility: ${e.message}"
            }
        }
    }

    /**
     * Launch an app
     */
    fun launchApp(app: AppModel) {
        viewModelScope.launch {
            try {
                appRepository.launchApp(app)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to launch app: ${e.message}"
            }
        }
    }

    /**
     * Handle app selection for various functions
     */
    fun selectedApp(appModel: AppModel, flag: Int) {
        when (flag) {
            Constants.FLAG_LAUNCH_APP, Constants.FLAG_HIDDEN_APPS -> {
                launchApp(appModel)
            }
            Constants.FLAG_SET_HOME_APP_1, Constants.FLAG_SET_HOME_APP_2,
            Constants.FLAG_SET_HOME_APP_3, Constants.FLAG_SET_HOME_APP_4,
            Constants.FLAG_SET_HOME_APP_5, Constants.FLAG_SET_HOME_APP_6,
            Constants.FLAG_SET_HOME_APP_7, Constants.FLAG_SET_HOME_APP_8 -> {
                setHomeApp(appModel, flag - Constants.FLAG_SET_HOME_APP_1)
            }
            Constants.FLAG_SET_SWIPE_LEFT_APP -> {
                setSwipeLeftApp(appModel)
            }
            Constants.FLAG_SET_SWIPE_RIGHT_APP -> {
                setSwipeRightApp(appModel)
            }
            Constants.FLAG_SET_CLOCK_APP -> {
                setClockApp(appModel)
            }
            Constants.FLAG_SET_CALENDAR_APP -> {
                setCalendarApp(appModel)
            }
        }
    }

    private fun setHomeApp(app: AppModel, position: Int) {
        viewModelScope.launch {
            prefsDataStore.setHomeApp(position, HomeAppPreference(
                label = app.appLabel,
                packageName = app.appPackage,
                activityClassName = app.activityClassName,
                userString = app.user.toString()
            ))
        }
    }

    private fun setSwipeLeftApp(app: AppModel) {
        viewModelScope.launch {
            prefsDataStore.setSwipeLeftApp(AppPreference(
                label = app.appLabel,
                packageName = app.appPackage,
                activityClassName = app.activityClassName,
                userString = app.user.toString()
            ))
        }
    }

    private fun setSwipeRightApp(app: AppModel) {
        viewModelScope.launch {
            prefsDataStore.setSwipeRightApp(AppPreference(
                label = app.appLabel,
                packageName = app.appPackage,
                activityClassName = app.activityClassName,
                userString = app.user.toString()
            ))
        }
    }

    private fun setClockApp(app: AppModel) {
        viewModelScope.launch {
            prefsDataStore.setClockApp(AppPreference(
                label = app.appLabel,
                packageName = app.appPackage,
                activityClassName = app.activityClassName,
                userString = app.user.toString()
            ))
        }
    }

    private fun setCalendarApp(app: AppModel) {
        viewModelScope.launch {
            prefsDataStore.setCalendarApp(AppPreference(
                label = app.appLabel,
                packageName = app.appPackage,
                activityClassName = app.activityClassName,
                userString = app.user.toString()
            ))
        }
    }

    fun lockScreen() {
        viewModelScope.launch {
            val prefs = prefsDataStore.preferences.first()
            if (prefs.doubleTapToLock) {
                // Use accessibility service to lock screen
                val intent = Intent(appContext, MyAccessibilityService::class.java)
                intent.action = "LOCK_SCREEN"
                appContext.startService(intent)
            }
        }
    }


    /**
     * Update home screen alignment
     */
    fun updateHomeAlignment(gravity: Int) {
        viewModelScope.launch {
            prefsDataStore.setHomeAlignment(gravity)
        }
    }

    /**
     * Toggle date and time visibility
     */
    fun toggleDateTime() {
        viewModelScope.launch {
            val currentVisibility = _homeScreenState.value.dateTimeVisibility
            prefsDataStore.setDateTimeVisibility(currentVisibility)
        }
    }

    /**
     * Update visibility of apps
     */
    fun updateShowApps(show: Boolean) {
        viewModelScope.launch {
            prefsDataStore.updatePreference { it.copy(showAppNames = show) }
        }
    }

    /**
     * Refresh home screen
     */
    fun refreshHome(appCountUpdated: Boolean) {
        if (appCountUpdated) {
            viewModelScope.launch {
                val currentCount = _homeScreenState.value.homeAppsNum
                prefsDataStore.setHomeAppsNum(currentCount)
            }
        }
    }

    /**
     * Launch home app at specified position
     */
    fun launchHomeApp(position: Int) {
        val app = getHomeAppModel(position)
        app?.let { launchApp(it) }
    }

    /**
     * Get home app model at specified position
     */
    fun getHomeAppModel(position: Int): AppModel? {
        if (position < 1 || position > 8) return null

        val homeApps = _homeScreenState.value.homeApps
        if (homeApps.size < position) return null

        return homeApps[position - 1]
    }

    /**
     * Launch swipe left app
     */
    fun launchSwipeLeftApp() {
        viewModelScope.launch {
            val prefs = prefsDataStore.preferences.first()
            if (prefs.swipeLeftApp.packageName.isNotEmpty()) {
                val app = AppModel(
                    appLabel = prefs.swipeLeftApp.label,
                    key = null,
                    appPackage = prefs.swipeLeftApp.packageName,
                    activityClassName = prefs.swipeLeftApp.activityClassName,
                    user = getUserHandleFromString(appContext, prefs.swipeLeftApp.userString)
                )
                launchApp(app)
            }
        }
    }

    /**
     * Launch swipe right app
     */
    fun launchSwipeRightApp() {
        viewModelScope.launch {
            val prefs = prefsDataStore.preferences.first()
            if (prefs.swipeRightApp.packageName.isNotEmpty()) {
                val app = AppModel(
                    appLabel = prefs.swipeRightApp.label,
                    key = null,
                    appPackage = prefs.swipeRightApp.packageName,
                    activityClassName = prefs.swipeRightApp.activityClassName,
                    user = getUserHandleFromString(appContext, prefs.swipeRightApp.userString)
                )
                launchApp(app)
            }
        }
    }

    /**
     * Open the configured clock app
     */
    fun openClockApp() {
        viewModelScope.launch {
            val prefs = prefsDataStore.preferences.first()
            if (prefs.clockApp.packageName.isNotEmpty()) {
                val app = AppModel(
                    appLabel = "Clock",
                    key = null,
                    appPackage = prefs.clockApp.packageName,
                    activityClassName = prefs.clockApp.activityClassName,
                    user = getUserHandleFromString(appContext, prefs.clockApp.userString)
                )
                launchApp(app)
            }
        }
    }

    /**
     * Open the configured calendar app
     */
    fun openCalendarApp() {
        viewModelScope.launch {
            val prefs = prefsDataStore.preferences.first()
            if (prefs.calendarApp.packageName.isNotEmpty()) {
                val app = AppModel(
                    appLabel = "Calendar",
                    key = null,
                    appPackage = prefs.calendarApp.packageName,
                    activityClassName = prefs.calendarApp.activityClassName,
                    user = getUserHandleFromString(appContext, prefs.calendarApp.userString)
                )
                launchApp(app)
            }
        }
    }

    /**
     * Search apps by query
     */
    fun searchApps(query: String) {
        viewModelScope.launch {
            _appDrawerState.value = _appDrawerState.value.copy(
                searchQuery = query,
                isLoading = true
            )

            try {
                val filteredApps = if (query.isBlank()) {
                    _appList.value
                } else {
                    val listToFilter = if (prefsDataStore.preferences.first().showHiddenAppsOnSearch) _appListAll else _appList
                    listToFilter.value.filter {
                        it.appLabel.contains(query, ignoreCase = true)
                    }
                }

                _appDrawerState.value = _appDrawerState.value.copy(
                    filteredApps = filteredApps,
                    isLoading = false
                )
            } catch (e: Exception) {
                _errorMessage.value = "Search failed: ${e.message}"
                _appDrawerState.value = _appDrawerState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }


    fun configureExistingWidget(widget: ExternalWidgetModel) {
        // Emit event to navigate to widget configuration screen
        viewModelScope.launch {
            _eventsFlow.emit(UiEvent.NavigateToWidgetConfig(widget))
        }
    }

    /**
     * Add an external widget to preferences
     */
    fun addExternalWidget(widget: ExternalWidgetModel) {
        viewModelScope.launch {
            prefsDataStore.addExternalWidget(widget)
        }
    }

    /**
     * Update an existing external widget
     */
    fun updateExternalWidget(widget: ExternalWidgetModel) {
        viewModelScope.launch {
            prefsDataStore.updateExternalWidget(widget)
        }
    }

    /**
     * Remove an external widget from preferences
     */
    fun removeExternalWidget(widgetId: String) {
        viewModelScope.launch {
            prefsDataStore.removeExternalWidget(widgetId)
        }
    }

    /**
     * Set whether widget backgrounds should be transparent
     */
    fun setTransparentWidgetBackground(transparent: Boolean) {
//        viewModelScope.launch {
//            prefsDataStore.setTransparentWidgetBackground(transparent)
//        }
    }

    /**
     * Reset launcher failed
     */
    fun setLauncherResetFailed(failed: Boolean) {
        _launcherResetFailed.value = failed
    }

    /**
     * Emit UI event
     */
    fun emitEvent(event: UiEvent) {
        viewModelScope.launch {
            _eventsFlow.emit(event)
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
        _appDrawerState.value = _appDrawerState.value.copy(error = null)
    }
}