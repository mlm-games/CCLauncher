package app.cclauncher.data.repository

import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import app.cclauncher.data.Constants
import app.cclauncher.data.HomeLayout
import app.cclauncher.data.settings.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.catch
import android.view.Gravity
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

// Extension property for Context to access the DataStore instance
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app.cclauncher.settings")

/**
 * Repository for managing application settings
 */
class SettingsRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val settingsManager = SettingsManager()
    private val updater = SettingsUpdater(context.settingsDataStore, json)

    companion object {
        val SHOW_APP_NAMES = booleanPreferencesKey("SHOW_APP_NAMES")
        val SHOW_APP_ICONS = booleanPreferencesKey("SHOW_APP_ICONS")
        val AUTO_SHOW_KEYBOARD = booleanPreferencesKey("AUTO_SHOW_KEYBOARD")
        val SHOW_HIDDEN_APPS_IN_SEARCH = booleanPreferencesKey("SHOW_HIDDEN_APPS_IN_SEARCH")
        val AUTO_OPEN_FILTERED_APP = booleanPreferencesKey("AUTO_OPEN_FILTERED_APP")
        val SEARCH_TYPE = intPreferencesKey("SEARCH_TYPE")
        val APP_THEME = intPreferencesKey("APP_THEME")
        val TEXT_SIZE_SCALE = floatPreferencesKey("TEXT_SIZE_SCALE")
        val FONT_WEIGHT = intPreferencesKey("FONT_WEIGHT")
        val USE_SYSTEM_FONT = booleanPreferencesKey("USE_SYSTEM_FONT")
        val USE_DYNAMIC_THEME = booleanPreferencesKey("USE_DYNAMIC_THEME")
        val ICON_CORNER_RADIUS = intPreferencesKey("ICON_CORNER_RADIUS")
        val ITEM_SPACING = intPreferencesKey("ITEM_SPACING")
        val STATUS_BAR = booleanPreferencesKey("STATUS_BAR")
        val SCREEN_ORIENTATION = intPreferencesKey("SCREEN_ORIENTATION")
        val SHOW_ICONS_IN_LANDSCAPE = booleanPreferencesKey("SHOW_ICONS_IN_LANDSCAPE")
        val SHOW_ICONS_IN_PORTRAIT = booleanPreferencesKey("SHOW_ICONS_IN_PORTRAIT")
        val SWIPE_DOWN_ACTION = intPreferencesKey("SWIPE_DOWN_ACTION")
        val SWIPE_UP_ACTION = intPreferencesKey("SWIPE_UP_ACTION")
        val DOUBLE_TAP_TO_LOCK = booleanPreferencesKey("DOUBLE_TAP_TO_LOCK")
        val FIRST_OPEN = booleanPreferencesKey("FIRST_OPEN")
        val FIRST_OPEN_TIME = longPreferencesKey("FIRST_OPEN_TIME")
        val FIRST_SETTINGS_OPEN = booleanPreferencesKey("FIRST_SETTINGS_OPEN")
        val FIRST_HIDE = booleanPreferencesKey("FIRST_HIDE")
        val USER_STATE = stringPreferencesKey("USER_STATE")
        val LOCK_MODE = booleanPreferencesKey("LOCK_MODE")
        val KEYBOARD_MESSAGE = booleanPreferencesKey("KEYBOARD_MESSAGE")
        val PLAIN_WALLPAPER = booleanPreferencesKey("PLAIN_WALLPAPER")
        val APP_LABEL_ALIGNMENT = intPreferencesKey("APP_LABEL_ALIGNMENT")
        val HIDDEN_APPS = stringSetPreferencesKey("HIDDEN_APPS")
        val HIDDEN_APPS_UPDATED = booleanPreferencesKey("HIDDEN_APPS_UPDATED")
        val SHOW_HINT_COUNTER = intPreferencesKey("SHOW_HINT_COUNTER")
        val ABOUT_CLICKED = booleanPreferencesKey("ABOUT_CLICKED")
        val RATE_CLICKED = booleanPreferencesKey("RATE_CLICKED")
        val SHARE_SHOWN_TIME = longPreferencesKey("SHARE_SHOWN_TIME")
        val SEARCH_RESULTS_USE_HOME_FONT = booleanPreferencesKey("SEARCH_RESULTS_USE_HOME_FONT")
        val SEARCH_RESULTS_FONT_SIZE = floatPreferencesKey("SEARCH_RESULTS_FONT_SIZE")
        val SHOW_HOME_SCREEN_ICONS = booleanPreferencesKey("SHOW_HOME_SCREEN_ICONS")
        val SCALE_HOME_APPS = booleanPreferencesKey("SCALE_HOME_APPS")
        val RENAMED_APPS_JSON = stringPreferencesKey("RENAMED_APPS_JSON")

        val HOME_APPS_JSON = stringPreferencesKey("HOME_APPS_JSON")
        val SWIPE_LEFT_APP_JSON = stringPreferencesKey("SWIPE_LEFT_APP_JSON")
        val SWIPE_RIGHT_APP_JSON = stringPreferencesKey("SWIPE_RIGHT_APP_JSON")
        val SWIPE_UP_APP_JSON = stringPreferencesKey("SWIPE_UP_APP_JSON")
        val SWIPE_DOWN_APP_JSON = stringPreferencesKey("SWIPE_DOWN_APP_JSON")

        val HOME_LAYOUT = stringPreferencesKey("HOME_LAYOUT_JSON")

        val LOCK_SETTINGS = booleanPreferencesKey("LOCK_SETTINGS")
        val SETTINGS_LOCK_PIN = stringPreferencesKey("SETTINGS_LOCK_PIN")

        val SWIPE_LEFT_ACTION = intPreferencesKey("SWIPE_LEFT_ACTION")
        val SWIPE_RIGHT_ACTION = intPreferencesKey("SWIPE_RIGHT_ACTION")

        val HOME_SCREEN_ROWS = intPreferencesKey("HOME_SCREEN_ROWS")
        val HOME_SCREEN_COLUMNS = intPreferencesKey("HOME_SCREEN_COLUMNS")

        val SELECTED_ICON_PACK = stringPreferencesKey("SELECTED_ICON_PACK")

        val SEARCH_SORT_ORDER = intPreferencesKey("SEARCH_SORT_ORDER")

        val RECENT_APP_HISTORY = stringPreferencesKey("RECENT_APP_HISTORY")

        val CUSTOM_FONT_PATH = stringPreferencesKey("CUSTOM_FONT_PATH")


    }

    private val defaultAppSettings = AppSettings.getDefault()
    private val defaultHomeApps: List<HomeAppPreference> = defaultAppSettings.homeApps
    private val defaultSwipeLeftApp: AppPreference = defaultAppSettings.swipeLeftApp
    private val defaultSwipeRightApp: AppPreference = defaultAppSettings.swipeRightApp
    private val defaultSwipeUpApp: AppPreference = defaultAppSettings.swipeUpApp
    private val defaultSwipeDownApp: AppPreference = defaultAppSettings.swipeDownApp

    /**
     * Flow of settings that emits whenever any setting changes
     */
    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->

        val homeApps = prefs[HOME_APPS_JSON]?.let {
            json.decodeFromStringCatching(it, defaultAppSettings.homeApps)
        } ?: defaultAppSettings.homeApps

        val swipeLeftApp = prefs[SWIPE_LEFT_APP_JSON]?.let {
            json.decodeFromStringCatching(it, defaultSwipeLeftApp)
        } ?: defaultSwipeLeftApp

        val swipeRightApp = prefs[SWIPE_RIGHT_APP_JSON]?.let {
            json.decodeFromStringCatching(it, defaultSwipeRightApp)
        } ?: defaultSwipeRightApp

        val swipeUpApp = prefs[SWIPE_UP_APP_JSON]?.let {
            json.decodeFromStringCatching(it, defaultSwipeUpApp)
        } ?: defaultSwipeUpApp

        val swipeDownApp = prefs[SWIPE_DOWN_APP_JSON]?.let {
            json.decodeFromStringCatching(it, defaultSwipeDownApp)
        } ?: defaultSwipeDownApp

        val renamedApps = prefs[RENAMED_APPS_JSON]?.let {
            try {
                json.decodeFromString<Map<String, String>>(it)
            } catch (e: Exception) {
                Log.e("SettingsRepo", "Failed to decode renamed apps JSON: ${e.message}")
                mapOf<String, String>()
            }
        } ?: mapOf()

        val recentAppHistory = prefs[RECENT_APP_HISTORY]?.let {
            try {
                json.decodeFromString<Map<String, Long>>(it)
            } catch (e: Exception) {
                Log.e("SettingsRepo", "Failed to decode recent app history JSON: ${e.message}")
                mapOf<String, Long>()
            }
        } ?: mapOf()

        AppSettings(
            // General settings
            showAppNames = prefs[SHOW_APP_NAMES] ?: false,
            showAppIcons = prefs[SHOW_APP_ICONS] ?: true,
            autoShowKeyboard = prefs[AUTO_SHOW_KEYBOARD] ?: true,
            showHiddenAppsOnSearch = prefs[SHOW_HIDDEN_APPS_IN_SEARCH] ?: false,
            autoOpenFilteredApp = prefs[AUTO_OPEN_FILTERED_APP] ?: true,
            searchType = prefs[SEARCH_TYPE] ?: Constants.SearchType.CONTAINS,
            searchSortOrder = prefs[SEARCH_SORT_ORDER] ?: Constants.SortOrder.ALPHABETICAL,

            // Appearance settings
            appTheme = prefs[APP_THEME] ?: AppCompatDelegate.MODE_NIGHT_YES,
            textSizeScale = prefs[TEXT_SIZE_SCALE] ?: 1.0f,
            fontWeight = prefs[FONT_WEIGHT] ?: 2,
            useSystemFont = prefs[USE_SYSTEM_FONT] ?: true,
            useDynamicTheme = prefs[USE_DYNAMIC_THEME] ?: false,
            iconCornerRadius = prefs[ICON_CORNER_RADIUS] ?: 0,
            itemSpacing = prefs[ITEM_SPACING] ?: 1,
            customFontPath = prefs[CUSTOM_FONT_PATH] ?: "",

            // Layout settings
            statusBar = prefs[STATUS_BAR] ?: false,
            screenOrientation = prefs[SCREEN_ORIENTATION] ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            showHomeScreenIcons = prefs[SHOW_HOME_SCREEN_ICONS] ?: false,
            showIconsInLandscape = prefs[SHOW_ICONS_IN_LANDSCAPE] ?: false,
            showIconsInPortrait = prefs[SHOW_ICONS_IN_PORTRAIT] ?: false,
            scaleHomeApps = prefs[SCALE_HOME_APPS] ?: true,
            homeScreenRows = prefs[HOME_SCREEN_ROWS] ?: 8,
            homeScreenColumns = prefs[HOME_SCREEN_COLUMNS] ?: 4,

            // Gestures settings
            swipeDownAction = prefs[SWIPE_DOWN_ACTION] ?: Constants.SwipeAction.NOTIFICATIONS,
            swipeUpAction = prefs[SWIPE_UP_ACTION] ?: Constants.SwipeAction.SEARCH,
            doubleTapToLock = prefs[DOUBLE_TAP_TO_LOCK] ?: false,
            swipeLeftAction = prefs[SWIPE_LEFT_ACTION] ?: Constants.SwipeAction.NULL,
            swipeRightAction = prefs[SWIPE_RIGHT_ACTION] ?: Constants.SwipeAction.NULL,

            lockSettings = prefs[LOCK_SETTINGS] ?: false,
            settingsLockPin = prefs[SETTINGS_LOCK_PIN] ?: "",

            // Other properties
            firstOpen = prefs[FIRST_OPEN] ?: true,
            firstOpenTime = prefs[FIRST_OPEN_TIME] ?: 0L,
            firstSettingsOpen = prefs[FIRST_SETTINGS_OPEN] ?: true,
            firstHide = prefs[FIRST_HIDE] ?: true,
            userState = prefs[USER_STATE] ?: Constants.UserState.START,
            lockMode = prefs[LOCK_MODE] ?: false,
            keyboardMessage = prefs[KEYBOARD_MESSAGE] ?: false,
            plainWallpaper = prefs[PLAIN_WALLPAPER] ?: false,
            appLabelAlignment = prefs[APP_LABEL_ALIGNMENT] ?: Gravity.START,
            hiddenApps = prefs[HIDDEN_APPS] ?: emptySet(),
            hiddenAppsUpdated = prefs[HIDDEN_APPS_UPDATED] ?: false,
            showHintCounter = prefs[SHOW_HINT_COUNTER] ?: 1,
            aboutClicked = prefs[ABOUT_CLICKED] ?: false,
            rateClicked = prefs[RATE_CLICKED] ?: false,
            shareShownTime = prefs[SHARE_SHOWN_TIME] ?: 0L,
            searchResultsUseHomeFont = prefs[SEARCH_RESULTS_USE_HOME_FONT] ?: false,
            searchResultsFontSize = prefs[SEARCH_RESULTS_FONT_SIZE] ?: 1.0f,
            selectedIconPack = prefs[SELECTED_ICON_PACK] ?: "default",

            homeApps = homeApps,
            swipeLeftApp = swipeLeftApp,
            swipeRightApp = swipeRightApp,
            swipeUpApp = swipeUpApp,
            swipeDownApp = swipeDownApp,
            renamedApps = renamedApps,
            recentAppHistory = recentAppHistory
        )
    }

    /**
     * Simplified update methods using the new updater
     */
    suspend fun setShowAppNames(value: Boolean) = updater.updateBoolean(SHOW_APP_NAMES, value)
    suspend fun setShowAppIcons(value: Boolean) = updater.updateBoolean(SHOW_APP_ICONS, value)
    suspend fun setAutoShowKeyboard(value: Boolean) = updater.updateBoolean(AUTO_SHOW_KEYBOARD, value)
    suspend fun setSearchType(value: Int) = updater.updateInt(SEARCH_TYPE, value)
    suspend fun setAppTheme(value: Int) = updater.updateInt(APP_THEME, value)
    suspend fun setTextSizeScale(value: Float) = updater.updateFloat(TEXT_SIZE_SCALE, value)
    suspend fun setFontWeight(value: Int) = updater.updateInt(FONT_WEIGHT, value)
    suspend fun setFirstOpen(value: Boolean) = updater.updateBoolean(FIRST_OPEN, value)
    suspend fun setDoubleTapToLock(value: Boolean) = updater.updateBoolean(DOUBLE_TAP_TO_LOCK, value)
    suspend fun setStatusBar(value: Boolean) = updater.updateBoolean(STATUS_BAR, value)
    suspend fun setScreenOrientation(value: Int) = updater.updateInt(SCREEN_ORIENTATION, value)
    suspend fun setHomeScreenRows(value: Int) = updater.updateInt(HOME_SCREEN_ROWS, value)
    suspend fun setHomeScreenColumns(value: Int) = updater.updateInt(HOME_SCREEN_COLUMNS, value)
    suspend fun setSelectedIconPack(value: String) = updater.updateString(SELECTED_ICON_PACK, value)
    suspend fun setSettingsLock(locked: Boolean) = updater.updateBoolean(LOCK_SETTINGS, locked)
    suspend fun setSettingsLockPin(pin: String) = updater.updateString(SETTINGS_LOCK_PIN, pin)

    /**
     * Generic update method for when property name is dynamic
     */
    suspend fun updateSetting(propertyName: String, value: Any) {
        when (propertyName) {
            "showAppNames" -> setShowAppNames(value as Boolean)
            "showAppIcons" -> setShowAppIcons(value as Boolean)
            "autoShowKeyboard" -> setAutoShowKeyboard(value as Boolean)
            "searchType" -> setSearchType(value as Int)
            "appTheme" -> setAppTheme(value as Int)
            "textSizeScale" -> setTextSizeScale(value as Float)
            "fontWeight" -> setFontWeight(value as Int)
            "statusBar" -> setStatusBar(value as Boolean)
            "screenOrientation" -> setScreenOrientation(value as Int)
            "homeScreenRows" -> setHomeScreenRows(value as Int)
            "homeScreenColumns" -> setHomeScreenColumns(value as Int)
            "selectedIconPack" -> setSelectedIconPack(value as String)
            "doubleTapToLock" -> setDoubleTapToLock(value as Boolean)
            // Need to add other mappings (skip for hybrid)
            else -> Log.w("SettingsRepo", "Unknown setting: $propertyName")
        }
    }

    /**
     * Complex update using lambda (replacing the old reflection-based method)
     */
    suspend fun updateSetting(update: (AppSettings) -> AppSettings) {
        val currentSettings = settings.first()
        val updatedSettings = update(currentSettings)

        // Use batch update for efficiency
        updater.updateMultiple { prefs ->
            // Only update changed values
            if (currentSettings.showAppNames != updatedSettings.showAppNames) {
                prefs[SHOW_APP_NAMES] = updatedSettings.showAppNames
            }
            if (currentSettings.autoShowKeyboard != updatedSettings.autoShowKeyboard) {
                prefs[AUTO_SHOW_KEYBOARD] = updatedSettings.autoShowKeyboard
            }
            if (currentSettings.searchType != updatedSettings.searchType) {
                prefs[SEARCH_TYPE] = updatedSettings.searchType
            }
            if (currentSettings.appTheme != updatedSettings.appTheme) {
                prefs[APP_THEME] = updatedSettings.appTheme
            }
            if (currentSettings.textSizeScale != updatedSettings.textSizeScale) {
                prefs[TEXT_SIZE_SCALE] = updatedSettings.textSizeScale
            }
            if (currentSettings.fontWeight != updatedSettings.fontWeight) {
                prefs[FONT_WEIGHT] = updatedSettings.fontWeight
            }
            if (currentSettings.statusBar != updatedSettings.statusBar) {
                prefs[STATUS_BAR] = updatedSettings.statusBar
            }
            if (currentSettings.doubleTapToLock != updatedSettings.doubleTapToLock) {
                prefs[DOUBLE_TAP_TO_LOCK] = updatedSettings.doubleTapToLock
            }
            // Add other properties (skip for hybrid)

            // Handle complex types
            if (currentSettings.hiddenApps != updatedSettings.hiddenApps) {
                prefs[HIDDEN_APPS] = updatedSettings.hiddenApps
            }
            if (currentSettings.homeApps != updatedSettings.homeApps) {
                prefs[HOME_APPS_JSON] = json.encodeToString(updatedSettings.homeApps)
            }
            if (currentSettings.renamedApps != updatedSettings.renamedApps) {
                prefs[RENAMED_APPS_JSON] = json.encodeToString(updatedSettings.renamedApps)
            }
        }
    }

    fun getHomeLayout(): Flow<HomeLayout> = context.settingsDataStore.data
        .map { prefs ->
            prefs[HOME_LAYOUT]?.let { jsonString ->
                try {
                    Json.decodeFromString<HomeLayout>(jsonString)
                } catch (e: Exception) {
                    Log.e("SettingsRepo", "Failed to decode HomeLayout JSON", e)
                    HomeLayout()
                }
            } ?: HomeLayout()
        }
        .catch { exception ->
            Log.e("SettingsRepo", "Error reading HomeLayout", exception)
            emit(HomeLayout())
        }

    suspend fun saveHomeLayout(layout: HomeLayout) {
        updater.updateJson(HOME_LAYOUT, layout)
    }

    suspend fun triggerHomeLayoutRefresh() {
        val currentLayout = getHomeLayout().first()
        saveHomeLayout(currentLayout)
    }

    suspend fun setHomeApp(position: Int, app: HomeAppPreference) {
        updateSetting { currentSettings ->
            val newHomeApps = currentSettings.homeApps.toMutableList()
            if (position in newHomeApps.indices) {
                newHomeApps[position] = app
            }
            currentSettings.copy(homeApps = newHomeApps)
        }
    }

    suspend fun setSwipeLeftApp(app: AppPreference) = updater.updateJson(SWIPE_LEFT_APP_JSON, app)
    suspend fun setSwipeRightApp(app: AppPreference) = updater.updateJson(SWIPE_RIGHT_APP_JSON, app)
    suspend fun setSwipeUpApp(app: AppPreference) = updater.updateJson(SWIPE_UP_APP_JSON, app)
    suspend fun setSwipeDownApp(app: AppPreference) = updater.updateJson(SWIPE_DOWN_APP_JSON, app)

    suspend fun getSwipeLeftApp(): AppPreference = settings.first().swipeLeftApp
    suspend fun getSwipeRightApp(): AppPreference = settings.first().swipeRightApp

    suspend fun validateSettingsPin(pin: String): Boolean = settings.first().settingsLockPin == pin

    suspend fun setCustomFont(uri: Uri) {
        try {
            val fontFile = File(context.filesDir, Constants.CUSTOM_FONT_FILENAME)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(fontFile).use { output ->
                    input.copyTo(output)
                }
            }
            updater.updateString(CUSTOM_FONT_PATH, fontFile.absolutePath)
        } catch (e: Exception) {
            Log.e("SettingsRepo", "Failed to copy font file", e)
        }
    }

    suspend fun clearCustomFont() {
        val currentPath = settings.first().customFontPath
        if (currentPath.isNotEmpty()) {
            try {
                File(currentPath).delete()
            } catch (e: Exception) {
                Log.e("SettingsRepo", "Error deleting old font file", e)
            }
        }
        updater.updateString(CUSTOM_FONT_PATH, "")
    }

    suspend fun toggleAppHidden(packageKey: String) {
        updateSetting {
            val updatedHiddenApps = it.hiddenApps.toMutableSet()
            if (updatedHiddenApps.contains(packageKey)) {
                updatedHiddenApps.remove(packageKey)
            } else {
                updatedHiddenApps.add(packageKey)
            }
            it.copy(hiddenApps = updatedHiddenApps)
        }
    }

    suspend fun setAppCustomName(appKey: String, customName: String) {
        val currentSettings = settings.first()
        val updatedRenamedApps = currentSettings.renamedApps.toMutableMap()

        if (customName.isBlank()) {
            updatedRenamedApps.remove(appKey)
        } else {
            updatedRenamedApps[appKey] = customName
        }

        updater.updateJson(RENAMED_APPS_JSON, updatedRenamedApps)
    }

    suspend fun removeAppCustomName(appKey: String) {
        val currentSettings = settings.first()
        val updatedRenamedApps = currentSettings.renamedApps.toMutableMap()
        updatedRenamedApps.remove(appKey)
        updater.updateJson(RENAMED_APPS_JSON, updatedRenamedApps)
    }

    suspend fun updateAppLaunchTime(appKey: String) {
        val currentSettings = settings.first()
        val updatedHistory = currentSettings.recentAppHistory.toMutableMap()
        updatedHistory[appKey] = System.currentTimeMillis()

        if (updatedHistory.size > 100) {
            val oldest = updatedHistory.entries.sortedBy { it.value }.take(20)
            oldest.forEach { updatedHistory.remove(it.key) }
        }

        updater.updateJson(RECENT_APP_HISTORY, updatedHistory)
    }

    private inline fun <reified T> Json.decodeFromStringCatching(jsonString: String, default: T): T {
        return try {
            this.decodeFromString<T>(jsonString)
        } catch (e: Exception) {
            Log.e("SettingsRepo", "Failed to decode JSON for ${T::class.simpleName}: ${e.message}. Using default.")
            default
        }
    }
}