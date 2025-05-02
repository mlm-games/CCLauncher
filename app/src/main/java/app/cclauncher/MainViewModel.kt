package app.cclauncher

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.cclauncher.data.*
import app.cclauncher.data.repository.AppRepository
import app.cclauncher.data.repository.SettingsRepository
import app.cclauncher.data.settings.AppSettings
import app.cclauncher.helper.MyAccessibilityService
import app.cclauncher.helper.PermissionManager
import app.cclauncher.helper.getUserHandleFromString
import app.cclauncher.ui.UiEvent
import app.cclauncher.ui.AppDrawerUiState
import app.cclauncher.ui.HomeScreenUiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * MainViewModel is the primary ViewModel for CCLauncher that manages app state and user interactions.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    val prefsDataStore = PrefsDataStore(appContext) // Keep for backward compatibility during transition
    val settingsRepository = SettingsRepository(appContext) // New settings repository
    private val appRepository = AppRepository(appContext, settingsRepository) // Will need refactoring in future
    private val permissionManager = PermissionManager(appContext)

    // Events manager for UI events
    private val _eventsFlow = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _eventsFlow.asSharedFlow()

    // UI States
    private val _homeScreenState = MutableStateFlow(HomeScreenUiState())
    val homeScreenState: StateFlow<HomeScreenUiState> = _homeScreenState.asStateFlow()

    private val _appDrawerState = MutableStateFlow(AppDrawerUiState())
    val appDrawerState: StateFlow<AppDrawerUiState> = _appDrawerState.asStateFlow()

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
        // Initialize UI states from settings
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                updateHomeScreenState(settings)
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

    private fun updateHomeScreenState(settings: AppSettings) {
        viewModelScope.launch {
            val homeApps = settingsRepository.getHomeApps()

            _homeScreenState.value = HomeScreenUiState(
                homeAppsNum = settings.homeAppsNum,
                homeScreenColumns = settings.homeScreenColumns,
                dateTimeVisibility = settings.dateTimeVisibility,
                homeAlignment = settings.homeAlignment,
                homeBottomAlignment = settings.homeBottomAlignment,
                homeApps = homeApps.map { app ->
                    getAppModelFromPreference(app)
                }
            )
        }
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
            settingsRepository.setFirstOpen(value)
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
            in Constants.FLAG_SET_HOME_APP_1..Constants.FLAG_SET_HOME_APP_16 -> {
                val position = flag - Constants.FLAG_SET_HOME_APP_1
                setHomeApp(appModel, position)
            }
        }
    }

    private fun setHomeApp(app: AppModel, position: Int) {
        viewModelScope.launch {
            settingsRepository.setHomeApp(position, HomeAppPreference(
                label = app.appLabel,
                packageName = app.appPackage,
                icon = app.appIcon,
                activityClassName = app.activityClassName,
                userString = app.user.toString()
            ))
        }
    }

    private fun setSwipeLeftApp(app: AppModel) {
        viewModelScope.launch {
            settingsRepository.setSwipeLeftApp(AppPreference(
                label = app.appLabel,
                packageName = app.appPackage,
                activityClassName = app.activityClassName,
                userString = app.user.toString()
            ))
        }
    }

    private fun setSwipeRightApp(app: AppModel) {
        viewModelScope.launch {
            settingsRepository.setSwipeRightApp(AppPreference(
                label = app.appLabel,
                packageName = app.appPackage,
                activityClassName = app.activityClassName,
                userString = app.user.toString()
            ))
        }
    }

    private fun setClockApp(app: AppModel) {
        viewModelScope.launch {
            settingsRepository.setClockApp(AppPreference(
                label = app.appLabel,
                packageName = app.appPackage,
                activityClassName = app.activityClassName,
                userString = app.user.toString()
            ))
        }
    }

    private fun setCalendarApp(app: AppModel) {
        viewModelScope.launch {
            settingsRepository.setCalendarApp(AppPreference(
                label = app.appLabel,
                packageName = app.appPackage,
                activityClassName = app.activityClassName,
                userString = app.user.toString()
            ))
        }
    }

    fun lockScreen() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            if (settings.doubleTapToLock) {
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
            settingsRepository.setHomeAlignment(gravity)
        }
    }

    /**
     * Toggle date and time visibility
     */
    fun toggleDateTime() {
        viewModelScope.launch {
            val currentVisibility = _homeScreenState.value.dateTimeVisibility
            settingsRepository.setDateTimeVisibility(currentVisibility)
        }
    }

    /**
     * Update visibility of apps
     */
    fun updateShowApps(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowAppNames(show)
        }
    }

    /**
     * Refresh home screen
     */
    fun refreshHome(appCountUpdated: Boolean) {
        if (appCountUpdated) {
            viewModelScope.launch {
                val currentCount = _homeScreenState.value.homeAppsNum
                settingsRepository.setHomeAppsNum(currentCount)

                val homeScreenColumnCount = _homeScreenState.value.homeScreenColumns
                settingsRepository.setHomeScreenColumns(homeScreenColumnCount)
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
        if (position < 1 || position > Constants.HomeAppCount.NUM) return null

        val homeApps = _homeScreenState.value.homeApps
        if (homeApps.size < position) return null

        return homeApps[position - 1]
    }

    /**
     * Launch swipe left app
     */
    fun launchSwipeLeftApp() {
        viewModelScope.launch {
            val swipeLeftApp = settingsRepository.getSwipeLeftApp()
            if (swipeLeftApp.packageName.isNotEmpty()) {
                val app = AppModel(
                    appLabel = swipeLeftApp.label,
                    key = null,
                    appPackage = swipeLeftApp.packageName,
                    activityClassName = swipeLeftApp.activityClassName,
                    user = getUserHandleFromString(appContext, swipeLeftApp.userString)
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
            val swipeRightApp = settingsRepository.getSwipeRightApp()
            if (swipeRightApp.packageName.isNotEmpty()) {
                val app = AppModel(
                    appLabel = swipeRightApp.label,
                    key = null,
                    appPackage = swipeRightApp.packageName,
                    activityClassName = swipeRightApp.activityClassName,
                    user = getUserHandleFromString(appContext, swipeRightApp.userString)
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
            val clockApp = settingsRepository.getClockApp()
            if (clockApp.packageName.isNotEmpty()) {
                val app = AppModel(
                    appLabel = "Clock",
                    key = null,
                    appPackage = clockApp.packageName,
                    activityClassName = clockApp.activityClassName,
                    user = getUserHandleFromString(appContext, clockApp.userString)
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
            val calendarApp = settingsRepository.getCalendarApp()
            if (calendarApp.packageName.isNotEmpty()) {
                val app = AppModel(
                    appLabel = "Calendar",
                    key = null,
                    appPackage = calendarApp.packageName,
                    activityClassName = calendarApp.activityClassName,
                    user = getUserHandleFromString(appContext, calendarApp.userString)
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
                val settings = settingsRepository.settings.first()
                val searchType = settings.searchType

                val filteredApps = if (query.isBlank()) {
                    _appList.value
                } else {
                    val listToFilter = if (settings.showHiddenAppsOnSearch) _appListAll else _appList

                    when (searchType) {
                        Constants.SearchType.FUZZY -> {
                            // Fuzzy search implementation
                            listToFilter.value.filter { app ->
                                fuzzyMatch(app.appLabel, query)
                            }
                        }
                        Constants.SearchType.STARTS_WITH -> {
                            // Starts with implementation
                            listToFilter.value.filter { app ->
                                app.appLabel.startsWith(query, ignoreCase = true)
                            }
                        }
                        else -> {
                            // Default contains search
                            listToFilter.value.filter { app ->
                                app.appLabel.contains(query, ignoreCase = true)
                            }
                        }
                    }
                }

                _appDrawerState.value = _appDrawerState.value.copy(
                    filteredApps = filteredApps,
                    isLoading = false
                )

                // Auto-open single match if enabled
                if (filteredApps.size == 1 && query.isNotEmpty() && settings.autoOpenFilteredApp) {
                    launchApp(filteredApps[0])
                }
            } catch (e: Exception) {
                _errorMessage.value = "Search failed: ${e.message}"
                _appDrawerState.value = _appDrawerState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    private fun fuzzyMatch(text: String, pattern: String): Boolean {
        val textLower = text.lowercase()
        val patternLower = pattern.lowercase()

        var textIndex = 0
        var patternIndex = 0

        while (textIndex < textLower.length && patternIndex < patternLower.length) {
            if (textLower[textIndex] == patternLower[patternIndex]) {
                patternIndex++
            }
            textIndex++
        }

        return patternIndex == patternLower.length
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

    /**
     * Update orientation based on settings
     */
    fun updateOrientation(isLandscape: Boolean) {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val showIcons = if (settings.showAppIcons) {
                if (isLandscape) settings.showIconsInLandscape else settings.showIconsInPortrait
            } else {
                false
            }

            // Update any UI state that depends on orientation
            // This would be expanded in a full implementation
        }
    }

    /**
     * Apply font settings to text
     */
    fun getFontWeight(settings: AppSettings) = when (settings.fontWeight) {
        0 -> androidx.compose.ui.text.font.FontWeight.Thin
        1 -> androidx.compose.ui.text.font.FontWeight.Light
        2 -> androidx.compose.ui.text.font.FontWeight.Normal
        3 -> androidx.compose.ui.text.font.FontWeight.Medium
        4 -> androidx.compose.ui.text.font.FontWeight.Bold
        5 -> androidx.compose.ui.text.font.FontWeight.Black
        else -> androidx.compose.ui.text.font.FontWeight.Normal
    }

    /**
     * Get item spacing based on settings
     */
    fun getItemSpacing(settings: AppSettings) = when (settings.itemSpacing) {
        0 -> 0
        1 -> 4
        2 -> 8
        3 -> 16
        else -> 8
    }
}