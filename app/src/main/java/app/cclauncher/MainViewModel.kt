package app.cclauncher

import android.app.Activity.RESULT_OK
import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.cclauncher.data.*
import app.cclauncher.data.repository.AppRepository
import app.cclauncher.data.repository.SettingsRepository
import app.cclauncher.data.settings.AppPreference
import app.cclauncher.data.settings.HomeAppPreference
import app.cclauncher.helper.MyAccessibilityService
import app.cclauncher.helper.PermissionManager
import app.cclauncher.helper.getScreenDimensions
import app.cclauncher.helper.getUserHandleFromString
import app.cclauncher.ui.UiEvent
import app.cclauncher.ui.AppDrawerUiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * MainViewModel is the primary ViewModel for CCLauncher that manages app state and user interactions.
 */
class MainViewModel(application: Application, private val appWidgetHost: AppWidgetHost) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    val settingsRepository = SettingsRepository(appContext) // New settings repository
    private val appRepository = AppRepository(appContext, settingsRepository) // Will need refactoring in future
    private val permissionManager = PermissionManager(appContext)

    private val REQUEST_CODE_CONFIGURE_WIDGET = 101
    private var pendingWidgetInfo: PendingWidgetInfo? = null
    data class PendingWidgetInfo(val appWidgetId: Int, val providerInfo: android.appwidget.AppWidgetProviderInfo)

    // Events manager for UI events
    private val _eventsFlow = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _eventsFlow.asSharedFlow()

    // UI States
    private val _homeLayoutState = MutableStateFlow(HomeLayout())
    val homeLayoutState: StateFlow<HomeLayout> = _homeLayoutState.asStateFlow()

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

    val appWidgetManager =  AppWidgetManager.getInstance(appContext)

    init {

        viewModelScope.launch {
            settingsRepository.getHomeLayout().collect { layout ->
                _homeLayoutState.value = layout
            }
        }

        // Initialize UI states from settings
//        viewModelScope.launch {
//            settingsRepository.settings.collect { settings ->
//                updateHomeScreenState(settings)
//            }
//        }

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
//
//    private fun updateHomeScreenState(settings: AppSettings) {
//        viewModelScope.launch {
//            val homeApps = settingsRepository.getHomeApps()
//
//            _homeScreenState.value = HomeScreenUiState(
//                homeAppsNum = settings.homeAppsNum,
//                homeScreenColumns = settings.homeScreenColumns,
//                dateTimeVisibility = settings.dateTimeVisibility,
////                homeAlignment = settings.homeAlignment,
//                homeBottomAlignment = settings.homeBottomAlignment,
//                homeApps = homeApps.map { app ->
//                    getAppModelFromPreference(app)
//                }
//            )
//        }
//    }

    fun addAppToHomeScreen(appModel: AppModel) {
        viewModelScope.launch {
            val nextPos = findNextAvailableGridPosition(_homeLayoutState.value, 1, 1)
            if (nextPos != null) {
                val appItem = HomeItem.App(
                    appModel = appModel,
                    row = nextPos.first,
                    column = nextPos.second
                )
                val currentLayout = _homeLayoutState.value
                val newItems = currentLayout.items + appItem
                settingsRepository.saveHomeLayout(currentLayout.copy(items = newItems))
            } else {
                _errorMessage.value = "No space available on home screen."
            }
        }
    }

    // Function to remove an app from the home screen layout
    fun removeAppFromHomeScreen(appItem: HomeItem.App) {
        viewModelScope.launch {
            val currentLayout = _homeLayoutState.value
            val newItems = currentLayout.items.filterNot { it.id == appItem.id }
            settingsRepository.saveHomeLayout(currentLayout.copy(items = newItems))
        }
    }

    private fun getCellSizeDp(screenWidthDp: Int, screenHeightDp: Int, rows: Int, columns: Int): Pair<Float, Float> {
        val cellWidthDp = screenWidthDp.toFloat() / columns
        val cellHeightDp = screenHeightDp.toFloat() / rows // Use available height
        return Pair(cellWidthDp, cellHeightDp)
    }

    private fun findNextAvailableGridPosition(layout: HomeLayout, widthSpan: Int, heightSpan: Int): Pair<Int, Int>? {
        val occupied = Array(layout.rows) { BooleanArray(layout.columns) }

        // Mark occupied cells
        layout.items.forEach { item ->
            for (r in item.row until (item.row + item.rowSpan).coerceAtMost(layout.rows)) {
                for (c in item.column until (item.column + item.columnSpan).coerceAtMost(layout.columns)) {
                    if (r >= 0 && c >= 0) { // Basic bounds check
                        occupied[r][c] = true
                    }
                }
            }
        }

        // Find the first available top-left corner for the required span
        for (r in 0 .. layout.rows - heightSpan) {
            for (c in 0 .. layout.columns - widthSpan) {
                if (isSpaceFreeInternal(occupied, r, c, widthSpan, heightSpan, layout.rows, layout.columns)) {
                    return Pair(r, c) // Found a spot
                }
            }
        }

        return null // No space found
    }


    private fun updateAppDrawerState() {
        _appDrawerState.value = _appDrawerState.value.copy(
            apps = _appList.value,
            isLoading = false
        )
    }

    fun startWidgetConfiguration(providerInfo: android.appwidget.AppWidgetProviderInfo) {
        viewModelScope.launch {
            try {
                val appWidgetId = appWidgetHost.allocateAppWidgetId()
                pendingWidgetInfo = PendingWidgetInfo(appWidgetId, providerInfo)

                if (providerInfo.configure != null) {
                    val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                        component = providerInfo.configure
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    emitEvent(UiEvent.StartActivityForResult(configIntent, REQUEST_CODE_CONFIGURE_WIDGET))
                } else {
                    // No configuration needed, add directly
                    addWidgetToLayout(appWidgetId, providerInfo)
                }
            } catch (e: Exception) {
                Log.e("ViewModelWidget", "Error allocating or configuring widget", e)
                _errorMessage.value = "Failed to add widget."
                pendingWidgetInfo?.let { appWidgetHost.deleteAppWidgetId(it.appWidgetId) } // Clean up allocated ID
                pendingWidgetInfo = null
            }
        }
    }

    private fun addWidgetToLayout(appWidgetId: Int, providerInfo: android.appwidget.AppWidgetProviderInfo) {
        viewModelScope.launch {
            val nextPos = findNextAvailableGridPosition(
                _homeLayoutState.value,
                providerInfo.minResizeWidth / 70, // Approximate cell calculation
                providerInfo.minResizeHeight / 70
            )

            if (nextPos != null) {
                val widgetItem = HomeItem.Widget(
                    appWidgetId = appWidgetId,
                    packageName = providerInfo.provider.packageName,
                    providerClassName = providerInfo.provider.className,
                    row = nextPos.first,
                    column = nextPos.second,
                    rowSpan = providerInfo.minResizeHeight / 70, // TODO: Example initial size
                    columnSpan = providerInfo.minResizeWidth / 70,
                )

                val success = appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, providerInfo.provider)

                if(success) {
                    val currentLayout = _homeLayoutState.value
                    val newItems = currentLayout.items + widgetItem
                    val newLayout = currentLayout.copy(items = newItems)
                    settingsRepository.saveHomeLayout(newLayout)
                } else {
                    Log.e("ViewModelWidget", "Failed to bind widget ID $appWidgetId")
                    _errorMessage.value = "Could not bind widget."
                    appWidgetHost.deleteAppWidgetId(appWidgetId)
                }

            } else {
                Log.w("ViewModelWidget", "No space found for widget ID $appWidgetId")
                _errorMessage.value = "No space available on home screen."
                appWidgetHost.deleteAppWidgetId(appWidgetId)
            }
            pendingWidgetInfo = null
        }
    }

    private fun isSpaceFreeInternal(occupiedGrid: Array<BooleanArray>, startRow: Int, startCol: Int, spanW: Int, spanH: Int, maxRows: Int, maxCols: Int): Boolean {
        // if (startRow + spanH > maxRows || startCol + spanW > maxCols) return false

        // Check all cells within the desired span
        for (r in startRow until startRow + spanH) {
            for (c in startCol until startCol + spanW) {
                if (r >= maxRows || c >= maxCols || occupiedGrid[r][c]) {
                    return false // occupied cell
                }
            }
        }
        return true
    }

    private fun checkResizeValidity(layout: HomeLayout, widgetToResize: HomeItem.Widget, newRowSpan: Int, newColSpan: Int): Boolean {
        val targetRow = widgetToResize.row
        val targetCol = widgetToResize.column

        if (targetRow + newRowSpan > layout.rows || targetCol + newColSpan > layout.columns) {
            return false
        }

        for (item in layout.items) {
            if (item.id == widgetToResize.id) continue // Skip self

            val horizontalOverlap = (item.column < targetCol + newColSpan) && (item.column + item.columnSpan > targetCol)
            val verticalOverlap = (item.row < targetRow + newRowSpan) && (item.row + item.rowSpan > targetRow)

            if (horizontalOverlap && verticalOverlap) {
                return false
            }
        }
        return true
    }


    fun resizeWidget(widgetItem: HomeItem.Widget, newRowSpan: Int, newColSpan: Int) {
        viewModelScope.launch {
            val currentLayout = _homeLayoutState.value

            // --- Implement Placeholder: Resize Validation ---
            if (!checkResizeValidity(currentLayout, widgetItem, newRowSpan, newColSpan)) {
                _errorMessage.value = "Cannot resize widget: overlaps or out of bounds."
                return@launch
            }

            val newItems = currentLayout.items.map {
                if (it.id == widgetItem.id && it is HomeItem.Widget) {
                    it.copy(rowSpan = newRowSpan, columnSpan = newColSpan)
                } else {
                    it
                }
            }
            val newLayout = currentLayout.copy(items = newItems)
            settingsRepository.saveHomeLayout(newLayout) // Persist the layout change

            // --- Implement Placeholder: Notify Widget Size Change ---
            // This requires knowing the screen/cell dimensions. Get from settings or calculate.
            // For simplicity, assume we can get screen Dp here (might need context or pass from UI)
            val screenWidthDp = getScreenDimensions(context = appContext).first // Example placeholder - GET ACTUAL VALUE
            val screenHeightDp = getScreenDimensions(appContext).second // Example placeholder - GET ACTUAL VALUE
            val (cellWidthDp, cellHeightDp) = getCellSizeDp(
                screenWidthDp, screenHeightDp, currentLayout.rows, currentLayout.columns
            )

            val minWidth = (newColSpan * cellWidthDp).toInt()
            val maxWidth = minWidth // Keep min/max same for simplicity now
            val minHeight = (newRowSpan * cellHeightDp).toInt()
            val maxHeight = minHeight

            val options = Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, minWidth)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, maxWidth)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, minHeight)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, maxHeight)
            }
            try {
                appWidgetManager.updateAppWidgetOptions(widgetItem.appWidgetId, options)
            } catch (e: Exception) {
                Log.e("ViewModelWidget", "Failed to update widget options for ID ${widgetItem.appWidgetId}", e)
            }
        }
    }
    fun removeWidget(widgetItem: HomeItem.Widget) {
        viewModelScope.launch {
            try {
                appWidgetHost.deleteAppWidgetId(widgetItem.appWidgetId)
                val currentLayout = _homeLayoutState.value
                val newItems = currentLayout.items.filterNot { it.id == widgetItem.id }
                val newLayout = currentLayout.copy(items = newItems)
                settingsRepository.saveHomeLayout(newLayout)
            } catch (e: Exception) {
                Log.e("ViewModelWidget", "Error deleting widget ID ${widgetItem.appWidgetId}", e)
                _errorMessage.value = "Failed to remove widget."
            }
        }
    }

    fun requestWidgetReconfigure(widgetItem: HomeItem.Widget) {
        viewModelScope.launch {
            val providerInfo = getAppWidgetInfo(widgetItem.packageName, widgetItem.providerClassName)
            if (providerInfo?.configure != null) {
                try {
                    val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                        component = providerInfo.configure
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetItem.appWidgetId)
                    }
                    // Store necessary info if result needs handling (e.g., update state on success/cancel)
                    // pendingReconfigureWidgetId = widgetItem.appWidgetId
                    emitEvent(UiEvent.StartActivityForResult(configIntent, REQUEST_CODE_CONFIGURE_WIDGET)) // Use same code or new one
                } catch (e: Exception) {
                    Log.e("ViewModelWidget", "Error requesting reconfigure for ${widgetItem.appWidgetId}", e)
                    _errorMessage.value = "Failed to reconfigure widget."
                }
            } else {
                _errorMessage.value = "This widget cannot be reconfigured."
            }
        }
    }

    // Helper to get provider info at runtime
    private fun getAppWidgetInfo(packageName: String, className: String): android.appwidget.AppWidgetProviderInfo? {
        return appWidgetManager.installedProviders.find {
            it.provider.packageName == packageName && it.provider.className == className
        }
    }

    // Handle result from widget configuration Activity
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_CONFIGURE_WIDGET) {
            val widgetId = pendingWidgetInfo?.appWidgetId ?: data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID

            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                if (resultCode == RESULT_OK) {
                    // Configuration successful
                    pendingWidgetInfo?.let { info ->
                        if(info.appWidgetId == widgetId) { // Ensure it's the one we were waiting for
                            addWidgetToLayout(info.appWidgetId, info.providerInfo)
                        }
                    } ?: run {
                        // Handle reconfigure success if needed (e.g., update UI state)
                        Log.d("ViewModelWidget", "Widget ID $widgetId reconfigured successfully.")
                        // Force UI refresh if needed
                        viewModelScope.launch { settingsRepository.triggerHomeLayoutRefresh() }
                    }
                } else {
                    // Configuration cancelled or failed
                    Log.w("ViewModelWidget", "Widget configuration cancelled/failed for ID $widgetId")
                    appWidgetHost.deleteAppWidgetId(widgetId) // Clean up allocated ID
                    _errorMessage.value = "Widget configuration cancelled."
                    pendingWidgetInfo = null // Clear pending state
                }
            }
            pendingWidgetInfo = null // Clear pending state regardless
        }
        // Handle other activity results if needed
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
                _appDrawerState.value =
                    _appDrawerState.value.copy(isLoading = false, error = e.message)
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
                _appDrawerState.value =
                    _appDrawerState.value.copy(isLoading = false, error = e.message)
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
                activityClassName = app.activityClassName,
                userString = app.user.toString()))
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
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
        _appDrawerState.value = _appDrawerState.value.copy(error = null)
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
    }
