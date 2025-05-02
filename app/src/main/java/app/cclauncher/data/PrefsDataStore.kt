package app.cclauncher.data

import android.content.Context
import android.media.Image
import android.util.Log
import android.view.Gravity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json

// Extension property for Context to access the DataStore instance
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app.cclauncher")

/**
 * Data class representing all launcher preferences
 */
data class LauncherPreferences(
    val firstOpen: Boolean = true,
    val firstOpenTime: Long = 0L,
    val firstSettingsOpen: Boolean = true,
    val firstHide: Boolean = true,
    val userState: String = Constants.UserState.START,
    val lockMode: Boolean = false,
    val homeAppsNum: Int = 0,
    val homeScreenColumns: Int = 1,
    val showAppNames: Boolean = true,
    val showAppIcons: Boolean = false,
    val autoShowKeyboard: Boolean = true,
    val keyboardMessage: Boolean = false,
    val plainWallpaper: Boolean = false,
    val homeAlignment: Int = Gravity.CENTER,
    val homeBottomAlignment: Boolean = false,
    val appLabelAlignment: Int = Gravity.START,
    val statusBar: Boolean = false,
    val dateTimeVisibility: Int = Constants.DateTime.ON,
    val swipeLeftEnabled: Boolean = true,
    val swipeRightEnabled: Boolean = true,
    val hiddenApps: Set<String> = emptySet(),
    val hiddenAppsUpdated: Boolean = false,
    val showHintCounter: Int = 1,
    val appTheme: Int = AppCompatDelegate.MODE_NIGHT_YES,
    val aboutClicked: Boolean = false,
    val rateClicked: Boolean = false,
    val shareShownTime: Long = 0L,
    val swipeDownAction: Int = Constants.SwipeDownAction.NOTIFICATIONS,
    val textSizeScale: Float = 1.0f,
    val useSystemFont: Boolean = true,
    val useDynamicTheme : Boolean = false,
    val autoOpenFilteredApp: Boolean = true,
    val showHiddenAppsOnSearch: Boolean = false,
    val doubleTapToLock: Boolean = false,
    val externalWidgets: List<ExternalWidgetModel> = emptyList(),

    val searchType: Int = Constants.SearchType.CONTAINS,

    val homeApps: List<HomeAppPreference> = List(Constants.HomeAppCount.NUM) { HomeAppPreference() },

    val swipeLeftApp: AppPreference = AppPreference(label = "Camera"),
    val swipeRightApp: AppPreference = AppPreference(label = "Phone"),

    val clockApp: AppPreference = AppPreference(),
    val calendarApp: AppPreference = AppPreference()
)

data class HomeAppPreference(
    val label: String = "",
    val packageName: String = "",
    val activityClassName: String? = null,
    val userString: String = "",
    val icon: ImageBitmap? = null
)

data class AppPreference(
    val label: String = "",
    val packageName: String = "",
    val activityClassName: String? = null,
    val userString: String = ""
)

