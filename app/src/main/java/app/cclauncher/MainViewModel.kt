package app.cclauncher

import android.app.Activity.RESULT_OK
import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.cclauncher.data.*
import app.cclauncher.data.Constants.MAX_PAGES
import app.cclauncher.data.repository.AppRepository
import app.cclauncher.settings.AppSettingsRepository
import app.cclauncher.settings.AppPreference
import app.cclauncher.settings.AppSettings
import app.cclauncher.helper.IconCache
import app.cclauncher.helper.MyAccessibilityService
import app.cclauncher.helper.PrivateSpaceHelper
 import app.cclauncher.helper.SearchAliasUtils
import app.cclauncher.helper.getScreenDimensions
import app.cclauncher.helper.getUserHandleFromString
import app.cclauncher.ui.UiEvent
import app.cclauncher.ui.AppDrawerUiState
import app.cclauncher.ui.components.snackbar.SnackbarManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID
import kotlin.math.ceil

/**
 * MainViewModel is the primary ViewModel for CCLauncher that manages app state and user interactions.
 */
class MainViewModel(application: Application, private val appWidgetHost: AppWidgetHost) : AndroidViewModel(application), KoinComponent {
    private val appContext = application.applicationContext
    val settingsRepository: AppSettingsRepository by inject()
    private val appRepository: AppRepository by inject()

    private val REQUEST_CODE_CONFIGURE_WIDGET = WidgetConstants.REQUEST_CODE_BIND_WIDGET
    private var pendingWidgetInfo: PendingWidgetInfo? = null

    private val _refreshTrigger = MutableStateFlow(0)
    val refreshTrigger = _refreshTrigger.asStateFlow()

    private val privateSpaceHelper = PrivateSpaceHelper(application.applicationContext)

    val isPrivateSpaceSupported = privateSpaceHelper.isPrivateSpaceSupported()

    private val _privateSpaceState = MutableStateFlow<PrivateSpaceState>(PrivateSpaceState.Unsupported)
    val privateSpaceState: StateFlow<PrivateSpaceState> = _privateSpaceState.asStateFlow()

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

    // Reset launcher state
    private val _launcherResetFailed = MutableStateFlow(false)
    val launcherResetFailed: StateFlow<Boolean> = _launcherResetFailed.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    val appWidgetManager: AppWidgetManager =  AppWidgetManager.getInstance(appContext)

    val snackbarManager : SnackbarManager by inject()

