package app.cclauncher.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import app.cclauncher.data.settings.AppSettings
import app.cclauncher.data.AppPreference
import app.cclauncher.data.Constants
import app.cclauncher.data.HomeAppPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import android.view.Gravity
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.flow.first

// Extension property for Context to access the DataStore instance
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app.cclauncher.settings")

/**
 * Repository for managing application settings
 */
class SettingsRepository(private val context: Context) {

    companion object {
        // Define all preference keys
        val HOME_APPS_NUM = intPreferencesKey("HOME_APPS_NUM")
        val HOME_SCREEN_COLUMNS = intPreferencesKey("HOME_SCREEN_COLUMNS")
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
        val SWIPE_LEFT_ENABLED = booleanPreferencesKey("SWIPE_LEFT_ENABLED")
        val SWIPE_RIGHT_ENABLED = booleanPreferencesKey("SWIPE_RIGHT_ENABLED")
        val SWIPE_DOWN_ACTION = intPreferencesKey("SWIPE_DOWN_ACTION")
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


        // App keys
        val APP_NAME_KEYS = List(Constants.HomeAppCount.NUM) { stringPreferencesKey("APP_NAME_${it+1}") }
        val APP_PACKAGE_KEYS = List(Constants.HomeAppCount.NUM) { stringPreferencesKey("APP_PACKAGE_${it+1}") }
        val APP_ICONS = List(Constants.HomeAppCount.NUM) { stringPreferencesKey("APP_ICONS_${it+1}")}
        val APP_ACTIVITY_CLASS_NAME_KEYS = List(Constants.HomeAppCount.NUM) { stringPreferencesKey("APP_ACTIVITY_CLASS_NAME_${it+1}") }
        val APP_USER_KEYS = List(Constants.HomeAppCount.NUM) { stringPreferencesKey("APP_USER_${it+1}") }

        val APP_NAME_SWIPE_LEFT = stringPreferencesKey("APP_NAME_SWIPE_LEFT")
        val APP_NAME_SWIPE_RIGHT = stringPreferencesKey("APP_NAME_SWIPE_RIGHT")
        val APP_PACKAGE_SWIPE_LEFT = stringPreferencesKey("APP_PACKAGE_SWIPE_LEFT")
        val APP_PACKAGE_SWIPE_RIGHT = stringPreferencesKey("APP_PACKAGE_SWIPE_RIGHT")
        val APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT = stringPreferencesKey("APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT")
        val APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT = stringPreferencesKey("APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT")
        val APP_USER_SWIPE_LEFT = stringPreferencesKey("APP_USER_SWIPE_LEFT")
        val APP_USER_SWIPE_RIGHT = stringPreferencesKey("APP_USER_SWIPE_RIGHT")

        val CLOCK_APP_PACKAGE = stringPreferencesKey("CLOCK_APP_PACKAGE")
        val CLOCK_APP_USER = stringPreferencesKey("CLOCK_APP_USER")
        val CLOCK_APP_CLASS_NAME = stringPreferencesKey("CLOCK_APP_CLASS_NAME")
        val CALENDAR_APP_PACKAGE = stringPreferencesKey("CALENDAR_APP_PACKAGE")
        val CALENDAR_APP_USER = stringPreferencesKey("CALENDAR_APP_USER")
        val CALENDAR_APP_CLASS_NAME = stringPreferencesKey("CALENDAR_APP_CLASS_NAME")
    }

    /**
     * Flow of settings that emits whenever any setting changes
     */
    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            // General settings
            homeAppsNum = prefs[HOME_APPS_NUM] ?: 0,
            showAppNames = prefs[SHOW_APP_NAMES] ?: true,
            showAppIcons = prefs[SHOW_APP_ICONS] ?: false,
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
            iconCornerRadius = prefs[ICON_CORNER_RADIUS] ?: 8,
            itemSpacing = prefs[ITEM_SPACING] ?: 1,

            // Layout settings
            homeAlignment = prefs[HOME_ALIGNMENT] ?: Gravity.CENTER,
            homeBottomAlignment = prefs[HOME_BOTTOM_ALIGNMENT] ?: false,
            statusBar = prefs[STATUS_BAR] ?: false,
            homeScreenColumns = prefs[HOME_SCREEN_COLUMNS] ?: 1,
            dateTimeVisibility = prefs[DATE_TIME_VISIBILITY] ?: Constants.DateTime.ON,
            forceLandscapeMode = prefs[FORCE_LANDSCAPE_MODE] ?: false,
            showHomeScreenIcons = prefs[SHOW_HOME_SCREEN_ICONS] ?: false,
            showIconsInLandscape = prefs[SHOW_ICONS_IN_LANDSCAPE] ?: false,
            showIconsInPortrait = prefs[SHOW_ICONS_IN_PORTRAIT] ?: false,