class PrefsDataStore(private val context: Context) {
    companion object {
        val FIRST_OPEN = booleanPreferencesKey("FIRST_OPEN")
        val FIRST_OPEN_TIME = longPreferencesKey("FIRST_OPEN_TIME")
        val FIRST_SETTINGS_OPEN = booleanPreferencesKey("FIRST_SETTINGS_OPEN")
        val FIRST_HIDE = booleanPreferencesKey("FIRST_HIDE")
        val USER_STATE = stringPreferencesKey("USER_STATE")
        val LOCK_MODE = booleanPreferencesKey("LOCK_MODE")
        val HOME_APPS_NUM = intPreferencesKey("HOME_APPS_NUM")
        val HOME_SCREEN_COLUMNS = intPreferencesKey("HOME_SCREEN_COLUMNS")
        val SHOW_APP_NAMES = booleanPreferencesKey("SHOW_APP_NAMES")
        val SHOW_APP_ICONS = booleanPreferencesKey("SHOW_APP_ICONS")
        val AUTO_SHOW_KEYBOARD = booleanPreferencesKey("AUTO_SHOW_KEYBOARD")
        val KEYBOARD_MESSAGE = booleanPreferencesKey("KEYBOARD_MESSAGE")
        val PLAIN_WALLPAPER = booleanPreferencesKey("PLAIN_WALLPAPER")
        val HOME_ALIGNMENT = intPreferencesKey("HOME_ALIGNMENT")
        val HOME_BOTTOM_ALIGNMENT = booleanPreferencesKey("HOME_BOTTOM_ALIGNMENT")
        val APP_LABEL_ALIGNMENT = intPreferencesKey("APP_LABEL_ALIGNMENT")
        val STATUS_BAR = booleanPreferencesKey("STATUS_BAR")
        val DATE_TIME_VISIBILITY = intPreferencesKey("DATE_TIME_VISIBILITY")
        val SWIPE_LEFT_ENABLED = booleanPreferencesKey("SWIPE_LEFT_ENABLED")
        val SWIPE_RIGHT_ENABLED = booleanPreferencesKey("SWIPE_RIGHT_ENABLED")
        val HIDDEN_APPS = stringSetPreferencesKey("HIDDEN_APPS")
        val HIDDEN_APPS_UPDATED = booleanPreferencesKey("HIDDEN_APPS_UPDATED")
        val SHOW_HINT_COUNTER = intPreferencesKey("SHOW_HINT_COUNTER")
        val APP_THEME = intPreferencesKey("APP_THEME")
        val ABOUT_CLICKED = booleanPreferencesKey("ABOUT_CLICKED")
        val RATE_CLICKED = booleanPreferencesKey("RATE_CLICKED")
        val SHARE_SHOWN_TIME = longPreferencesKey("SHARE_SHOWN_TIME")
        val SWIPE_DOWN_ACTION = intPreferencesKey("SWIPE_DOWN_ACTION")
        val TEXT_SIZE_SCALE = floatPreferencesKey("TEXT_SIZE_SCALE")
        val USE_SYSTEM_FONT = booleanPreferencesKey("USE_SYSTEM_FONT")
        val USE_DYNAMIC_THEME = booleanPreferencesKey("USE_DYNAMIC_THEME")
        val AUTO_OPEN_FILTERED_APP = booleanPreferencesKey("AUTO_OPEN_FILTERED_APP")
        val SHOW_HIDDEN_APPS_IN_SEARCH = booleanPreferencesKey("SHOW_HIDDEN_APPS_IN_SEARCH")
        val DOUBLE_TAP_TO_LOCK = booleanPreferencesKey("DOUBLE_TAP_TO_LOCK")
        val SEARCH_TYPE = intPreferencesKey("SEARCH_TYPE")

        val APP_NAME_KEYS = List(Constants.HomeAppCount.NUM) { stringPreferencesKey("APP_NAME_${it+1}") }
        val APP_PACKAGE_KEYS = List(Constants.HomeAppCount.NUM) { stringPreferencesKey("APP_PACKAGE_${it+1}") }
        val APP_ACTIVITY_CLASS_NAME_KEYS = List(Constants.HomeAppCount.NUM) { stringPreferencesKey("APP_ACTIVITY_CLASS_NAME_${it+1}") }
        val APP_USER_KEYS = List(Constants.HomeAppCount.NUM) { stringPreferencesKey("APP_USER_${it+1}") }
        
        val EXTERNAL_WIDGETS = stringPreferencesKey("EXTERNAL_WIDGETS")


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

        private val gson = Gson()
        private val widgetListType = object : TypeToken<List<ExternalWidgetModel>>() {}.type

    }