    private val appRefreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action == "app.cclauncher.ACTION_REFRESH_APPS") {
                loadApps()
                updatePrivateSpaceState()
            }
        }
    }

    // Alias search index: key = app.getKey(), value = set of aliases
    private var searchAliasIndex: Map<String, Set<String>> = emptyMap()

    init {

        viewModelScope.launch {
            combine(
                settingsRepository.getHomeLayout(),
                settingsRepository.settings
            ) { layout, settings ->
                if (settings.homeScreenPages != layout.pageCount) {
                    settingsRepository.updateSetting("homeScreenPages", layout.pageCount)
                }
                val updatedLayout = layout.copy(
                    rows = settings.homeScreenRows,
                    columns = settings.homeScreenColumns
                )
                loadIconsForHomeLayout(updatedLayout, settings)
            }.collect { updatedLayout ->
                _homeLayoutState.value = updatedLayout
            }
        }

        viewModelScope.launch {
            combine(
                settingsRepository.getHomeLayout(),
                settingsRepository.settings
            ) { layout, settings ->
                val updatedLayout = layout.copy(
                    rows = settings.homeScreenRows,
                    columns = settings.homeScreenColumns
                )
                loadIconsForHomeLayout(updatedLayout, settings)
            }.collect { updatedLayout ->
                _homeLayoutState.value = updatedLayout
            }
        }

        viewModelScope.launch {
            appRepository.appListAll.collect { apps ->
                _appListAll.value = apps
                updateAppDrawerState()
                reapplySearchFilter()
            }
        }

        // Observe app list changes
        viewModelScope.launch {
            appRepository.appList.collect { apps ->
                _appList.value = apps
                updateAppDrawerState()
                reapplySearchFilter()
            }
        }

        // Observe hidden apps changes
        viewModelScope.launch {
            appRepository.hiddenApps.collect { apps ->
                _hiddenApps.value = apps
            }
        }

        viewModelScope.launch {
            settingsRepository.settings
                .map { it.selectedIconPack }
                .distinctUntilChanged()
                .drop(1) // Skip initial value
                .collect { _ ->
                    refreshHomeScreenAppIcons()
                }
        }

        // Rebuild alias index whenever app list or relevant settings change
        viewModelScope.launch {
            combine(
                appRepository.appListAll,
                settingsRepository.settings
                    .map { it.searchAliasesMode to it.searchIncludePackageNames }
                    .distinctUntilChanged()
            ) { _, _ -> }
                .collect {
                    rebuildSearchAliasIndex()
                    reapplySearchFilter()
                }
        }

        viewModelScope.launch {
            settingsRepository.settings
                .map { Triple(it.searchType, it.searchSortOrder, it.showHiddenAppsOnSearch) }
                .distinctUntilChanged()
                .collect { reapplySearchFilter() }
        }

        updatePrivateSpaceState()

        val filter = IntentFilter("app.cclauncher.ACTION_REFRESH_APPS")
        ContextCompat.registerReceiver(
            appContext,
            appRefreshReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private suspend fun rebuildSearchAliasIndex() {
        val settings = settingsRepository.settings.first()
        val mode = settings.searchAliasesMode
        val includePkg = settings.searchIncludePackageNames

        if (mode == SearchAliasUtils.Mode.OFF && !includePkg) {
            searchAliasIndex = emptyMap()
            return
        }

        val idx = HashMap<String, Set<String>>(_appListAll.value.size)
        for (app in _appListAll.value) {
            val aliases = SearchAliasUtils.buildAppAliases(
                label = app.appLabel,
                packageName = app.appPackage,
                mode = mode,
                includePkg = includePkg
            )
            idx[app.getKey()] = aliases
        }
        searchAliasIndex = idx
    }

    /**
     * Load icons for all apps in the home layout
     */
    private suspend fun loadIconsForHomeLayout(layout: HomeLayout, settings: AppSettings): HomeLayout {
        if (!settings.showHomeScreenIcons) {
            return layout // Don't load icons if they're not shown
        }

        val iconCache = IconCache(appContext)

        val updatedItems = layout.items.map { item ->
            when (item) {
                is HomeItem.App -> {
                    val resolvedUser = getUserHandleFromString(appContext, item.appModel.userString)
                    val icon = iconCache.getIcon(
                        packageName = item.appModel.appPackage,
                        className = item.appModel.activityClassName,
                        user = resolvedUser,
                        iconPackName = settings.selectedIconPack,
                    )
                    val updatedAppModel = item.appModel.copy(appIcon = icon)
                    item.copy(appModel = updatedAppModel)
                }
                is HomeItem.Widget -> item
            }
        }

        return layout.copy(items = updatedItems)
    }

    private suspend fun refreshHomeScreenAppIcons() {
        val currentLayout = _homeLayoutState.value
        val settings = settingsRepository.settings.first()
        val iconCache = IconCache(appContext)

        val updatedItems = currentLayout.items.map { item ->
            when (item) {
                is HomeItem.App -> {
                    val updatedIcon = if (settings.showHomeScreenIcons) {
                        iconCache.getIcon(
                            packageName = item.appModel.appPackage,
                            className = item.appModel.activityClassName,
                            user = item.appModel.user,
                            iconPackName = settings.selectedIconPack,
                        )
                    } else {
                        null
                    }
                    val updatedAppModel = item.appModel.copy(appIcon = updatedIcon)
                    item.copy(appModel = updatedAppModel)
                }
                is HomeItem.Widget -> item
            }
        }

        val updatedLayout = currentLayout.copy(items = updatedItems)
        _homeLayoutState.value = updatedLayout
        settingsRepository.saveHomeLayout(updatedLayout)
    }

    suspend fun updateGridSize(newRows: Int, newColumns: Int) {
        val currentLayout = _homeLayoutState.value

        val itemsOutOfBounds = currentLayout.items.filter { item ->
            item.row + item.rowSpan > newRows || item.column + item.columnSpan > newColumns
        }

        if (itemsOutOfBounds.isNotEmpty()) {
            val updatedItems = currentLayout.items.map { item ->
                if (item.row + item.rowSpan > newRows || item.column + item.columnSpan > newColumns) {
                    val newPosition = findNextAvailableGridPosition(
                        currentLayout.copy(rows = newRows, columns = newColumns),
                        item.columnSpan,
                        item.rowSpan
                    )
                    when (item) {
                        is HomeItem.App -> item.copy(
                            row = newPosition?.first ?: 0,
                            column = newPosition?.second ?: 0
                        )
                        is HomeItem.Widget -> item.copy(
                            row = newPosition?.first ?: 0,
                            column = newPosition?.second ?: 0
                        )
                    }
                } else item
            }

            val newLayout = currentLayout.copy(
                items = updatedItems,
                rows = newRows,
                columns = newColumns
            )
            settingsRepository.saveHomeLayout(newLayout)
        } else {
            val newLayout = currentLayout.copy(rows = newRows, columns = newColumns)
            settingsRepository.saveHomeLayout(newLayout)
        }
    }

    fun addAppToHomeScreen(appModel: AppModel, targetPage: Int? = null) {
        viewModelScope.launch {
            Log.d("HomeScreen", "Attempting to add app: ${appModel.appLabel}")
            val currentLayout = _homeLayoutState.value
            val page = targetPage ?: _currentPage.value

            val nextPos = findNextAvailableGridPosition(currentLayout, 1, 1, page)

            if (nextPos != null) {
                val appModelWithUserString = appModel.copy(userString = appModel.userString)
                val appItem = HomeItem.App(
                    appModel = appModelWithUserString,
                    page = page,
                    row = nextPos.first,
                    column = nextPos.second
                )
                val existingItem = currentLayout.items.find {
                    it is HomeItem.App && it.appModel.getKey() == appModel.getKey()
                }
                if (existingItem == null) {
                    val newItems = currentLayout.items + appItem
                    settingsRepository.saveHomeLayout(currentLayout.copy(items = newItems))
                }
            } else {
                if (page < currentLayout.pageCount - 1) {
                    addAppToHomeScreen(appModel, page + 1)
                } else if (currentLayout.pageCount < MAX_PAGES) {
                    // Adds a new page
                    val newLayout = currentLayout.copy(pageCount = currentLayout.pageCount + 1)
                    settingsRepository.saveHomeLayout(newLayout)
                    addAppToHomeScreen(appModel, currentLayout.pageCount)
                } else {
                    snackbarManager.show("No space available on any home screen page.")
                }
            }
        }
    }


    fun toggleAppInPrivateSpace(app: AppModel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            viewModelScope.launch {
                try {
                    val privateSpaceHelper = PrivateSpaceHelper(appContext)

                    if (!privateSpaceHelper.isPrivateSpaceSupported()) {
                        snackbarManager.show("Private Space requires Android 15 or higher")
                        return@launch
                    }

                    if (!privateSpaceHelper.isPrivateSpaceSetUp()) {
                        snackbarManager.show("Private Space is not set up on this device. Set it up in Android Settings.")
                        return@launch
                    }

                    if (privateSpaceHelper.isPrivateSpaceLocked()) {
                        snackbarManager.show("Private Space is locked. Please unlock it first.")
                        return@launch
                    }

                    val isInPrivateSpace = privateSpaceHelper.isPrivateSpaceProfile(app.user)
                    val message = if (isInPrivateSpace) {
                        "To remove apps from Private Space, use Android Settings > Private Space settings"
                    } else {
                        "To add apps to Private Space, use Android Settings > Private Space settings"
                    }

                    snackbarManager.show(message)

                    loadApps()
                    updatePrivateSpaceState()
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error with Private Space operation", e)
                    snackbarManager.show("Private Space operation failed: ${e.message}")
                }
            }
        } else {
            snackbarManager.show("Private Space requires Android 15 or higher")
        }
    }

    fun removeAppFromHomeScreen(appItem: HomeItem.App) {
        viewModelScope.launch {
            val currentLayout = _homeLayoutState.value
            val newItems = currentLayout.items.filterNot { it.id == appItem.id }
            settingsRepository.saveHomeLayout(currentLayout.copy(items = newItems))
        }
    }

    private fun getCellSizeDp(screenWidthDp: Int, screenHeightDp: Int, rows: Int, columns: Int): Pair<Float, Float> {
        val cellWidthDp = screenWidthDp.toFloat() / columns
        val cellHeightDp = screenHeightDp.toFloat() / rows
        return Pair(cellWidthDp, cellHeightDp)
    }

    private fun findNextAvailableGridPosition(
        layout: HomeLayout,
        widthSpan: Int,
        heightSpan: Int,
        page: Int = 0,
    ): Pair<Int, Int>? {
        val occupied = Array(layout.rows) { BooleanArray(layout.columns) }

        layout.itemsForPage(page).forEach { item ->
            for (r in item.row until (item.row + item.rowSpan).coerceAtMost(layout.rows)) {
                for (c in item.column until (item.column + item.columnSpan).coerceAtMost(layout.columns)) {
                    if (r >= 0 && c >= 0) occupied[r][c] = true
                }
            }
        }

        for (r in 0..layout.rows - heightSpan) {
            for (c in 0..layout.columns - widthSpan) {
                if (isSpaceFreeInternal(occupied, r, c, widthSpan, heightSpan, layout.rows, layout.columns)) {
                    return Pair(r, c)
                }
            }
        }
        return null
    }

    private fun isSpaceFreeInternal(occupiedGrid: Array<BooleanArray>, startRow: Int, startCol: Int, spanW: Int, spanH: Int, maxRows: Int, maxCols: Int): Boolean {
        for (r in startRow until startRow + spanH) {
            for (c in startCol until startCol + spanW) {
                if (r >= maxRows || c >= maxCols || occupiedGrid[r][c]) return false
            }
        }
        return true
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
                @Suppress("SENSELESS_COMPARISON")
                if (providerInfo == null) {
                    Log.e("WidgetDebug", "CRITICAL: providerInfo is NULL in startWidgetConfiguration")
                    snackbarManager.show("Internal error: Widget provider information missing.")
                    return@launch
                }

                val componentName = providerInfo.provider
                if (componentName == null) {
                    Log.e("WidgetDebug", "CRITICAL: providerInfo.provider is NULL")
                    snackbarManager.show("Internal error: Widget component name missing.")
                    return@launch
                }

                val appWidgetId = appWidgetHost.allocateAppWidgetId()
                val bindSuccess = appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, componentName)

                if (bindSuccess) {
                    if (providerInfo.configure != null) {
                        pendingWidgetInfo = PendingWidgetInfo(appWidgetId, providerInfo)
                        emitEvent(UiEvent.ConfigureWidget(appWidgetId, providerInfo))
                    }
                    addWidgetToLayout(appWidgetId, providerInfo)
                } else {
                    pendingWidgetInfo = PendingWidgetInfo(appWidgetId, providerInfo)
                    val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, componentName)
                    }
                    emitEvent(UiEvent.StartActivityForResult(bindIntent, WidgetConstants.REQUEST_CODE_BIND_WIDGET))
                }
            } catch (e: Exception) {
                Log.e("WidgetDebug", "Error in startWidgetConfiguration", e)
                snackbarManager.show("Failed to start widget configuration: ${e.message}")
            }
        }
    }

    private fun addWidgetToLayout(appWidgetId: Int, providerInfo: android.appwidget.AppWidgetProviderInfo) {
        viewModelScope.launch {
            try {
                val screenDimensions = getScreenDimensions(context = appContext)
                val screenWidthDp = screenDimensions.first
                val screenHeightDp = screenDimensions.second

                val currentLayout = _homeLayoutState.value
                val cellWidthDp = screenWidthDp / currentLayout.columns
                val cellHeightDp = screenHeightDp / currentLayout.rows

                val widgetWidthCells = 1.coerceAtLeast(ceil(providerInfo.minWidth.toDouble() / cellWidthDp).toInt())
                val widgetHeightCells = 1.coerceAtLeast(ceil(providerInfo.minHeight.toDouble() / cellHeightDp).toInt())

                var targetPage = _currentPage.value
                var nextPos = findNextAvailableGridPosition(currentLayout, widgetWidthCells, widgetHeightCells, targetPage)

                if (nextPos == null) {
                    for (page in 0 until currentLayout.pageCount) {
                        if (page != targetPage) {
                            nextPos = findNextAvailableGridPosition(currentLayout, widgetWidthCells, widgetHeightCells, page)
                            if (nextPos != null) {
                                targetPage = page
                                break
                            }
                        }
                    }
                }

                if (nextPos == null && currentLayout.pageCount < MAX_PAGES) {
                    targetPage = currentLayout.pageCount
                    val expandedLayout = currentLayout.copy(pageCount = currentLayout.pageCount + 1)
                    settingsRepository.saveHomeLayout(expandedLayout)
                    nextPos = Pair(0, 0)
                }

                if (nextPos != null) {
                    val widgetItem = HomeItem.Widget(
                        id = UUID.randomUUID().toString(),
                        appWidgetId = appWidgetId,
                        packageName = providerInfo.provider.packageName,
                        providerClassName = providerInfo.provider.className,
                        page = targetPage,
                        row = nextPos.first,
                        column = nextPos.second,
                        rowSpan = widgetHeightCells,
                        columnSpan = widgetWidthCells
                    )
                    val newItems = _homeLayoutState.value.items + widgetItem
                    settingsRepository.saveHomeLayout(_homeLayoutState.value.copy(items = newItems))

                    val options = Bundle().apply {
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, providerInfo.minWidth)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, providerInfo.minWidth)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, providerInfo.minHeight)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, providerInfo.minHeight)
                    }
                    appWidgetManager.updateAppWidgetOptions(appWidgetId, options)
                    settingsRepository.triggerHomeLayoutRefresh()

                    _currentPage.value = targetPage
                } else {
                    snackbarManager.show("No space available for widget on any home screen page.")
                    appWidgetHost.deleteAppWidgetId(appWidgetId)
                }
            } catch (e: Exception) {
                Log.e("WidgetDebug", "Error adding widget to layout", e)
                snackbarManager.show("Failed to add widget: ${e.message}")
                try {
                    appWidgetHost.deleteAppWidgetId(appWidgetId)
                } catch (e2: Exception) {
                    Log.e("WidgetDebug", "Error cleaning up widget ID", e2)
                }
            }
        }
    }

    private fun checkResizeValidity(layout: HomeLayout, widgetToResize: HomeItem.Widget, newRowSpan: Int, newColSpan: Int): Boolean {
        val targetRow = widgetToResize.row
        val targetCol = widgetToResize.column

        if (targetRow + newRowSpan > layout.rows || targetCol + newColSpan > layout.columns) return false

        for (item in layout.items) {
            if (item.id == widgetToResize.id) continue
            val horizontalOverlap = (item.column < targetCol + newColSpan) && (item.column + item.columnSpan > targetCol)
            val verticalOverlap = (item.row < targetRow + newRowSpan) && (item.row + item.rowSpan > targetRow)
            if (horizontalOverlap && verticalOverlap) return false
        }
        return true
    }

    fun moveItemToPage(item: HomeItem, targetPage: Int) {
        viewModelScope.launch {
            val currentLayout = _homeLayoutState.value

            val newPageCount = if (targetPage >= currentLayout.pageCount) {
                (targetPage + 1).coerceAtMost(MAX_PAGES)
            } else {
                currentLayout.pageCount
            }

            if (targetPage >= newPageCount) {
                snackbarManager.show("Cannot move: maximum pages reached")
                return@launch
            }

            val workingLayout = currentLayout.copy(pageCount = newPageCount)
            val nextPos = findNextAvailableGridPosition(
                workingLayout,
                item.columnSpan,
                item.rowSpan,
                targetPage
            )

            if (nextPos == null) {
                snackbarManager.show("No space available on page ${targetPage + 1}")
                return@launch
            }

            val updatedItems = currentLayout.items.map { existingItem ->
                if (existingItem.id == item.id) {
                    when (existingItem) {
                        is HomeItem.App -> existingItem.copy(
                            page = targetPage,
                            row = nextPos.first,
                            column = nextPos.second
                        )
                        is HomeItem.Widget -> existingItem.copy(
                            page = targetPage,
                            row = nextPos.first,
                            column = nextPos.second
                        )
                    }
                } else existingItem
            }

            val newLayout = workingLayout.copy(items = updatedItems)
            settingsRepository.saveHomeLayout(newLayout)
            _currentPage.value = targetPage
        }
    }

    fun renameApp(app: AppModel, newName: String) {
        viewModelScope.launch {
            val appKey = app.getKey()
            if (newName.isBlank() || newName == app.appLabel) {
                settingsRepository.removeAppCustomName(appKey)
            } else {
                settingsRepository.setAppCustomName(appKey, newName)
            }
            loadApps()
        }
    }

    fun removeWidget(widgetItem: HomeItem.Widget) {
        viewModelScope.launch {
            try {
                val currentLayout = _homeLayoutState.value
                val newItems = currentLayout.items.filterNot { it.id == widgetItem.id }
                val newLayout = currentLayout.copy(items = newItems)
                _homeLayoutState.value = newLayout
                appWidgetHost.deleteAppWidgetId(widgetItem.appWidgetId)
                settingsRepository.saveHomeLayout(newLayout)
                settingsRepository.triggerHomeLayoutRefresh()
            } catch (e: Exception) {
                Log.e("ViewModelWidget", "Error deleting widget ID ${widgetItem.appWidgetId}", e)
                snackbarManager.show("Failed to remove widget.")
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
                    emitEvent(UiEvent.StartActivityForResult(configIntent, REQUEST_CODE_CONFIGURE_WIDGET))
                } catch (e: Exception) {
                    Log.e("ViewModelWidget", "Error requesting reconfigure for ${widgetItem.appWidgetId}", e)
                    snackbarManager.show("Failed to reconfigure widget.")
                }
            } else {
                snackbarManager.show("This widget cannot be reconfigured.")
            }
        }
    }

    private fun getAppWidgetInfo(packageName: String, className: String): android.appwidget.AppWidgetProviderInfo? {
        return appWidgetManager.installedProviders.find {
            it.provider.packageName == packageName && it.provider.className == className
        }
    }

    fun moveApp(appItem: HomeItem.App, newRow: Int, newColumn: Int) {
        viewModelScope.launch {
            val currentLayout = _homeLayoutState.value

            if (!validateAndReport(
                    currentLayout,
                    appItem.id,
                    appItem.page,
                    newRow,
                    newColumn,
                    appItem.rowSpan,
                    appItem.columnSpan,
                    "move app"
                )) return@launch

            val updatedItems = currentLayout.items.map { item ->
                if (item.id == appItem.id && item is HomeItem.App) {
                    item.copy(row = newRow, column = newColumn)
                } else item
            }

            settingsRepository.saveHomeLayout(currentLayout.copy(items = updatedItems))
        }
    }

    fun resizeWidget(widgetItem: HomeItem.Widget, newRowSpan: Int, newColSpan: Int) {
        viewModelScope.launch {
            val currentLayout = _homeLayoutState.value

            if (!validateAndReport(
                    currentLayout,
                    widgetItem.id,
                    widgetItem.page,
                    widgetItem.row,
                    widgetItem.column,
                    newRowSpan,
                    newColSpan,
                    "resize widget"
                )) return@launch

            val newItems = currentLayout.items.map {
                if (it.id == widgetItem.id && it is HomeItem.Widget) {
                    it.copy(rowSpan = newRowSpan, columnSpan = newColSpan)
                } else it
            }

            val newLayout = currentLayout.copy(items = newItems)
            _homeLayoutState.value = newLayout
            settingsRepository.saveHomeLayout(newLayout)
            settingsRepository.triggerHomeLayoutRefresh()

            // Update options
            val screenWidthDp = getScreenDimensions(context = appContext).first
            val screenHeightDp = getScreenDimensions(appContext).second
            val cellWidthDp = screenWidthDp.toFloat() / currentLayout.columns
            val cellHeightDp = screenHeightDp.toFloat() / currentLayout.rows

            val options = Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, (newColSpan * cellWidthDp).toInt())
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, (newColSpan * cellWidthDp).toInt())
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, (newRowSpan * cellHeightDp).toInt())
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, (newRowSpan * cellHeightDp).toInt())
            }

            try {
                appWidgetManager.updateAppWidgetOptions(widgetItem.appWidgetId, options)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to update widget options", e)
            }
        }
    }

    fun resizeApp(appItem: HomeItem.App, newRowSpan: Int, newColSpan: Int) {
        viewModelScope.launch {
            val currentLayout = _homeLayoutState.value

            if (!validateAndReport(
                    currentLayout,
                    appItem.id,
                    appItem.page,
                    appItem.row,
                    appItem.column,
                    newRowSpan,
                    newColSpan,
                    "resize app"
                )) return@launch

            val updatedItems = currentLayout.items.map { item ->
                if (item.id == appItem.id && item is HomeItem.App) {
                    item.copy(rowSpan = newRowSpan, columnSpan = newColSpan)
                } else item
            }

            val newLayout = currentLayout.copy(items = updatedItems)
            _homeLayoutState.value = newLayout
            settingsRepository.saveHomeLayout(newLayout)
            settingsRepository.triggerHomeLayoutRefresh()
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_CONFIGURE_WIDGET) {
            val widgetId = pendingWidgetInfo?.appWidgetId
                ?: data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                ?: AppWidgetManager.INVALID_APPWIDGET_ID

            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                if (resultCode == RESULT_OK) {
                    pendingWidgetInfo?.let { info ->
                        if(info.appWidgetId == widgetId) {
                            addWidgetToLayout(info.appWidgetId, info.providerInfo)
                        }
                    } ?: run {
                        Log.d("ViewModelWidget", "Widget ID $widgetId reconfigured successfully.")
                        viewModelScope.launch {
                            val currentLayout = _homeLayoutState.value
                            _homeLayoutState.value = currentLayout.copy()
                            settingsRepository.triggerHomeLayoutRefresh()
                        }
                    }
                } else {
                    Log.w("ViewModelWidget", "Widget configuration cancelled/failed for ID $widgetId")
                    appWidgetHost.deleteAppWidgetId(widgetId)
                    snackbarManager.show("Widget configuration cancelled.")
                }
            }
            pendingWidgetInfo = null
        }
    }

    fun updatePrivateSpaceState() {
        viewModelScope.launch {
            try {
                if (!privateSpaceHelper.isPrivateSpaceSupported()) {
                    _privateSpaceState.value = PrivateSpaceState.Unsupported
                    return@launch
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    val isSetUp = privateSpaceHelper.isPrivateSpaceSetUp()
                    if (!isSetUp) {
                        _privateSpaceState.value = PrivateSpaceState.NotSetUp
                        return@launch
                    }
                    val isLocked = privateSpaceHelper.isPrivateSpaceLocked()
                    _privateSpaceState.value = if (isLocked) PrivateSpaceState.Locked else PrivateSpaceState.Unlocked
                } else {
                    _privateSpaceState.value = PrivateSpaceState.Unsupported
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error updating Private Space state", e)
                _privateSpaceState.value = PrivateSpaceState.NotSetUp
            }
        }
    }

    fun togglePrivateSpace() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            privateSpaceHelper.togglePrivateSpaceLock(
                onSuccess = {
                    updatePrivateSpaceState()
                    loadApps()
                    _refreshTrigger.value++
                },
                onFailure = { message -> snackbarManager.show(message) }
            )
        } else {
            snackbarManager.show("Private Space requires Android 15 or higher")
        }
    }

    fun isAppInPrivateSpace(app: AppModel): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return privateSpaceHelper.isPrivateSpaceProfile(app.user)
        }
        return false
    }

    fun firstOpen(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFirstOpen(value)
        }
    }

    fun loadApps() {
        viewModelScope.launch {
            try {
                _appDrawerState.value = _appDrawerState.value.copy(isLoading = true)
                updatePrivateSpaceState()
                appRepository.loadApps()
                // Rebuild alias index after loading apps (in case we missed the combine for some reason)
                rebuildSearchAliasIndex()
            } catch (e: Exception) {
                snackbarManager.show("Failed to load apps: ${e.message}")
                _appDrawerState.value = _appDrawerState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun getHiddenApps() {
        viewModelScope.launch {
            try {
                _appDrawerState.value = _appDrawerState.value.copy(isLoading = true)
                appRepository.loadHiddenApps()
                _appDrawerState.value = _appDrawerState.value.copy(isLoading = false)
            } catch (e: Exception) {
                snackbarManager.show("Failed to load hidden apps: ${e.message}")
                _appDrawerState.value = _appDrawerState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun toggleAppHidden(app: AppModel) {
        viewModelScope.launch {
            try {
                appRepository.toggleAppHidden(app)
            } catch (e: Exception) {
                snackbarManager.show("Failed to toggle app visibility: ${e.message}")
            }
        }
    }

    fun launchApp(app: AppModel) {
        viewModelScope.launch {
            try {
                appRepository.launchApp(app)
                settingsRepository.updateAppLaunchTime(app.getKey())
            } catch (e: Exception) {
                snackbarManager.show("Failed to launch app: ${e.message}")
            }
        }
    }

    fun selectedApp(appModel: AppModel, flag: Int) {
        when (flag) {
            Constants.FLAG_LAUNCH_APP, Constants.FLAG_HIDDEN_APPS -> launchApp(appModel)
            Constants.FLAG_SET_SWIPE_LEFT_APP -> setSwipeLeftApp(appModel)
            Constants.FLAG_SET_SWIPE_RIGHT_APP -> setSwipeRightApp(appModel)
            Constants.FLAG_SET_SWIPE_UP_APP -> setSwipeUpApp(appModel)
            Constants.FLAG_SET_SWIPE_DOWN_APP -> setSwipeDownApp(appModel)
        }
    }

    private fun setSwipeLeftApp(app: AppModel) {
        viewModelScope.launch {
            val appPreference = AppPreference(
                label = app.appLabel,
                packageName = app.appPackage,
                activityClassName = app.activityClassName,
                userString = app.user.toString()
            )
            settingsRepository.setSwipeLeftApp(appPreference)
        }
    }

    private fun setSwipeRightApp(app: AppModel) {
        viewModelScope.launch {
            val appPreference = AppPreference(
                label = app.appLabel,
                packageName = app.appPackage,
                activityClassName = app.activityClassName,
                userString = app.user.toString()
            )
            settingsRepository.setSwipeRightApp(appPreference)
        }
    }

    fun launchSwipeUpApp() {
        viewModelScope.launch {
            val swipeUpApp = settingsRepository.settings.first().swipeUpApp
            if (swipeUpApp.packageName.isNotEmpty()) {
                val app = AppModel(
                    appLabel = swipeUpApp.label,
                    key = null,
                    appPackage = swipeUpApp.packageName,
                    activityClassName = swipeUpApp.activityClassName,
                    user = getUserHandleFromString(appContext, swipeUpApp.userString)
                )
                launchApp(app)
            }
        }
    }

    fun launchSwipeDownApp() {
        viewModelScope.launch {
            val swipeDownApp = settingsRepository.settings.first().swipeDownApp
            if (swipeDownApp.packageName.isNotEmpty()) {
                val app = AppModel(
                    appLabel = swipeDownApp.label,
                    key = null,
                    appPackage = swipeDownApp.packageName,
                    activityClassName = swipeDownApp.activityClassName,
                    user = getUserHandleFromString(appContext, swipeDownApp.userString)
                )
                launchApp(app)
            }
        }
    }

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

    private fun setSwipeUpApp(app: AppModel) {
        viewModelScope.launch {
            settingsRepository.setSwipeUpApp(
                AppPreference(
                    label = app.appLabel,
                    packageName = app.appPackage,
                    activityClassName = app.activityClassName,
                    userString = app.user.toString()
                )
            )
        }
    }

    private fun setSwipeDownApp(app: AppModel) {
        viewModelScope.launch {
            settingsRepository.setSwipeDownApp(
                AppPreference(
                    label = app.appLabel,
                    packageName = app.appPackage,
                    activityClassName = app.activityClassName,
                    userString = app.user.toString()
                )
            )
        }
    }

    fun setCurrentPage(page: Int) {
        val layout = _homeLayoutState.value
        if (page in 0 until layout.pageCount) {
            _currentPage.value = page
        }
    }

    fun willPageChangeAffectItems(newPageCount: Int): Boolean {
        val currentLayout = _homeLayoutState.value
        if (newPageCount >= currentLayout.pageCount) return false

        return currentLayout.items.any { it.page >= newPageCount }
    }

    fun updatePageCount(newPageCount: Int) {
        viewModelScope.launch {
            val currentLayout = _homeLayoutState.value
            val clampedCount = newPageCount.coerceIn(1, MAX_PAGES)

            if (clampedCount < currentLayout.pageCount) {
                val itemsToRelocate = currentLayout.items.filter { it.page >= clampedCount }

                if (itemsToRelocate.isNotEmpty()) {
                    var workingLayout = currentLayout.copy(pageCount = clampedCount)
                    val relocatedItems = mutableListOf<HomeItem>()
                    val removedWidgetIds = mutableListOf<Int>()

                    val validItems = currentLayout.items.filter { it.page < clampedCount }.toMutableList()

                    for (item in itemsToRelocate) {
                        var placed = false

                        for (targetPage in (clampedCount - 1) downTo 0) {
                            val tempLayout = workingLayout.copy(items = validItems + relocatedItems)
                            val newPos = findNextAvailableGridPosition(
                                tempLayout,
                                item.columnSpan,
                                item.rowSpan,
                                targetPage
                            )

                            if (newPos != null) {
                                val relocatedItem = when (item) {
                                    is HomeItem.App -> item.copy(
                                        page = targetPage,
                                        row = newPos.first,
                                        column = newPos.second
                                    )
                                    is HomeItem.Widget -> item.copy(
                                        page = targetPage,
                                        row = newPos.first,
                                        column = newPos.second
                                    )
                                }
                                relocatedItems.add(relocatedItem)
                                placed = true
                                break
                            }
                        }

                        if (!placed) {
                            if (item is HomeItem.Widget) {
                                removedWidgetIds.add(item.appWidgetId)
                            }
                            snackbarManager.show("Some items could not be relocated and were removed")
                        }
                    }

                    removedWidgetIds.forEach { widgetId ->
                        try {
                            appWidgetHost.deleteAppWidgetId(widgetId)
                        } catch (e: Exception) {
                            Log.e("MainViewModel", "Error deleting widget ID $widgetId", e)
                        }
                    }

                    val newLayout = workingLayout.copy(items = validItems + relocatedItems)
                    settingsRepository.saveHomeLayout(newLayout)
                } else {
                    settingsRepository.saveHomeLayout(currentLayout.copy(pageCount = clampedCount))
                }
            } else {
                settingsRepository.saveHomeLayout(currentLayout.copy(pageCount = clampedCount))
            }

            if (_currentPage.value >= clampedCount) {
                _currentPage.value = clampedCount - 1
            }

            settingsRepository.updateSetting("homeScreenPages", clampedCount)
        }
    }

    fun lockScreen() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            if (settings.doubleTapToLock) {
                val intent = Intent(appContext, MyAccessibilityService::class.java)
                intent.action = "LOCK_SCREEN"
                appContext.startService(intent)
            }
        }
    }

    /**
     * Search apps by query with alias support.
     */
    fun searchApps(query: String) {
        viewModelScope.launch {
            _appDrawerState.value = _appDrawerState.value.copy(searchQuery = query, isLoading = true)
            try {
                val filtered = filterAndRank(query)
                _appDrawerState.value = _appDrawerState.value.copy(
                    filteredApps = filtered,
                    isLoading = false,
                    error = null
                )

                val settings = settingsRepository.settings.first()
                if (filtered.size == 1 && query.isNotEmpty() && settings.autoOpenFilteredApp) {
                    launchApp(filtered[0])
                }
            } catch (e: Exception) {
                snackbarManager.show("Search failed: ${e.message}")
                _appDrawerState.value = _appDrawerState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private suspend fun filterAndRank(query: String): List<AppModel> {
        val settings = settingsRepository.settings.first()
        val listToFilter = if (settings.showHiddenAppsOnSearch) _appListAll.value else _appList.value

        if (query.isBlank()) return listToFilter

        val mode = settings.searchAliasesMode
        val searchType = settings.searchType
        val queryVariants = SearchAliasUtils.buildQueryVariants(query, mode)

        val filtered = listToFilter.filter { app ->
            val label = app.appLabel
            val labelNorm = label.lowercase()

            val direct = when (searchType) {
                Constants.SearchType.FUZZY -> fuzzyMatch(label, query)
                Constants.SearchType.STARTS_WITH -> queryVariants.any { v -> labelNorm.startsWith(v) }
                else -> queryVariants.any { v -> labelNorm.contains(v) }
            }
            if (direct) return@filter true

            val aliases = searchAliasIndex[app.getKey()] ?: emptySet()
            when (searchType) {
                Constants.SearchType.STARTS_WITH -> queryVariants.any { v -> aliases.any { it.startsWith(v) } }
                Constants.SearchType.FUZZY -> queryVariants.any { v -> aliases.any { it.contains(v) } }
                else -> queryVariants.any { v -> aliases.any { it.contains(v) } }
            }
        }

        return filtered
    }

    private suspend fun reapplySearchFilter() {
        val current = _appDrawerState.value
        val newFiltered = filterAndRank(current.searchQuery)
        _appDrawerState.value = current.copy(
            filteredApps = newFiltered,
            isLoading = false,
            error = null
        )
    }

    fun moveWidget(widgetItem: HomeItem.Widget, newRow: Int, newColumn: Int) {
        viewModelScope.launch {
            val currentLayout = _homeLayoutState.value

            if (!validateAndReport(
                    currentLayout,
                    widgetItem.id,
                    widgetItem.page,
                    newRow,
                    newColumn,
                    widgetItem.rowSpan,
                    widgetItem.columnSpan,
                    "move widget"
                )) return@launch

            val updatedItems = currentLayout.items.map { item ->
                if (item.id == widgetItem.id && item is HomeItem.Widget) {
                    item.copy(row = newRow, column = newColumn)
                } else item
            }

            settingsRepository.saveHomeLayout(currentLayout.copy(items = updatedItems))
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

    fun emitEvent(event: UiEvent) {
        viewModelScope.launch { _eventsFlow.emit(event) }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            appContext.unregisterReceiver(appRefreshReceiver)
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error unregistering receiver", e)
        }
    }

    enum class PrivateSpaceState {
        Unsupported,
        NotSetUp,
        Locked,
        Unlocked
    }

    private sealed class PlacementResult {
        object Valid : PlacementResult()
        data class Invalid(val reason: String) : PlacementResult()
    }

    private fun validatePlacement(
        layout: HomeLayout,
        itemId: String,
        page: Int,
        row: Int,
        column: Int,
        rowSpan: Int,
        columnSpan: Int
    ): PlacementResult {
        // Bounds check
        if (row < 0 || column < 0) {
            return PlacementResult.Invalid("Invalid position")
        }

        if (row + rowSpan > layout.rows) {
            return PlacementResult.Invalid("Would go out of bounds vertically")
        }

        if (column + columnSpan > layout.columns) {
            return PlacementResult.Invalid("Would go out of bounds horizontally")
        }

        // Overlap check - only check items on the same page
        val hasOverlap = layout.itemsForPage(page).any { item ->
            if (item.id == itemId) return@any false

            val itemEndRow = item.row + item.rowSpan
            val itemEndCol = item.column + item.columnSpan
            val newEndRow = row + rowSpan
            val newEndCol = column + columnSpan

            // Check if rectangles overlap
            !(row >= itemEndRow || newEndRow <= item.row ||
                    column >= itemEndCol || newEndCol <= item.column)
        }

        return if (hasOverlap) {
            PlacementResult.Invalid("Would overlap with other items")
        } else {
            PlacementResult.Valid
        }
    }

    /**
     * Validates and shows error if invalid, returns true if valid
     */
    private fun validateAndReport(
        layout: HomeLayout,
        itemId: String,
        page: Int,
        row: Int,
        column: Int,
        rowSpan: Int,
        columnSpan: Int,
        actionName: String
    ): Boolean {
        return when (val result = validatePlacement(layout, itemId, page, row, column, rowSpan, columnSpan)) {
            is PlacementResult.Valid -> true
            is PlacementResult.Invalid -> {
                snackbarManager.show("Cannot $actionName: ${result.reason}")
                false
            }
        }
    }
}