            // Gestures settings
            swipeLeftEnabled = prefs[SWIPE_LEFT_ENABLED] ?: true,
            swipeRightEnabled = prefs[SWIPE_RIGHT_ENABLED] ?: true,
            swipeDownAction = prefs[SWIPE_DOWN_ACTION] ?: Constants.SwipeDownAction.NOTIFICATIONS,
            doubleTapToLock = prefs[DOUBLE_TAP_TO_LOCK] ?: false,

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

            // Search result appearance
            searchResultsUseHomeFont = prefs[SEARCH_RESULTS_USE_HOME_FONT] ?: false,
            searchResultsFontSize = prefs[SEARCH_RESULTS_FONT_SIZE] ?: 1.0f
        )
    }

    /**
     * Update a specific setting
     */
    suspend fun updateSetting(update: (AppSettings) -> AppSettings) {
        val currentSettings = settings.first()
        val updatedSettings = update(currentSettings)

        context.settingsDataStore.edit { prefs ->
            // General settings
            if (currentSettings.homeAppsNum != updatedSettings.homeAppsNum)
                prefs[HOME_APPS_NUM] = updatedSettings.homeAppsNum
            if (currentSettings.showAppNames != updatedSettings.showAppNames)
                prefs[SHOW_APP_NAMES] = updatedSettings.showAppNames
            if (currentSettings.showAppIcons != updatedSettings.showAppIcons)
                prefs[SHOW_APP_ICONS] = updatedSettings.showAppIcons
            if (currentSettings.autoShowKeyboard != updatedSettings.autoShowKeyboard)
                prefs[AUTO_SHOW_KEYBOARD] = updatedSettings.autoShowKeyboard
            if (currentSettings.showHiddenAppsOnSearch != updatedSettings.showHiddenAppsOnSearch)
                prefs[SHOW_HIDDEN_APPS_IN_SEARCH] = updatedSettings.showHiddenAppsOnSearch
            if (currentSettings.autoOpenFilteredApp != updatedSettings.autoOpenFilteredApp)
                prefs[AUTO_OPEN_FILTERED_APP] = updatedSettings.autoOpenFilteredApp
            if (currentSettings.searchType != updatedSettings.searchType)
                prefs[SEARCH_TYPE] = updatedSettings.searchType

            // Appearance settings
            if (currentSettings.appTheme != updatedSettings.appTheme)
                prefs[APP_THEME] = updatedSettings.appTheme
            if (currentSettings.textSizeScale != updatedSettings.textSizeScale)
                prefs[TEXT_SIZE_SCALE] = updatedSettings.textSizeScale
            if (currentSettings.fontWeight != updatedSettings.fontWeight)
                prefs[FONT_WEIGHT] = updatedSettings.fontWeight
            if (currentSettings.useSystemFont != updatedSettings.useSystemFont)
                prefs[USE_SYSTEM_FONT] = updatedSettings.useSystemFont
            if (currentSettings.useDynamicTheme != updatedSettings.useDynamicTheme)
                prefs[USE_DYNAMIC_THEME] = updatedSettings.useDynamicTheme
            if (currentSettings.iconCornerRadius != updatedSettings.iconCornerRadius)
                prefs[ICON_CORNER_RADIUS] = updatedSettings.iconCornerRadius
            if (currentSettings.itemSpacing != updatedSettings.itemSpacing)
                prefs[ITEM_SPACING] = updatedSettings.itemSpacing

            // Layout settings
            if (currentSettings.homeAlignment != updatedSettings.homeAlignment)
                prefs[HOME_ALIGNMENT] = updatedSettings.homeAlignment
            if (currentSettings.homeBottomAlignment != updatedSettings.homeBottomAlignment)
                prefs[HOME_BOTTOM_ALIGNMENT] = updatedSettings.homeBottomAlignment
            if (currentSettings.statusBar != updatedSettings.statusBar)
                prefs[STATUS_BAR] = updatedSettings.statusBar
            if (currentSettings.homeScreenColumns != updatedSettings.homeScreenColumns)
                prefs[HOME_SCREEN_COLUMNS] = updatedSettings.homeScreenColumns
            if (currentSettings.dateTimeVisibility != updatedSettings.dateTimeVisibility)
                prefs[DATE_TIME_VISIBILITY] = updatedSettings.dateTimeVisibility
            if (currentSettings.forceLandscapeMode != updatedSettings.forceLandscapeMode)
                prefs[FORCE_LANDSCAPE_MODE] = updatedSettings.forceLandscapeMode
            if (currentSettings.showHomeScreenIcons != updatedSettings.showHomeScreenIcons)
                prefs[SHOW_HOME_SCREEN_ICONS] = updatedSettings.showHomeScreenIcons
            if (currentSettings.showIconsInLandscape != updatedSettings.showIconsInLandscape)
                prefs[SHOW_ICONS_IN_LANDSCAPE] = updatedSettings.showIconsInLandscape
            if (currentSettings.showIconsInPortrait != updatedSettings.showIconsInPortrait)
                prefs[SHOW_ICONS_IN_PORTRAIT] = updatedSettings.showIconsInPortrait

            // Gestures settings
            if (currentSettings.swipeLeftEnabled != updatedSettings.swipeLeftEnabled)
                prefs[SWIPE_LEFT_ENABLED] = updatedSettings.swipeLeftEnabled
            if (currentSettings.swipeRightEnabled != updatedSettings.swipeRightEnabled)
                prefs[SWIPE_RIGHT_ENABLED] = updatedSettings.swipeRightEnabled
            if (currentSettings.swipeDownAction != updatedSettings.swipeDownAction)
                prefs[SWIPE_DOWN_ACTION] = updatedSettings.swipeDownAction
            if (currentSettings.doubleTapToLock != updatedSettings.doubleTapToLock)
                prefs[DOUBLE_TAP_TO_LOCK] = updatedSettings.doubleTapToLock

            // Other properties
            if (currentSettings.firstOpen != updatedSettings.firstOpen)
                prefs[FIRST_OPEN] = updatedSettings.firstOpen
            if (currentSettings.firstOpenTime != updatedSettings.firstOpenTime)
                prefs[FIRST_OPEN_TIME] = updatedSettings.firstOpenTime
            if (currentSettings.firstSettingsOpen != updatedSettings.firstSettingsOpen)
                prefs[FIRST_SETTINGS_OPEN] = updatedSettings.firstSettingsOpen
            if (currentSettings.firstHide != updatedSettings.firstHide)
                prefs[FIRST_HIDE] = updatedSettings.firstHide
            if (currentSettings.userState != updatedSettings.userState)
                prefs[USER_STATE] = updatedSettings.userState
            if (currentSettings.lockMode != updatedSettings.lockMode)
                prefs[LOCK_MODE] = updatedSettings.lockMode
            if (currentSettings.keyboardMessage != updatedSettings.keyboardMessage)
                prefs[KEYBOARD_MESSAGE] = updatedSettings.keyboardMessage
            if (currentSettings.plainWallpaper != updatedSettings.plainWallpaper)
                prefs[PLAIN_WALLPAPER] = updatedSettings.plainWallpaper
            if (currentSettings.appLabelAlignment != updatedSettings.appLabelAlignment)
                prefs[APP_LABEL_ALIGNMENT] = updatedSettings.appLabelAlignment
            if (currentSettings.hiddenApps != updatedSettings.hiddenApps)
                prefs[HIDDEN_APPS] = updatedSettings.hiddenApps
            if (currentSettings.hiddenAppsUpdated != updatedSettings.hiddenAppsUpdated)
                prefs[HIDDEN_APPS_UPDATED] = updatedSettings.hiddenAppsUpdated
            if (currentSettings.showHintCounter != updatedSettings.showHintCounter)
                prefs[SHOW_HINT_COUNTER] = updatedSettings.showHintCounter
            if (currentSettings.aboutClicked != updatedSettings.aboutClicked)
                prefs[ABOUT_CLICKED] = updatedSettings.aboutClicked
            if (currentSettings.rateClicked != updatedSettings.rateClicked)
                prefs[RATE_CLICKED] = updatedSettings.rateClicked
            if (currentSettings.shareShownTime != updatedSettings.shareShownTime)
                prefs[SHARE_SHOWN_TIME] = updatedSettings.shareShownTime

            // Search result appearance
            if (currentSettings.searchResultsUseHomeFont != updatedSettings.searchResultsUseHomeFont)
                prefs[SEARCH_RESULTS_USE_HOME_FONT] = updatedSettings.searchResultsUseHomeFont
            if (currentSettings.searchResultsFontSize != updatedSettings.searchResultsFontSize)
                prefs[SEARCH_RESULTS_FONT_SIZE] = updatedSettings.searchResultsFontSize
        }
    }

    /**
     * Convenience methods for updating specific settings
     */
    suspend fun setHomeAppsNum(value: Int) {
        updateSetting { it.copy(homeAppsNum = value) }
    }

    suspend fun setShowAppNames(value: Boolean) {
        updateSetting { it.copy(showAppNames = value) }
    }

    suspend fun setShowAppIcons(value: Boolean) {
        updateSetting { it.copy(showAppIcons = value) }
    }

    suspend fun setAutoShowKeyboard(value: Boolean) {
        updateSetting { it.copy(autoShowKeyboard = value) }
    }

    suspend fun setShowHiddenAppsOnSearch(value: Boolean) {
        updateSetting { it.copy(showHiddenAppsOnSearch = value) }
    }

    suspend fun setAutoOpenFilteredApp(value: Boolean) {
        updateSetting { it.copy(autoOpenFilteredApp = value) }
    }

    suspend fun setSearchType(value: Int) {
        updateSetting { it.copy(searchType = value) }
    }

    suspend fun setAppTheme(value: Int) {
        updateSetting { it.copy(appTheme = value) }
    }

    suspend fun setTextSizeScale(value: Float) {
        updateSetting { it.copy(textSizeScale = value) }
    }

    suspend fun setFontWeight(value: Int) {
        updateSetting { it.copy(fontWeight = value) }
    }

    suspend fun setUseSystemFont(value: Boolean) {
        updateSetting { it.copy(useSystemFont = value) }
    }

    suspend fun setUseDynamicTheme(value: Boolean) {
        updateSetting { it.copy(useDynamicTheme = value) }
    }

    suspend fun setIconCornerRadius(value: Int) {
        updateSetting { it.copy(iconCornerRadius = value) }
    }

    suspend fun setItemSpacing(value: Int) {
        updateSetting { it.copy(itemSpacing = value) }
    }

    suspend fun setHomeAlignment(value: Int) {
        updateSetting { it.copy(homeAlignment = value) }
    }

    suspend fun setHomeBottomAlignment(value: Boolean) {
        updateSetting { it.copy(homeBottomAlignment = value) }
    }

    suspend fun setStatusBar(value: Boolean) {
        updateSetting { it.copy(statusBar = value) }
    }

    suspend fun setHomeScreenColumns(value: Int) {
        updateSetting { it.copy(homeScreenColumns = value) }
    }

    suspend fun setDateTimeVisibility(value: Int) {
        updateSetting { it.copy(dateTimeVisibility = value) }
    }

    suspend fun setForceLandscapeMode(value: Boolean) {
        updateSetting { it.copy(forceLandscapeMode = value) }
    }

    suspend fun setShowIconsInLandscape(value: Boolean) {
        updateSetting { it.copy(showIconsInLandscape = value) }
    }

    suspend fun setShowIconsInPortrait(value: Boolean) {
        updateSetting { it.copy(showIconsInPortrait = value) }
    }

    suspend fun setShowHomeScreenIcons(value: Boolean) {
        updateSetting { it.copy(showHomeScreenIcons = value) }
    }

    suspend fun setSwipeLeftEnabled(value: Boolean) {
        updateSetting { it.copy(swipeLeftEnabled = value) }
    }

    suspend fun setSwipeRightEnabled(value: Boolean) {
        updateSetting { it.copy(swipeRightEnabled = value) }
    }

    suspend fun setSwipeDownAction(value: Int) {
        updateSetting { it.copy(swipeDownAction = value) }
    }

    suspend fun setDoubleTapToLock(value: Boolean) {
        updateSetting { it.copy(doubleTapToLock = value) }
    }

    suspend fun setFirstOpen(value: Boolean) {
        updateSetting { it.copy(firstOpen = value) }
    }

    suspend fun setSearchResultsUseHomeFont(value: Boolean) {
        updateSetting { it.copy(searchResultsUseHomeFont = value) }
    }

    suspend fun setSearchResultsFontSize(value: Float) {
        updateSetting { it.copy(searchResultsFontSize = value) }
    }

    /**
     * Methods for managing home apps
     */
    suspend fun setHomeApp(position: Int, app: HomeAppPreference) {
        context.settingsDataStore.edit { prefs ->
            if (position >= 0 && position < Constants.HomeAppCount.NUM) {
                prefs[APP_NAME_KEYS[position]] = app.label
                prefs[APP_PACKAGE_KEYS[position]] = app.packageName
                if (app.activityClassName != null) {
                    prefs[APP_ACTIVITY_CLASS_NAME_KEYS[position]] = app.activityClassName
                } else {
                    prefs.remove(APP_ACTIVITY_CLASS_NAME_KEYS[position])
                }
                prefs[APP_USER_KEYS[position]] = app.userString
            }
        }
    }

    suspend fun getHomeApps(): List<HomeAppPreference> {
        return context.settingsDataStore.data.map { prefs ->
            List(Constants.HomeAppCount.NUM) { i ->
                HomeAppPreference(
                    label = prefs[APP_NAME_KEYS[i]] ?: "",
                    packageName = prefs[APP_PACKAGE_KEYS[i]] ?: "",
                    activityClassName = prefs[APP_ACTIVITY_CLASS_NAME_KEYS[i]],
                    userString = prefs[APP_USER_KEYS[i]] ?: ""
                )
            }
        }.first()
    }

    /**
     * Methods for managing swipe apps
     */
    suspend fun setSwipeLeftApp(app: AppPreference) {
        context.settingsDataStore.edit { prefs ->
            prefs[APP_NAME_SWIPE_LEFT] = app.label
            prefs[APP_PACKAGE_SWIPE_LEFT] = app.packageName
            if (app.activityClassName != null) {
                prefs[APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT] = app.activityClassName
            } else {
                prefs.remove(APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT)
            }
            prefs[APP_USER_SWIPE_LEFT] = app.userString
        }
    }

    suspend fun setSwipeRightApp(app: AppPreference) {
        context.settingsDataStore.edit { prefs ->
            prefs[APP_NAME_SWIPE_RIGHT] = app.label
            prefs[APP_PACKAGE_SWIPE_RIGHT] = app.packageName
            if (app.activityClassName != null) {
                prefs[APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT] = app.activityClassName
            } else {
                prefs.remove(APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT)
            }
            prefs[APP_USER_SWIPE_RIGHT] = app.userString
        }
    }

    suspend fun getSwipeLeftApp(): AppPreference {
        return context.settingsDataStore.data.map { prefs ->
            AppPreference(
                label = prefs[APP_NAME_SWIPE_LEFT] ?: "Not set",
                packageName = prefs[APP_PACKAGE_SWIPE_LEFT] ?: "",
                activityClassName = prefs[APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT],
                userString = prefs[APP_USER_SWIPE_LEFT] ?: ""
            )
        }.first()
    }

    suspend fun getSwipeRightApp(): AppPreference {
        return context.settingsDataStore.data.map { prefs ->
            AppPreference(
                label = prefs[APP_NAME_SWIPE_RIGHT] ?: "Not set",
                packageName = prefs[APP_PACKAGE_SWIPE_RIGHT] ?: "",
                activityClassName = prefs[APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT],
                userString = prefs[APP_USER_SWIPE_RIGHT] ?: ""
            )
        }.first()
    }

    /**
     * Methods for managing clock and calendar apps
     */
    suspend fun setClockApp(app: AppPreference) {
        context.settingsDataStore.edit { prefs ->
            prefs[CLOCK_APP_PACKAGE] = app.packageName
            prefs[CLOCK_APP_USER] = app.userString
            if (app.activityClassName != null) {
                prefs[CLOCK_APP_CLASS_NAME] = app.activityClassName
            } else {
                prefs.remove(CLOCK_APP_CLASS_NAME)
            }
        }
    }

    suspend fun setCalendarApp(app: AppPreference) {
        context.settingsDataStore.edit { prefs ->
            prefs[CALENDAR_APP_PACKAGE] = app.packageName
            prefs[CALENDAR_APP_USER] = app.userString
            if (app.activityClassName != null) {
                prefs[CALENDAR_APP_CLASS_NAME] = app.activityClassName
            } else {
                prefs.remove(CALENDAR_APP_CLASS_NAME)
            }
        }
    }

    suspend fun getClockApp(): AppPreference {
        return context.settingsDataStore.data.map { prefs ->
            AppPreference(
                label = "Clock",
                packageName = prefs[CLOCK_APP_PACKAGE] ?: "",
                activityClassName = prefs[CLOCK_APP_CLASS_NAME],
                userString = prefs[CLOCK_APP_USER] ?: ""
            )
        }.first()
    }

    suspend fun getCalendarApp(): AppPreference {
        return context.settingsDataStore.data.map { prefs ->
            AppPreference(
                label = "Calendar",
                packageName = prefs[CALENDAR_APP_PACKAGE] ?: "",
                activityClassName = prefs[CALENDAR_APP_CLASS_NAME],
                userString = prefs[CALENDAR_APP_USER] ?: ""
            )
        }.first()
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
}