    val preferences: Flow<LauncherPreferences> = context.dataStore.data.map { prefs ->
        LauncherPreferences(
            firstOpen = prefs[FIRST_OPEN] != false,
            firstOpenTime = prefs[FIRST_OPEN_TIME] ?: 0L,
            firstSettingsOpen = prefs[FIRST_SETTINGS_OPEN] != false,
            firstHide = prefs[FIRST_HIDE] != false,
            userState = prefs[USER_STATE] ?: Constants.UserState.START,
            lockMode = prefs[LOCK_MODE] == true,
            homeAppsNum = prefs[HOME_APPS_NUM] ?: 0,
            homeScreenColumns = prefs[HOME_SCREEN_COLUMNS] ?: 1,
            showAppNames = prefs[SHOW_APP_NAMES] != false,
            showAppIcons = prefs[SHOW_APP_ICONS] == true,
            autoShowKeyboard = prefs[AUTO_SHOW_KEYBOARD] != false,
            keyboardMessage = prefs[KEYBOARD_MESSAGE] == true,
            plainWallpaper = prefs[PLAIN_WALLPAPER] == true,
            homeAlignment = prefs[HOME_ALIGNMENT] ?: Gravity.CENTER,
            homeBottomAlignment = prefs[HOME_BOTTOM_ALIGNMENT] == true,
            appLabelAlignment = prefs[APP_LABEL_ALIGNMENT] ?: Gravity.START,
            statusBar = prefs[STATUS_BAR] == true,
            dateTimeVisibility = prefs[DATE_TIME_VISIBILITY] ?: Constants.DateTime.ON,
            swipeLeftEnabled = prefs[SWIPE_LEFT_ENABLED] != false,
            swipeRightEnabled = prefs[SWIPE_RIGHT_ENABLED] != false,
            hiddenApps = prefs[HIDDEN_APPS] ?: emptySet(),
            hiddenAppsUpdated = prefs[HIDDEN_APPS_UPDATED] == true,
            showHintCounter = prefs[SHOW_HINT_COUNTER] ?: 1,
            appTheme = prefs[APP_THEME] ?: AppCompatDelegate.MODE_NIGHT_YES,
            aboutClicked = prefs[ABOUT_CLICKED] == true,
            rateClicked = prefs[RATE_CLICKED] == true,
            shareShownTime = prefs[SHARE_SHOWN_TIME] ?: 0L,
            swipeDownAction = prefs[SWIPE_DOWN_ACTION] ?: Constants.SwipeDownAction.NOTIFICATIONS,
            textSizeScale = prefs[TEXT_SIZE_SCALE] ?: 1.0f,
            useSystemFont = prefs[USE_SYSTEM_FONT] != false,
            useDynamicTheme = prefs[USE_DYNAMIC_THEME] == true,
            autoOpenFilteredApp = prefs[AUTO_OPEN_FILTERED_APP] != false,
            showHiddenAppsOnSearch = prefs[SHOW_HIDDEN_APPS_IN_SEARCH] == true,
            doubleTapToLock = prefs[DOUBLE_TAP_TO_LOCK] == true,
            searchType = prefs[SEARCH_TYPE] ?: Constants.SearchType.CONTAINS,

            homeApps = List(Constants.HomeAppCount.NUM) { i ->
                HomeAppPreference(
                    label = prefs[APP_NAME_KEYS[i]] ?: "",
                    packageName = prefs[APP_PACKAGE_KEYS[i]] ?: "",
                    activityClassName = prefs[APP_ACTIVITY_CLASS_NAME_KEYS[i]],
                    userString = prefs[APP_USER_KEYS[i]] ?: ""
                )
            },

            swipeLeftApp = AppPreference(
                label = prefs[APP_NAME_SWIPE_LEFT] ?: "Not set",
                packageName = prefs[APP_PACKAGE_SWIPE_LEFT] ?: "",
                activityClassName = prefs[APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT],
                userString = prefs[APP_USER_SWIPE_LEFT] ?: ""
            ),
            swipeRightApp = AppPreference(
                label = prefs[APP_NAME_SWIPE_RIGHT] ?: "Not set",
                packageName = prefs[APP_PACKAGE_SWIPE_RIGHT] ?: "",
                activityClassName = prefs[APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT],
                userString = prefs[APP_USER_SWIPE_RIGHT] ?: ""
            ),

            clockApp = AppPreference(
                label = "Clock",
                packageName = prefs[CLOCK_APP_PACKAGE] ?: "",
                activityClassName = prefs[CLOCK_APP_CLASS_NAME],
                userString = prefs[CLOCK_APP_USER] ?: ""
            ),
            calendarApp = AppPreference(
                label = "Calendar",
                packageName = prefs[CALENDAR_APP_PACKAGE] ?: "",
                activityClassName = prefs[CALENDAR_APP_CLASS_NAME],
                userString = prefs[CALENDAR_APP_USER] ?: ""
            ),
            externalWidgets = try {
                val widgetsString = prefs[EXTERNAL_WIDGETS]
                if (widgetsString.isNullOrEmpty()) {
                    emptyList()
                } else {
                    gson.fromJson<List<ExternalWidgetModel>>(widgetsString, widgetListType)
                }
            } catch (e: Exception) {
                Log.e("PrefsDataStore", "Error deserializing widgets: ${e.message}")
                emptyList()
            }
        )
    }.distinctUntilChanged()

