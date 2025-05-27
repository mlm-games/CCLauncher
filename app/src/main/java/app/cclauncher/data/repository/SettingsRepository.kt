package app.cclauncher.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import app.cclauncher.data.Constants
import app.cclauncher.data.settings.AppSettings
import app.cclauncher.data.settings.SettingsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import android.view.Gravity
import androidx.appcompat.app.AppCompatDelegate
import kotlin.reflect.full.memberProperties
import app.cclauncher.data.HomeLayout
import app.cclauncher.data.settings.AppPreference
import app.cclauncher.data.settings.HomeAppPreference
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.catch

// Extension property for Context to access the DataStore instance
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app.cclauncher.settings")

/**
 * Repository for managing application settings
 */
@Suppress("NullableBooleanElvis")
class SettingsRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false } // Configure Json instance

    private val settingsManager = SettingsManager()

    companion object {
        // Define all preference keys
        val HOME_APPS_NUM = intPreferencesKey("HOME_APPS_NUM")
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
        val HOME_ALIGNMENT = intPreferencesKey("HOME_ALIGNMENT")
        val HOME_BOTTOM_ALIGNMENT = booleanPreferencesKey("HOME_BOTTOM_ALIGNMENT")
        val STATUS_BAR = booleanPreferencesKey("STATUS_BAR")
        val DATE_TIME_VISIBILITY = intPreferencesKey("DATE_TIME_VISIBILITY")
        val FORCE_LANDSCAPE_MODE = booleanPreferencesKey("FORCE_LANDSCAPE_MODE")
        val SHOW_ICONS_IN_LANDSCAPE = booleanPreferencesKey("SHOW_ICONS_IN_LANDSCAPE")
        val SHOW_ICONS_IN_PORTRAIT = booleanPreferencesKey("SHOW_ICONS_IN_PORTRAIT")
        val EDIT_HOME_APPS = booleanPreferencesKey("EDIT_HOME_APPS")
        val EDIT_WIDGETS = booleanPreferencesKey("EDIT_WIDGETS")
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
            json.decodeFromStringCatching(it, defaultHomeApps)
        } ?: defaultHomeApps

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

        AppSettings(
            // General settings
            homeAppsNum = prefs[HOME_APPS_NUM] ?: 0,
            showAppNames = prefs[SHOW_APP_NAMES] ?: false,
            showAppIcons = prefs[SHOW_APP_ICONS] ?: true,
            autoShowKeyboard = prefs[AUTO_SHOW_KEYBOARD] ?: true,
            showHiddenAppsOnSearch = prefs[SHOW_HIDDEN_APPS_IN_SEARCH] ?: false,
            autoOpenFilteredApp = prefs[AUTO_OPEN_FILTERED_APP] ?: true,
            searchType = prefs[SEARCH_TYPE] ?: Constants.SearchType.CONTAINS,

            // Appearance settings
            appTheme = prefs[APP_THEME] ?: AppCompatDelegate.MODE_NIGHT_YES,
            textSizeScale = prefs[TEXT_SIZE_SCALE] ?: 1.0f,
            fontWeight = prefs[FONT_WEIGHT] ?: 2,
            useSystemFont = prefs[USE_SYSTEM_FONT] ?: true,
            useDynamicTheme = prefs[USE_DYNAMIC_THEME] ?: false,
            iconCornerRadius = prefs[ICON_CORNER_RADIUS] ?: 25,
            itemSpacing = prefs[ITEM_SPACING] ?: 1,

            // Layout settings
            homeAlignment = prefs[HOME_ALIGNMENT] ?: Gravity.CENTER,
            homeBottomAlignment = prefs[HOME_BOTTOM_ALIGNMENT] ?: false,
            statusBar = prefs[STATUS_BAR] ?: false,
            dateTimeVisibility = prefs[DATE_TIME_VISIBILITY] ?: Constants.DateTime.ON,
            forceLandscapeMode = prefs[FORCE_LANDSCAPE_MODE] ?: false,
            showHomeScreenIcons = prefs[SHOW_HOME_SCREEN_ICONS] ?: false,
            showIconsInLandscape = prefs[SHOW_ICONS_IN_LANDSCAPE] ?: false,
            showIconsInPortrait = prefs[SHOW_ICONS_IN_PORTRAIT] ?: false,
            editHomeApps = prefs[EDIT_HOME_APPS] ?: false,
            editWidgets = prefs[EDIT_WIDGETS] ?: false,
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
            renamedApps = renamedApps
        )
    }

    private inline fun <reified T> Json.decodeFromStringCatching(jsonString: String, default: T): T {
        return try {
            this.decodeFromString<T>(jsonString)
        } catch (e: Exception) {
            Log.e("SettingsRepo", "Failed to decode JSON for ${T::class.simpleName}: ${e.message}. Using default.")
            default
        }
    }


    /**
     * Update a specific setting using reflection
     */
    suspend fun updateSetting(update: (AppSettings) -> AppSettings) {
        val currentSettings = settings.first()
        val updatedSettings = update(currentSettings)

        context.settingsDataStore.edit { prefs ->
            // Use reflection to find changed properties
            AppSettings::class.memberProperties.forEach { property ->
                val name = property.name
                val currentValue = property.get(currentSettings)
                val newValue = property.get(updatedSettings)

                if (currentValue != newValue) {
                    @Suppress("UNCHECKED_CAST")
                    when (name) {
                        // General settings
                        "homeAppsNum" -> prefs[HOME_APPS_NUM] = newValue as Int
                        "showAppNames" -> prefs[SHOW_APP_NAMES] = newValue as Boolean
                        "showAppIcons" -> prefs[SHOW_APP_ICONS] = true // as Boolean
                        "autoShowKeyboard" -> prefs[AUTO_SHOW_KEYBOARD] = newValue as Boolean
                        "showHiddenAppsOnSearch" -> prefs[SHOW_HIDDEN_APPS_IN_SEARCH] = newValue as Boolean
                        "autoOpenFilteredApp" -> prefs[AUTO_OPEN_FILTERED_APP] = newValue as Boolean
                        "searchType" -> prefs[SEARCH_TYPE] = newValue as Int

                        // Appearance settings
                        "appTheme" -> prefs[APP_THEME] = newValue as Int
                        "textSizeScale" -> prefs[TEXT_SIZE_SCALE] = newValue as Float
                        "fontWeight" -> prefs[FONT_WEIGHT] = newValue as Int
                        "useSystemFont" -> prefs[USE_SYSTEM_FONT] = newValue as Boolean
                        "useDynamicTheme" -> prefs[USE_DYNAMIC_THEME] = newValue as Boolean
                        "iconCornerRadius" -> prefs[ICON_CORNER_RADIUS] = newValue as Int
                        "itemSpacing" -> prefs[ITEM_SPACING] = newValue as Int

                        // Layout settings
                        "homeAlignment" -> prefs[HOME_ALIGNMENT] = newValue as Int
                        "homeBottomAlignment" -> prefs[HOME_BOTTOM_ALIGNMENT] = newValue as Boolean
                        "statusBar" -> prefs[STATUS_BAR] = newValue as Boolean
                        "dateTimeVisibility" -> prefs[DATE_TIME_VISIBILITY] = newValue as Int
                        "forceLandscapeMode" -> prefs[FORCE_LANDSCAPE_MODE] = newValue as Boolean
                        "showHomeScreenIcons" -> prefs[SHOW_HOME_SCREEN_ICONS] = newValue as Boolean
                        "showIconsInLandscape" -> prefs[SHOW_ICONS_IN_LANDSCAPE] = newValue as Boolean
                        "showIconsInPortrait" -> prefs[SHOW_ICONS_IN_PORTRAIT] = newValue as Boolean
                        "editHomeApps" -> prefs[EDIT_HOME_APPS] = newValue as Boolean
                        "editWidgets" -> prefs[EDIT_WIDGETS] = newValue as Boolean
                        "scaleHomeApps" -> prefs[SCALE_HOME_APPS] = newValue as Boolean
                        "homeScreenRows" -> prefs[HOME_SCREEN_ROWS] = newValue as Int
                        "homeScreenColumns" -> prefs[HOME_SCREEN_COLUMNS] = newValue as Int

                        // Gestures settings
                        "swipeDownAction" -> prefs[SWIPE_DOWN_ACTION] = newValue as Int
                        "swipeUpAction" -> prefs[SWIPE_UP_ACTION] = newValue as Int
                        "doubleTapToLock" -> prefs[DOUBLE_TAP_TO_LOCK] = newValue as Boolean
                        "swipeLeftAction" -> prefs[SWIPE_LEFT_ACTION] = newValue as Int
                        "swipeRightAction" -> prefs[SWIPE_RIGHT_ACTION] = newValue as Int

                        // Search result appearance
                        "searchResultsUseHomeFont" -> prefs[SEARCH_RESULTS_USE_HOME_FONT] = newValue as Boolean
                        "searchResultsFontSize" -> prefs[SEARCH_RESULTS_FONT_SIZE] = newValue as Float

                        "lockSettings" -> prefs[LOCK_SETTINGS] = newValue as Boolean
                        "settingsLockPin" -> prefs[SETTINGS_LOCK_PIN] = newValue as String
                        "selectedIconPack" -> prefs[SELECTED_ICON_PACK] = newValue as String

                        // Other properties
                        "firstOpen" -> prefs[FIRST_OPEN] = newValue as Boolean
                        "firstOpenTime" -> prefs[FIRST_OPEN_TIME] = newValue as Long
                        "firstSettingsOpen" -> prefs[FIRST_SETTINGS_OPEN] = newValue as Boolean
                        "firstHide" -> prefs[FIRST_HIDE] = newValue as Boolean
                        "userState" -> prefs[USER_STATE] = newValue as String
                        "lockMode" -> prefs[LOCK_MODE] = newValue as Boolean
                        "keyboardMessage" -> prefs[KEYBOARD_MESSAGE] = newValue as Boolean
                        "plainWallpaper" -> prefs[PLAIN_WALLPAPER] = newValue as Boolean
                        "appLabelAlignment" -> prefs[APP_LABEL_ALIGNMENT] = newValue as Int
                        "hiddenAppsUpdated" -> prefs[HIDDEN_APPS_UPDATED] = newValue as Boolean
                        "showHintCounter" -> prefs[SHOW_HINT_COUNTER] = newValue as Int
                        "aboutClicked" -> prefs[ABOUT_CLICKED] = newValue as Boolean
                        "rateClicked" -> prefs[RATE_CLICKED] = newValue as Boolean
                        "shareShownTime" -> prefs[SHARE_SHOWN_TIME] = newValue as Long

                        // Special handling for complex types
                        "hiddenApps" -> prefs[HIDDEN_APPS] = newValue as Set<String>

                        "homeApps" -> prefs[HOME_APPS_JSON] = json.encodeToString(newValue)
                        "swipeLeftApp" -> prefs[SWIPE_LEFT_APP_JSON] = json.encodeToString(newValue)
                        "swipeRightApp" -> prefs[SWIPE_RIGHT_APP_JSON] = json.encodeToString(newValue)
                        "clockApp" -> prefs[SWIPE_UP_APP_JSON] = json.encodeToString(newValue)
                        "calendarApp" -> prefs[SWIPE_DOWN_APP_JSON] = json.encodeToString(newValue)
                        "renamedApps" -> prefs[RENAMED_APPS_JSON] = json.encodeToString(newValue)
                    }
                }
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
                    HomeLayout() // Return default on error
                }
            } ?: HomeLayout() // Return default if key not found
        }
        .catch { exception ->
            Log.e("SettingsRepo", "Error reading HomeLayout", exception)
            emit(HomeLayout()) // Emit default on error
        }

    suspend fun saveHomeLayout(layout: HomeLayout) {
        try {
            val jsonString = Json.encodeToString(layout)
            context.settingsDataStore.edit { prefs ->
                prefs[HOME_LAYOUT] = jsonString
            }
        } catch (e: Exception) {
            Log.e("SettingsRepo", "Failed to encode or save HomeLayout JSON", e)
            // Optionally notify UI of error
        }
    }

    suspend fun triggerHomeLayoutRefresh() {
        // Read the current value and write it back to trigger the flow
        val currentLayout = getHomeLayout().first()
        saveHomeLayout(currentLayout)
    }


    /**
     * Update a setting by property name
     */
    suspend fun updateSetting(propertyName: String, value: Any) {
        val currentSettings = settings.first()
        val updatedSettings = settingsManager.updateSetting(currentSettings, propertyName, value)
        updateSetting { updatedSettings }
    }

    /**
     * Methods for managing home apps
     */
    suspend fun setHomeApp(position: Int, app: HomeAppPreference) {
        updateSetting { currentSettings ->
            // Create a mutable copy since AppSettings.homeApps is immutable.
            val newHomeApps = currentSettings.homeApps.toMutableList()
            if (position in newHomeApps.indices) {
                newHomeApps[position] = app
            }
            currentSettings.copy(homeApps = newHomeApps)
        }
    }

    /**
     * Methods for managing other settable apps
     */
    suspend fun setSwipeLeftApp(app: AppPreference) {
        context.settingsDataStore.edit { prefs ->
            prefs[SWIPE_LEFT_APP_JSON] = json.encodeToString(app)
        }
    }

    suspend fun setSwipeRightApp(app: AppPreference) {
        context.settingsDataStore.edit { prefs ->
            prefs[SWIPE_RIGHT_APP_JSON] = json.encodeToString(app)
        }
    }

    suspend fun setSwipeUpApp(app: AppPreference) {
        context.settingsDataStore.edit { prefs ->
            prefs[SWIPE_UP_APP_JSON] = json.encodeToString(app)
        }
    }

    suspend fun setSwipeDownApp(app: AppPreference) {
        context.settingsDataStore.edit { prefs ->
            prefs[SWIPE_DOWN_APP_JSON] = json.encodeToString(app)
        }
    }

    suspend fun getSwipeLeftApp(): AppPreference {
        return settings.first().swipeLeftApp
    }

    suspend fun getSwipeRightApp(): AppPreference {
        return settings.first().swipeRightApp
    }

    suspend fun setSettingsLock(locked: Boolean) {
        updateSetting { it.copy(lockSettings = locked) }
    }

    suspend fun setSettingsLockPin(pin: String) {
        updateSetting { it.copy(settingsLockPin = pin) }
    }

    suspend fun validateSettingsPin(pin: String): Boolean {
        return settings.first().settingsLockPin == pin
    }

    /**
     * Methods for managing hidden apps
     */
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

    suspend fun setFirstOpen(value: Boolean) {
        updateSetting { it.copy(firstOpen = value) }
    }

    suspend fun setAppTheme(value: Int) {
        updateSetting { it.copy(appTheme = value) }
    }

    suspend fun setAppCustomName(appKey: String, customName: String) {
        val currentSettings = settings.first()
        val updatedRenamedApps = currentSettings.renamedApps.toMutableMap()

        if (customName.isBlank()) {
            updatedRenamedApps.remove(appKey)
        } else {
            updatedRenamedApps[appKey] = customName
        }

        context.settingsDataStore.edit { prefs ->
            prefs[RENAMED_APPS_JSON] = json.encodeToString(updatedRenamedApps)
        }
    }

    suspend fun removeAppCustomName(appKey: String) {
        val currentSettings = settings.first()
        val updatedRenamedApps = currentSettings.renamedApps.toMutableMap()
        updatedRenamedApps.remove(appKey)

        context.settingsDataStore.edit { prefs ->
            prefs[RENAMED_APPS_JSON] = json.encodeToString(updatedRenamedApps)
        }
    }


}