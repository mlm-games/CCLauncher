package app.cclauncher.data.repository

import android.content.Context
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

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app.cclauncher.settings")

class SettingsRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    companion object {
        val SHOW_APP_NAMES = booleanPreferencesKey("SHOW_APP_NAMES")
        val SHOW_APP_ICONS = booleanPreferencesKey("SHOW_APP_ICONS")
        val AUTO_SHOW_KEYBOARD = booleanPreferencesKey("AUTO_SHOW_KEYBOARD")
        val SHOW_HIDDEN_APPS_IN_SEARCH = booleanPreferencesKey("SHOW_HIDDEN_APPS_IN_SEARCH")
        val AUTO_OPEN_FILTERED_APP = booleanPreferencesKey("AUTO_OPEN_FILTERED_APP")
        val SEARCH_TYPE = intPreferencesKey("SEARCH_TYPE")
        val APP_THEME = intPreferencesKey("APP_THEME")
        val TEXT_SIZE_SCALE = floatPreferencesKey("TEXT_SIZE_SCALE")
        val GESTURE_SENSITIVITY = floatPreferencesKey("GESTURE_SENSITIVITY")
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
        val ANIMATION_SPEED = floatPreferencesKey("ANIMATION_SPEED")
        val SEARCH_ALIASES_MODE = intPreferencesKey("SEARCH_ALIASES_MODE")
        val SEARCH_INCLUDE_PACKAGE_NAMES = booleanPreferencesKey("SEARCH_INCLUDE_PACKAGE_NAMES")
        val APP_DRAWER_TAP_TO_OPEN = booleanPreferencesKey("APP_DRAWER_TAP_TO_OPEN")
        val TEXT_COLOR = intPreferencesKey("TEXT_COLOR")
        val USE_CUSTOM_TEXT_COLOR = booleanPreferencesKey("USE_CUSTOM_TEXT_COLOR")
}

    private val settingDefinitions: Map<String, SettingDefinition<*>> = mapOf(
        // Boolean settings
        "showAppNames" to SettingDefinition.BooleanSetting("showAppNames", SHOW_APP_NAMES) { it.showAppNames },
        "showAppIcons" to SettingDefinition.BooleanSetting("showAppIcons", SHOW_APP_ICONS) { it.showAppIcons },
        "autoShowKeyboard" to SettingDefinition.BooleanSetting("autoShowKeyboard", AUTO_SHOW_KEYBOARD) { it.autoShowKeyboard },
        "showHiddenAppsOnSearch" to SettingDefinition.BooleanSetting("showHiddenAppsOnSearch", SHOW_HIDDEN_APPS_IN_SEARCH) { it.showHiddenAppsOnSearch },
        "autoOpenFilteredApp" to SettingDefinition.BooleanSetting("autoOpenFilteredApp", AUTO_OPEN_FILTERED_APP) { it.autoOpenFilteredApp },
        "useSystemFont" to SettingDefinition.BooleanSetting("useSystemFont", USE_SYSTEM_FONT) { it.useSystemFont },
        "useDynamicTheme" to SettingDefinition.BooleanSetting("useDynamicTheme", USE_DYNAMIC_THEME) { it.useDynamicTheme },
        "statusBar" to SettingDefinition.BooleanSetting("statusBar", STATUS_BAR) { it.statusBar },
        "showHomeScreenIcons" to SettingDefinition.BooleanSetting("showHomeScreenIcons", SHOW_HOME_SCREEN_ICONS) { it.showHomeScreenIcons },
        "showIconsInLandscape" to SettingDefinition.BooleanSetting("showIconsInLandscape", SHOW_ICONS_IN_LANDSCAPE) { it.showIconsInLandscape },
        "showIconsInPortrait" to SettingDefinition.BooleanSetting("showIconsInPortrait", SHOW_ICONS_IN_PORTRAIT) { it.showIconsInPortrait },
        "scaleHomeApps" to SettingDefinition.BooleanSetting("scaleHomeApps", SCALE_HOME_APPS) { it.scaleHomeApps },
        "doubleTapToLock" to SettingDefinition.BooleanSetting("doubleTapToLock", DOUBLE_TAP_TO_LOCK) { it.doubleTapToLock },
        "firstOpen" to SettingDefinition.BooleanSetting("firstOpen", FIRST_OPEN) { it.firstOpen },
        "firstSettingsOpen" to SettingDefinition.BooleanSetting("firstSettingsOpen", FIRST_SETTINGS_OPEN) { it.firstSettingsOpen },
        "firstHide" to SettingDefinition.BooleanSetting("firstHide", FIRST_HIDE) { it.firstHide },
        "lockMode" to SettingDefinition.BooleanSetting("lockMode", LOCK_MODE) { it.lockMode },
        "keyboardMessage" to SettingDefinition.BooleanSetting("keyboardMessage", KEYBOARD_MESSAGE) { it.keyboardMessage },
        "plainWallpaper" to SettingDefinition.BooleanSetting("plainWallpaper", PLAIN_WALLPAPER) { it.plainWallpaper },
        "hiddenAppsUpdated" to SettingDefinition.BooleanSetting("hiddenAppsUpdated", HIDDEN_APPS_UPDATED) { it.hiddenAppsUpdated },
        "aboutClicked" to SettingDefinition.BooleanSetting("aboutClicked", ABOUT_CLICKED) { it.aboutClicked },
        "rateClicked" to SettingDefinition.BooleanSetting("rateClicked", RATE_CLICKED) { it.rateClicked },
        "searchResultsUseHomeFont" to SettingDefinition.BooleanSetting("searchResultsUseHomeFont", SEARCH_RESULTS_USE_HOME_FONT) { it.searchResultsUseHomeFont },
        "lockSettings" to SettingDefinition.BooleanSetting("lockSettings", LOCK_SETTINGS) { it.lockSettings },
        "searchIncludePackageNames" to SettingDefinition.BooleanSetting("searchIncludePackageNames", SEARCH_INCLUDE_PACKAGE_NAMES) { it.searchIncludePackageNames },
        "appDrawerTapToOpen" to SettingDefinition.BooleanSetting("appDrawerTapToOpen", APP_DRAWER_TAP_TO_OPEN) { it.appDrawerTapToOpen },
        "useCustomTextColor" to SettingDefinition.BooleanSetting("useCustomTextColor", USE_CUSTOM_TEXT_COLOR) { it.useCustomTextColor },

        // Int settings
        "textColor" to SettingDefinition.IntSetting("textColor", TEXT_COLOR) { it.textColor },
        "searchType" to SettingDefinition.IntSetting("searchType", SEARCH_TYPE) { it.searchType },
        "appTheme" to SettingDefinition.IntSetting("appTheme", APP_THEME) { it.appTheme },
        "fontWeight" to SettingDefinition.IntSetting("fontWeight", FONT_WEIGHT) { it.fontWeight },
        "iconCornerRadius" to SettingDefinition.IntSetting("iconCornerRadius", ICON_CORNER_RADIUS) { it.iconCornerRadius },
        "itemSpacing" to SettingDefinition.IntSetting("itemSpacing", ITEM_SPACING) { it.itemSpacing },
        "screenOrientation" to SettingDefinition.IntSetting("screenOrientation", SCREEN_ORIENTATION) { it.screenOrientation },
        "swipeDownAction" to SettingDefinition.IntSetting("swipeDownAction", SWIPE_DOWN_ACTION) { it.swipeDownAction },
        "swipeUpAction" to SettingDefinition.IntSetting("swipeUpAction", SWIPE_UP_ACTION) { it.swipeUpAction },
        "swipeLeftAction" to SettingDefinition.IntSetting("swipeLeftAction", SWIPE_LEFT_ACTION) { it.swipeLeftAction },
        "swipeRightAction" to SettingDefinition.IntSetting("swipeRightAction", SWIPE_RIGHT_ACTION) { it.swipeRightAction },
        "appLabelAlignment" to SettingDefinition.IntSetting("appLabelAlignment", APP_LABEL_ALIGNMENT) { it.appLabelAlignment },
        "showHintCounter" to SettingDefinition.IntSetting("showHintCounter", SHOW_HINT_COUNTER) { it.showHintCounter },
        "homeScreenRows" to SettingDefinition.IntSetting("homeScreenRows", HOME_SCREEN_ROWS) { it.homeScreenRows },
        "homeScreenColumns" to SettingDefinition.IntSetting("homeScreenColumns", HOME_SCREEN_COLUMNS) { it.homeScreenColumns },
        "searchSortOrder" to SettingDefinition.IntSetting("searchSortOrder", SEARCH_SORT_ORDER) { it.searchSortOrder },
        "searchAliasesMode" to SettingDefinition.IntSetting("searchAliasesMode", SEARCH_ALIASES_MODE) { it.searchAliasesMode },

        // Float settings
        "gestureSensitivity" to SettingDefinition.FloatSetting("gestureSensitivity", GESTURE_SENSITIVITY) { it.gestureSensitivity },
        "textSizeScale" to SettingDefinition.FloatSetting("textSizeScale", TEXT_SIZE_SCALE) { it.textSizeScale },
        "searchResultsFontSize" to SettingDefinition.FloatSetting("searchResultsFontSize", SEARCH_RESULTS_FONT_SIZE) { it.searchResultsFontSize },
        "animationSpeed" to SettingDefinition.FloatSetting("animationSpeed", ANIMATION_SPEED) { it.animationSpeed },

        // String settings
        "userState" to SettingDefinition.StringSetting("userState", USER_STATE) { it.userState },
        "selectedIconPack" to SettingDefinition.StringSetting("selectedIconPack", SELECTED_ICON_PACK) { it.selectedIconPack },
        "customFontPath" to SettingDefinition.StringSetting("customFontPath", CUSTOM_FONT_PATH) { it.customFontPath },
        "settingsLockPin" to SettingDefinition.StringSetting("settingsLockPin", SETTINGS_LOCK_PIN) { it.settingsLockPin },

        // Long settings
        "firstOpenTime" to SettingDefinition.LongSetting("firstOpenTime", FIRST_OPEN_TIME) { it.firstOpenTime },
        "shareShownTime" to SettingDefinition.LongSetting("shareShownTime", SHARE_SHOWN_TIME) { it.shareShownTime },

        // String Set settings
        "hiddenApps" to SettingDefinition.StringSetSetting("hiddenApps", HIDDEN_APPS) { it.hiddenApps }
    )

    private val defaultAppSettings = AppSettings.getDefault()

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->

        val swipeLeftApp = prefs[SWIPE_LEFT_APP_JSON]?.let {
            json.decodeFromStringCatching(it, defaultAppSettings.swipeLeftApp)
        } ?: defaultAppSettings.swipeLeftApp

        val swipeRightApp = prefs[SWIPE_RIGHT_APP_JSON]?.let {
            json.decodeFromStringCatching(it, defaultAppSettings.swipeRightApp)
        } ?: defaultAppSettings.swipeRightApp

        val swipeUpApp = prefs[SWIPE_UP_APP_JSON]?.let {
            json.decodeFromStringCatching(it, defaultAppSettings.swipeUpApp)
        } ?: defaultAppSettings.swipeUpApp

        val swipeDownApp = prefs[SWIPE_DOWN_APP_JSON]?.let {
            json.decodeFromStringCatching(it, defaultAppSettings.swipeDownApp)
        } ?: defaultAppSettings.swipeDownApp

        val renamedApps = prefs[RENAMED_APPS_JSON]?.let {
            try { json.decodeFromString<Map<String, String>>(it) }
            catch (e: Exception) { Log.e("SettingsRepo", "Failed to decode renamed apps JSON: ${e.message}"); mapOf() }
        } ?: mapOf()

        val recentAppHistory = prefs[RECENT_APP_HISTORY]?.let {
            try { json.decodeFromString<Map<String, Long>>(it) }
            catch (e: Exception) { Log.e("SettingsRepo", "Failed to decode recent app history JSON: ${e.message}"); mapOf() }
        } ?: mapOf()

        AppSettings(
            // General
            showAppNames = prefs[SHOW_APP_NAMES] ?: false,
            showAppIcons = prefs[SHOW_APP_ICONS] ?: true,
            autoShowKeyboard = prefs[AUTO_SHOW_KEYBOARD] ?: true,
            showHiddenAppsOnSearch = prefs[SHOW_HIDDEN_APPS_IN_SEARCH] ?: false,
            autoOpenFilteredApp = prefs[AUTO_OPEN_FILTERED_APP] ?: true,
            searchType = prefs[SEARCH_TYPE] ?: Constants.SearchType.CONTAINS,
            searchSortOrder = prefs[SEARCH_SORT_ORDER] ?: Constants.SortOrder.ALPHABETICAL,
            searchAliasesMode = prefs[SEARCH_ALIASES_MODE] ?: 0,
            searchIncludePackageNames = prefs[SEARCH_INCLUDE_PACKAGE_NAMES] ?: false,

            appTheme = prefs[APP_THEME] ?: AppCompatDelegate.MODE_NIGHT_YES,
            textSizeScale = prefs[TEXT_SIZE_SCALE] ?: 1.0f,
            gestureSensitivity = prefs[GESTURE_SENSITIVITY] ?: 1.0f,
            fontWeight = prefs[FONT_WEIGHT] ?: 2,
            useSystemFont = prefs[USE_SYSTEM_FONT] ?: true,
            useDynamicTheme = prefs[USE_DYNAMIC_THEME] ?: false,
            iconCornerRadius = prefs[ICON_CORNER_RADIUS] ?: 0,
            itemSpacing = prefs[ITEM_SPACING] ?: 1,
            customFontPath = prefs[CUSTOM_FONT_PATH] ?: "",
            animationSpeed = prefs[ANIMATION_SPEED] ?: 1.0f,

            statusBar = prefs[STATUS_BAR] ?: false,
            appDrawerTapToOpen = prefs[APP_DRAWER_TAP_TO_OPEN] ?: true,
            screenOrientation = prefs[SCREEN_ORIENTATION] ?: 0,
            showHomeScreenIcons = prefs[SHOW_HOME_SCREEN_ICONS] ?: false,
            showIconsInLandscape = prefs[SHOW_ICONS_IN_LANDSCAPE] ?: false,
            showIconsInPortrait = prefs[SHOW_ICONS_IN_PORTRAIT] ?: false,
            scaleHomeApps = prefs[SCALE_HOME_APPS] ?: true,
            homeScreenRows = prefs[HOME_SCREEN_ROWS] ?: 8,
            homeScreenColumns = prefs[HOME_SCREEN_COLUMNS] ?: 4,

            swipeDownAction = prefs[SWIPE_DOWN_ACTION] ?: Constants.SwipeAction.NOTIFICATIONS,
            swipeUpAction = prefs[SWIPE_UP_ACTION] ?: Constants.SwipeAction.SEARCH,
            doubleTapToLock = prefs[DOUBLE_TAP_TO_LOCK] ?: false,
            swipeLeftAction = prefs[SWIPE_LEFT_ACTION] ?: Constants.SwipeAction.NULL,
            swipeRightAction = prefs[SWIPE_RIGHT_ACTION] ?: Constants.SwipeAction.NULL,

            lockSettings = prefs[LOCK_SETTINGS] ?: false,
            settingsLockPin = prefs[SETTINGS_LOCK_PIN] ?: "",

            firstOpen = prefs[FIRST_OPEN] ?: true,
            firstOpenTime = prefs[FIRST_OPEN_TIME] ?: 0L,
            firstSettingsOpen = prefs[FIRST_SETTINGS_OPEN] ?: true,
            firstHide = prefs[FIRST_HIDE] ?: true,
            userState = prefs[USER_STATE] ?: Constants.UserState.START,
            lockMode = prefs[LOCK_MODE] ?: false,
            keyboardMessage = prefs[KEYBOARD_MESSAGE] ?: false,
            plainWallpaper = prefs[PLAIN_WALLPAPER] ?: false,
            appLabelAlignment = prefs[APP_LABEL_ALIGNMENT] ?: 0,
            hiddenApps = prefs[HIDDEN_APPS] ?: emptySet(),
            hiddenAppsUpdated = prefs[HIDDEN_APPS_UPDATED] ?: false,
            showHintCounter = prefs[SHOW_HINT_COUNTER] ?: 1,
            aboutClicked = prefs[ABOUT_CLICKED] ?: false,
            rateClicked = prefs[RATE_CLICKED] ?: false,
            shareShownTime = prefs[SHARE_SHOWN_TIME] ?: 0L,
            searchResultsUseHomeFont = prefs[SEARCH_RESULTS_USE_HOME_FONT] ?: false,
            searchResultsFontSize = prefs[SEARCH_RESULTS_FONT_SIZE] ?: 1.0f,
            selectedIconPack = prefs[SELECTED_ICON_PACK] ?: "default",
            textColor = prefs[TEXT_COLOR] ?: 0,
            useCustomTextColor = prefs[USE_CUSTOM_TEXT_COLOR] ?: false,

            swipeLeftApp = swipeLeftApp,
            swipeRightApp = swipeRightApp,
            swipeUpApp = swipeUpApp,
            swipeDownApp = swipeDownApp,
            renamedApps = renamedApps,
            recentAppHistory = recentAppHistory
        )
    }

    suspend fun updateSetting(propertyName: String, value: Any) {
        val definition = settingDefinitions[propertyName]
        if (definition != null) {
            context.settingsDataStore.edit { prefs ->
                when (definition) {
                    is SettingDefinition.BooleanSetting -> prefs[definition.key] = value as Boolean
                    is SettingDefinition.IntSetting -> prefs[definition.key] = value as Int
                    is SettingDefinition.FloatSetting -> prefs[definition.key] = value as Float
                    is SettingDefinition.StringSetting -> prefs[definition.key] = value as String
                    is SettingDefinition.LongSetting -> prefs[definition.key] = value as Long
                    is SettingDefinition.StringSetSetting -> {
                        @Suppress("UNCHECKED_CAST")
                        prefs[definition.key] = value as Set<String>
                    }
                }
            }
        } else {
            Log.w("SettingsRepo", "Unknown setting: $propertyName")
        }
    }

    suspend fun updateSetting(update: (AppSettings) -> AppSettings) {
        val currentSettings = settings.first()
        val updatedSettings = update(currentSettings)

        context.settingsDataStore.edit { prefs ->
            settingDefinitions.values.forEach { definition ->
                val currentValue = definition.getValue(currentSettings)
                val newValue = definition.getValue(updatedSettings)
                if (currentValue != newValue) {
                    @Suppress("UNCHECKED_CAST")
                    when (definition) {
                        is SettingDefinition.BooleanSetting -> prefs[definition.key] = newValue as Boolean
                        is SettingDefinition.IntSetting -> prefs[definition.key] = newValue as Int
                        is SettingDefinition.FloatSetting -> prefs[definition.key] = newValue as Float
                        is SettingDefinition.StringSetting -> prefs[definition.key] = newValue as String
                        is SettingDefinition.LongSetting -> prefs[definition.key] = newValue as Long
                        is SettingDefinition.StringSetSetting -> prefs[definition.key] = newValue as Set<String>
                    }
                }
            }

            if (currentSettings.swipeLeftApp != updatedSettings.swipeLeftApp) {
                prefs[SWIPE_LEFT_APP_JSON] = json.encodeToString(updatedSettings.swipeLeftApp)
            }
            if (currentSettings.swipeRightApp != updatedSettings.swipeRightApp) {
                prefs[SWIPE_RIGHT_APP_JSON] = json.encodeToString(updatedSettings.swipeRightApp)
            }
            if (currentSettings.swipeUpApp != updatedSettings.swipeUpApp) {
                prefs[SWIPE_UP_APP_JSON] = json.encodeToString(updatedSettings.swipeUpApp)
            }
            if (currentSettings.swipeDownApp != updatedSettings.swipeDownApp) {
                prefs[SWIPE_DOWN_APP_JSON] = json.encodeToString(updatedSettings.swipeDownApp)
            }
            if (currentSettings.renamedApps != updatedSettings.renamedApps) {
                prefs[RENAMED_APPS_JSON] = json.encodeToString(updatedSettings.renamedApps)
            }
            if (currentSettings.recentAppHistory != updatedSettings.recentAppHistory) {
                prefs[RECENT_APP_HISTORY] = json.encodeToString(updatedSettings.recentAppHistory)
            }
        }
    }

    suspend fun setFirstOpen(value: Boolean) = updateSetting("firstOpen", value)
    suspend fun setAppTheme(value: Int) = updateSetting("appTheme", value)

    fun getHomeLayout(): Flow<HomeLayout> = context.settingsDataStore.data
        .map { prefs ->
            prefs[HOME_LAYOUT]?.let { jsonString ->
                try { Json.decodeFromString<HomeLayout>(jsonString) }
                catch (e: Exception) { Log.e("SettingsRepo", "Failed to decode HomeLayout JSON", e); HomeLayout() }
            } ?: HomeLayout()
        }
        .catch { exception ->
            Log.e("SettingsRepo", "Error reading HomeLayout", exception)
            emit(HomeLayout())
        }

    suspend fun saveHomeLayout(layout: HomeLayout) {
        try {
            val jsonString = Json.encodeToString(layout)
            context.settingsDataStore.edit { prefs -> prefs[HOME_LAYOUT] = jsonString }
        } catch (e: Exception) {
            Log.e("SettingsRepo", "Failed to encode or save HomeLayout JSON", e)
        }
    }

    suspend fun triggerHomeLayoutRefresh() {
        val currentLayout = getHomeLayout().first()
        saveHomeLayout(currentLayout)
    }

    suspend fun setSwipeLeftApp(app: AppPreference) {
        context.settingsDataStore.edit { prefs -> prefs[SWIPE_LEFT_APP_JSON] = json.encodeToString(app) }
    }

    suspend fun setSwipeRightApp(app: AppPreference) {
        context.settingsDataStore.edit { prefs -> prefs[SWIPE_RIGHT_APP_JSON] = json.encodeToString(app) }
    }

    suspend fun setSwipeUpApp(app: AppPreference) {
        context.settingsDataStore.edit { prefs -> prefs[SWIPE_UP_APP_JSON] = json.encodeToString(app) }
    }

    suspend fun setSwipeDownApp(app: AppPreference) {
        context.settingsDataStore.edit { prefs -> prefs[SWIPE_DOWN_APP_JSON] = json.encodeToString(app) }
    }

    suspend fun getSwipeLeftApp(): AppPreference = settings.first().swipeLeftApp
    suspend fun getSwipeRightApp(): AppPreference = settings.first().swipeRightApp

    suspend fun setSettingsLock(locked: Boolean) = updateSetting("lockSettings", locked)
    suspend fun setSettingsLockPin(pin: String) = updateSetting("settingsLockPin", pin)
    suspend fun validateSettingsPin(pin: String): Boolean = settings.first().settingsLockPin == pin

    suspend fun setCustomFont(uri: Uri) {
        try {
            val fontFile = File(context.filesDir, Constants.CUSTOM_FONT_FILENAME)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(fontFile).use { output -> input.copyTo(output) }
            }
            updateSetting("customFontPath", fontFile.absolutePath)
        } catch (e: Exception) {
            Log.e("SettingsRepo", "Failed to copy font file", e)
        }
    }

    suspend fun clearCustomFont() {
        val currentPath = settings.first().customFontPath
        if (currentPath.isNotEmpty()) {
            try { File(currentPath).delete() }
            catch (e: Exception) { Log.e("SettingsRepo", "Error deleting old font file", e) }
        }
        updateSetting("customFontPath", "")
    }

    suspend fun toggleAppHidden(packageKey: String) {
        updateSetting {
            val updatedHiddenApps = it.hiddenApps.toMutableSet()
            if (updatedHiddenApps.contains(packageKey)) updatedHiddenApps.remove(packageKey) else updatedHiddenApps.add(packageKey)
            it.copy(hiddenApps = updatedHiddenApps)
        }
    }

    suspend fun setAppCustomName(appKey: String, customName: String) {
        val currentSettings = settings.first()
        val updatedRenamedApps = currentSettings.renamedApps.toMutableMap()
        if (customName.isBlank()) updatedRenamedApps.remove(appKey) else updatedRenamedApps[appKey] = customName
        context.settingsDataStore.edit { prefs -> prefs[RENAMED_APPS_JSON] = json.encodeToString(updatedRenamedApps) }
    }

    suspend fun removeAppCustomName(appKey: String) {
        val currentSettings = settings.first()
        val updatedRenamedApps = currentSettings.renamedApps.toMutableMap()
        updatedRenamedApps.remove(appKey)
        context.settingsDataStore.edit { prefs -> prefs[RENAMED_APPS_JSON] = json.encodeToString(updatedRenamedApps) }
    }

    suspend fun updateAppLaunchTime(appKey: String) {
        val currentSettings = settings.first()
        val updatedHistory = currentSettings.recentAppHistory.toMutableMap()
        updatedHistory[appKey] = System.currentTimeMillis()
        if (updatedHistory.size > 100) {
            val oldest = updatedHistory.entries.sortedBy { it.value }.take(20)
            oldest.forEach { updatedHistory.remove(it.key) }
        }
        context.settingsDataStore.edit { prefs -> prefs[RECENT_APP_HISTORY] = json.encodeToString(updatedHistory) }
    }

    private inline fun <reified T> Json.decodeFromStringCatching(jsonString: String, default: T): T {
        return try { this.decodeFromString<T>(jsonString) }
        catch (e: Exception) {
            Log.e("SettingsRepo", "Failed to decode JSON for ${T::class.simpleName}: ${e.message}. Using default.")
            default
        }
    }
}