    val firstOpen: Flow<Boolean> = preferences.map { it.firstOpen }
    val homeAppsNum: Flow<Int> = preferences.map { it.homeAppsNum }
    val homeScreenColumns: Flow<Int> = preferences.map { it.homeScreenColumns }
    val dateTimeVisibility: Flow<Int> = preferences.map { it.dateTimeVisibility }
    val homeAlignment: Flow<Int> = preferences.map { it.homeAlignment }
    val hiddenApps: Flow<Set<String>> = preferences.map { it.hiddenApps }
    val appTheme: Flow<Int> = preferences.map { it.appTheme }
    val textSizeScale: Flow<Float> = preferences.map { it.textSizeScale }
    val plainWallpaper: Flow<Boolean> = preferences.map { it.plainWallpaper }
    val useDynamicTheme: Flow<Boolean> = preferences.map { it.useDynamicTheme }



    suspend fun updatePreference(update: (LauncherPreferences) -> LauncherPreferences) {
        val currentPrefs = preferences.first()
        val updatedPrefs = update(currentPrefs)

        // update only changed values
        context.dataStore.edit { prefs ->
            if (currentPrefs.firstOpen != updatedPrefs.firstOpen)
                prefs[FIRST_OPEN] = updatedPrefs.firstOpen
            if (currentPrefs.firstOpenTime != updatedPrefs.firstOpenTime)
                prefs[FIRST_OPEN_TIME] = updatedPrefs.firstOpenTime
            if (currentPrefs.homeAppsNum != updatedPrefs.homeAppsNum)
                prefs[HOME_APPS_NUM] = updatedPrefs.homeAppsNum
            if (currentPrefs.homeScreenColumns != updatedPrefs.homeScreenColumns)
                prefs[HOME_SCREEN_COLUMNS] = updatedPrefs.homeScreenColumns
            if (currentPrefs.dateTimeVisibility != updatedPrefs.dateTimeVisibility)
                prefs[DATE_TIME_VISIBILITY] = updatedPrefs.dateTimeVisibility
            if (currentPrefs.homeAlignment != updatedPrefs.homeAlignment)
                prefs[HOME_ALIGNMENT] = updatedPrefs.homeAlignment
            if (currentPrefs.appTheme != updatedPrefs.appTheme)
                prefs[APP_THEME] = updatedPrefs.appTheme
            if (currentPrefs.textSizeScale != updatedPrefs.textSizeScale)
                prefs[TEXT_SIZE_SCALE] = updatedPrefs.textSizeScale
            if (currentPrefs.hiddenApps != updatedPrefs.hiddenApps)
                prefs[HIDDEN_APPS] = updatedPrefs.hiddenApps
            if (currentPrefs.showAppNames != updatedPrefs.showAppNames)
                prefs[SHOW_APP_NAMES] = updatedPrefs.showAppNames
            if (currentPrefs.showAppIcons != updatedPrefs.showAppIcons)
                prefs[SHOW_APP_ICONS] = updatedPrefs.showAppIcons
            if (currentPrefs.autoShowKeyboard != updatedPrefs.autoShowKeyboard)
                prefs[AUTO_SHOW_KEYBOARD] = updatedPrefs.autoShowKeyboard
            if (currentPrefs.useSystemFont != updatedPrefs.useSystemFont)
                prefs[USE_SYSTEM_FONT] = updatedPrefs.useSystemFont
            if (currentPrefs.useDynamicTheme != updatedPrefs.useDynamicTheme)
                prefs[USE_DYNAMIC_THEME] = updatedPrefs.useDynamicTheme
            if (currentPrefs.autoOpenFilteredApp != updatedPrefs.autoOpenFilteredApp)
                prefs[AUTO_OPEN_FILTERED_APP] = updatedPrefs.autoOpenFilteredApp
            if (currentPrefs.showHiddenAppsOnSearch != updatedPrefs.showHiddenAppsOnSearch)
                prefs[SHOW_HIDDEN_APPS_IN_SEARCH] = updatedPrefs.showHiddenAppsOnSearch
            if (currentPrefs.swipeLeftEnabled != updatedPrefs.swipeLeftEnabled)
                prefs[SWIPE_LEFT_ENABLED] = updatedPrefs.swipeLeftEnabled
            if (currentPrefs.swipeRightEnabled != updatedPrefs.swipeRightEnabled)
                prefs[SWIPE_RIGHT_ENABLED] = updatedPrefs.swipeRightEnabled
            if (currentPrefs.homeBottomAlignment != updatedPrefs.homeBottomAlignment)
                prefs[HOME_BOTTOM_ALIGNMENT] = updatedPrefs.homeBottomAlignment
            if (currentPrefs.statusBar != updatedPrefs.statusBar)
                prefs[STATUS_BAR] = updatedPrefs.statusBar
            if (currentPrefs.doubleTapToLock != updatedPrefs.doubleTapToLock)
                prefs[DOUBLE_TAP_TO_LOCK] = updatedPrefs.doubleTapToLock

            if (currentPrefs.searchType != updatedPrefs.searchType)
                prefs[SEARCH_TYPE] = updatedPrefs.searchType

            if (currentPrefs.externalWidgets != updatedPrefs.externalWidgets) {
                try {
                    val widgetsString = gson.toJson(updatedPrefs.externalWidgets, widgetListType)
                    prefs[EXTERNAL_WIDGETS] = widgetsString
                } catch (e: Exception) {
                    Log.e("PrefsDataStore", "Error serializing widgets: ${e.message}")
                }
            }



                currentPrefs.homeApps.forEachIndexed { i, oldApp ->
                val newApp = updatedPrefs.homeApps[i]
                if (oldApp != newApp) {
                    prefs[APP_NAME_KEYS[i]] = newApp.label
                    prefs[APP_PACKAGE_KEYS[i]] = newApp.packageName
                    if (newApp.activityClassName != null) {
                        prefs[APP_ACTIVITY_CLASS_NAME_KEYS[i]] = newApp.activityClassName
                    } else {
                        prefs.remove(APP_ACTIVITY_CLASS_NAME_KEYS[i])
                    }
                    prefs[APP_USER_KEYS[i]] = newApp.userString
                }
            }

            if (currentPrefs.swipeLeftApp != updatedPrefs.swipeLeftApp) {
                prefs[APP_NAME_SWIPE_LEFT] = updatedPrefs.swipeLeftApp.label
                prefs[APP_PACKAGE_SWIPE_LEFT] = updatedPrefs.swipeLeftApp.packageName
                if (updatedPrefs.swipeLeftApp.activityClassName != null) {
                    prefs[APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT] = updatedPrefs.swipeLeftApp.activityClassName
                } else {
                    prefs.remove(APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT)
                }
                prefs[APP_USER_SWIPE_LEFT] = updatedPrefs.swipeLeftApp.userString
            }

            if (currentPrefs.swipeRightApp != updatedPrefs.swipeRightApp) {
                prefs[APP_NAME_SWIPE_RIGHT] = updatedPrefs.swipeRightApp.label
                prefs[APP_PACKAGE_SWIPE_RIGHT] = updatedPrefs.swipeRightApp.packageName
                if (updatedPrefs.swipeRightApp.activityClassName != null) {
                    prefs[APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT] = updatedPrefs.swipeRightApp.activityClassName
                } else {
                    prefs.remove(APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT)
                }
                prefs[APP_USER_SWIPE_RIGHT] = updatedPrefs.swipeRightApp.userString
            }

            if (currentPrefs.clockApp != updatedPrefs.clockApp) {
                prefs[CLOCK_APP_PACKAGE] = updatedPrefs.clockApp.packageName
                prefs[CLOCK_APP_USER] = updatedPrefs.clockApp.userString
                if (updatedPrefs.clockApp.activityClassName != null) {
                    prefs[CLOCK_APP_CLASS_NAME] = updatedPrefs.clockApp.activityClassName
                } else {
                    prefs.remove(CLOCK_APP_CLASS_NAME)
                }
            }

            if (currentPrefs.calendarApp != updatedPrefs.calendarApp) {
                prefs[CALENDAR_APP_PACKAGE] = updatedPrefs.calendarApp.packageName
                prefs[CALENDAR_APP_USER] = updatedPrefs.calendarApp.userString
                if (updatedPrefs.calendarApp.activityClassName != null) {
                    prefs[CALENDAR_APP_CLASS_NAME] = updatedPrefs.calendarApp.activityClassName
                } else {
                    prefs.remove(CALENDAR_APP_CLASS_NAME)
                }
            }
        }
    }

    suspend fun setFirstOpen(value: Boolean) {
        updatePreference { it.copy(firstOpen = value) }
    }

    suspend fun setFirstOpenTime(value: Long) {
        updatePreference { it.copy(firstOpenTime = value) }
    }

    suspend fun setHomeAppsNum(value: Int) {
        updatePreference { it.copy(homeAppsNum = value) }
    }

    suspend fun setDateTimeVisibility(value: Int) {
        updatePreference { it.copy(dateTimeVisibility = value) }
    }

    suspend fun setHomeAlignment(value: Int) {
        updatePreference { it.copy(homeAlignment = value) }
    }

    suspend fun setAppTheme(value: Int) {
        updatePreference { it.copy(appTheme = value) }
    }

    suspend fun setTextSizeScale(value: Float) {
        updatePreference { it.copy(textSizeScale = value) }
    }

    suspend fun setHiddenApps(value: Set<String>) {
        updatePreference { it.copy(hiddenApps = value) }
    }

    suspend fun setHomeApp(position: Int, app: HomeAppPreference) {
//            println("Setting home app at position: $position for app: ${app.packageName}")

        updatePreference {
            val newHomeApps = it.homeApps.toMutableList()

            while (newHomeApps.size <= position) {
                newHomeApps.add(HomeAppPreference())
            }
            newHomeApps[position] = app

            it.copy(homeApps = newHomeApps)
        }
    }

    suspend fun setHomeScreenColumns(value: Int) {

        updatePreference { it.copy(homeScreenColumns = value) }
    }

    suspend fun setSwipeLeftApp(app: AppPreference) {
        updatePreference { it.copy(swipeLeftApp = app) }
    }

    suspend fun setSwipeRightApp(app: AppPreference) {
        updatePreference { it.copy(swipeRightApp = app) }
    }

    suspend fun setClockApp(app: AppPreference) {
        updatePreference { it.copy(clockApp = app) }
    }

    suspend fun setCalendarApp(app: AppPreference) {
        updatePreference { it.copy(calendarApp = app) }
    }

    suspend fun setShowAppNames(value: Boolean) {
        updatePreference { it.copy(showAppNames = value) }
    }

    suspend fun setShowAppIcons(value: Boolean) {
        updatePreference { it.copy(showAppIcons = value) }
    }


    suspend fun setAutoShowKeyboard(value: Boolean) {
        updatePreference { it.copy(autoShowKeyboard = value) }
    }

    suspend fun setUseSystemFont(value: Boolean) {
        updatePreference { it.copy(useSystemFont = value) }
    }

    suspend fun setUseDynamicTheme(value: Boolean) {
        updatePreference { it.copy(useDynamicTheme = value) }
    }

    suspend fun setAutoOpenFilteredApp(value: Boolean) {
        updatePreference { it.copy(autoOpenFilteredApp = value) }
    }

    suspend fun setShowHiddenAppsOnSearch(value: Boolean) {
        updatePreference { it.copy(showHiddenAppsOnSearch = value) }
    }

    suspend fun setHomeBottomAlignment(value: Boolean) {
        updatePreference { it.copy(homeBottomAlignment = value) }
    }

    suspend fun setStatusBar(value: Boolean) {
        updatePreference { it.copy(statusBar = value) }
    }

    suspend fun setSwipeLeftEnabled(value: Boolean) {
        updatePreference { it.copy(swipeLeftEnabled = value) }
    }

    suspend fun setSwipeRightEnabled(value: Boolean) {
        updatePreference { it.copy(swipeRightEnabled = value) }
    }

    suspend fun setSwipeDownAction(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[SWIPE_DOWN_ACTION] = value
        }
//        val updatedValue = context.dataStore.data.first()[SWIPE_DOWN_ACTION]
//        println("DEBUG: Set swipe down action to $value, stored value is $updatedValue")
    }

    suspend fun setDoubleTapToLock(enabled: Boolean) {
        updatePreference { it.copy(doubleTapToLock = enabled) }
    }

    suspend fun setSearchType(value: Int) {
        updatePreference { it.copy(searchType = value) }
    }





    suspend fun toggleAppHidden(packageKey: String) {
        updatePreference {
            val updatedHiddenApps = it.hiddenApps.toMutableSet()
            if (updatedHiddenApps.contains(packageKey)) {
                updatedHiddenApps.remove(packageKey)
            } else {
                updatedHiddenApps.add(packageKey)
            }
            it.copy(hiddenApps = updatedHiddenApps)
        }
    }

    suspend fun addExternalWidget(widget: ExternalWidgetModel) {
        updatePreference { prefs ->
            val updatedWidgets = prefs.externalWidgets.toMutableList()
            updatedWidgets.add(widget)
            prefs.copy(externalWidgets = updatedWidgets)
        }
    }

    suspend fun updateExternalWidget(widget: ExternalWidgetModel) {
        updatePreference { prefs ->
            val updatedWidgets = prefs.externalWidgets.toMutableList()
            val index = updatedWidgets.indexOfFirst { it.id == widget.id }
            if (index != -1) {
                updatedWidgets[index] = widget
            }
            prefs.copy(externalWidgets = updatedWidgets)
        }
    }

    suspend fun removeExternalWidget(widgetId: String) {
        updatePreference { prefs ->
            val updatedWidgets = prefs.externalWidgets.filter { it.id != widgetId }
            prefs.copy(externalWidgets = updatedWidgets)
        }
    }


}

