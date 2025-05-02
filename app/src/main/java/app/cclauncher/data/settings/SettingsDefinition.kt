package app.cclauncher.data.settings

import android.view.Gravity
import androidx.appcompat.app.AppCompatDelegate
import app.cclauncher.data.Constants

/**
 * Enum of setting types used for UI generation
 */
enum class SettingType {
    TOGGLE,
    SLIDER,
    DROPDOWN,
    BUTTON,
    COLOR_PICKER
}

/**
 * Categories for organizing settings
 */
enum class SettingCategory {
    GENERAL,
    APPEARANCE,
    LAYOUT,
    GESTURES,
    SYSTEM
}

/**
 * Annotation to provide metadata for settings
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Setting(
    val title: String,
    val description: String = "",
    val category: SettingCategory,
    val type: SettingType,
    val dependsOn: String = "",
    val min: Float = 0f,
    val max: Float = 100f,
    val step: Float = 1f,
    val options: Array<String> = []
)

/**
 * Central data class for all application settings
 */
data class AppSettings(
    // General settings
    @Setting(
        title = "Home Apps Number",
        category = SettingCategory.GENERAL,
        type = SettingType.SLIDER,
        min = 0f,
        max = 16f
    )
    val homeAppsNum: Int = 0,

    @Setting(
        title = "Show App Names",
        category = SettingCategory.GENERAL,
        type = SettingType.TOGGLE
    )
    val showAppNames: Boolean = true,

    @Setting(
        title = "Show App Icons",
        category = SettingCategory.GENERAL,
        type = SettingType.TOGGLE
    )
    val showAppIcons: Boolean = false,

    @Setting(
        title = "Auto Show Keyboard",
        category = SettingCategory.GENERAL,
        type = SettingType.TOGGLE
    )
    val autoShowKeyboard: Boolean = true,

    @Setting(
        title = "Show Hidden in Search",
        category = SettingCategory.GENERAL,
        type = SettingType.TOGGLE
    )
    val showHiddenAppsOnSearch: Boolean = false,

    @Setting(
        title = "Auto Open Single Matches",
        category = SettingCategory.GENERAL,
        type = SettingType.TOGGLE
    )
    val autoOpenFilteredApp: Boolean = true,

    @Setting(
        title = "Search Type",
        category = SettingCategory.GENERAL,
        type = SettingType.DROPDOWN,
        options = ["Contains", "Fuzzy Match", "Starts With"]
    )
    val searchType: Int = Constants.SearchType.CONTAINS,

    // Appearance settings
    @Setting(
        title = "Theme",
        category = SettingCategory.APPEARANCE,
        type = SettingType.DROPDOWN,
        options = ["System", "Light", "Dark"]
    )
    val appTheme: Int = AppCompatDelegate.MODE_NIGHT_YES,

    @Setting(
        title = "Text Size",
        category = SettingCategory.APPEARANCE,
        type = SettingType.SLIDER,
        min = 0.5f,
        max = 2.0f,
        step = 0.1f
    )
    val textSizeScale: Float = 1.0f,

    @Setting(
        title = "Font Weight",
        category = SettingCategory.APPEARANCE,
        type = SettingType.DROPDOWN,
        options = ["Thin", "Light", "Normal", "Medium", "Bold", "Black"]
    )
    val fontWeight: Int = 2, // Normal by default

    @Setting(
        title = "Use System Font",
        category = SettingCategory.APPEARANCE,
        type = SettingType.TOGGLE
    )
    val useSystemFont: Boolean = true,

    @Setting(
        title = "Use Dynamic Theme",
        category = SettingCategory.APPEARANCE,
        type = SettingType.TOGGLE
    )
    val useDynamicTheme: Boolean = false,

    @Setting(
        title = "Icon Corner Radius",
        category = SettingCategory.APPEARANCE,
        type = SettingType.SLIDER,
        min = 0f,
        max = 50f
    )
    val iconCornerRadius: Int = 8,

    @Setting(
        title = "Item Spacing",
        category = SettingCategory.APPEARANCE,
        type = SettingType.DROPDOWN,
        options = ["None", "Small", "Medium", "Large"]
    )
    val itemSpacing: Int = 1, // Small by default

    // Layout settings
    @Setting(
        title = "Alignment",
        category = SettingCategory.LAYOUT,
        type = SettingType.DROPDOWN,
        options = ["Left", "Center", "Right"]
    )
    val homeAlignment: Int = Gravity.CENTER,

    @Setting(
        title = "Bottom Alignment",
        category = SettingCategory.LAYOUT,
        type = SettingType.TOGGLE
    )
    val homeBottomAlignment: Boolean = false,

    @Setting(
        title = "Show Status Bar",
        category = SettingCategory.LAYOUT,
        type = SettingType.TOGGLE
    )
    val statusBar: Boolean = false,

    @Setting(
        title = "Number of Columns",
        category = SettingCategory.LAYOUT,
        type = SettingType.SLIDER,
        min = 1f,
        max = 4f
    )
    val homeScreenColumns: Int = 1,

    @Setting(
        title = "Date & Time",
        category = SettingCategory.LAYOUT,
        type = SettingType.DROPDOWN,
        options = ["Off", "Date Only", "On"]
    )
    val dateTimeVisibility: Int = Constants.DateTime.ON,

    @Setting(
        title = "Force Landscape Mode",
        category = SettingCategory.LAYOUT,
        type = SettingType.TOGGLE,
        description = "Force landscape orientation on smartphones"
    )
    val forceLandscapeMode: Boolean = false,

    @Setting(
        title = "Show App Icons on Home Screen",
        category = SettingCategory.APPEARANCE,
        type = SettingType.TOGGLE,
        description = "Display app icons on the home screen"
    )
    val showHomeScreenIcons: Boolean = false,


    @Setting(
        title = "Show Home App Icons in Landscape",
        category = SettingCategory.LAYOUT,
        type = SettingType.TOGGLE,
        dependsOn = "showHomeScreenIcons"
    )
    val showIconsInLandscape: Boolean = false,

    @Setting(
        title = "Show Home App Icons in Portrait",
        category = SettingCategory.LAYOUT,
        type = SettingType.TOGGLE,
        dependsOn = "showHomeScreenIcons"
    )
    val showIconsInPortrait: Boolean = false,

    // Gestures settings
    @Setting(
        title = "Left Swipe Gesture",
        category = SettingCategory.GESTURES,
        type = SettingType.TOGGLE
    )
    val swipeLeftEnabled: Boolean = true,

    @Setting(
        title = "Right Swipe Gesture",
        category = SettingCategory.GESTURES,
        type = SettingType.TOGGLE
    )
    val swipeRightEnabled: Boolean = true,

    @Setting(
        title = "Swipe Down Action",
        category = SettingCategory.GESTURES,
        type = SettingType.DROPDOWN,
        options = ["Notifications", "Search"]
    )
    val swipeDownAction: Int = Constants.SwipeDownAction.NOTIFICATIONS,

    @Setting(
        title = "Double Tap to Lock Screen",
        category = SettingCategory.GESTURES,
        type = SettingType.TOGGLE
    )
    val doubleTapToLock: Boolean = false,

    // Other properties not directly exposed in UI but part of settings
    val firstOpen: Boolean = true,
    val firstOpenTime: Long = 0L,
    val firstSettingsOpen: Boolean = true,
    val firstHide: Boolean = true,
    val userState: String = Constants.UserState.START,
    val lockMode: Boolean = false,
    val keyboardMessage: Boolean = false,
    val plainWallpaper: Boolean = false,
    val appLabelAlignment: Int = Gravity.START,
    val hiddenApps: Set<String> = emptySet(),
    val hiddenAppsUpdated: Boolean = false,
    val showHintCounter: Int = 1,
    val aboutClicked: Boolean = false,
    val rateClicked: Boolean = false,
    val shareShownTime: Long = 0L,

    // Search result appearance settings
    @Setting(
        title = "Search Results Use Home Font Size",
        category = SettingCategory.APPEARANCE,
        type = SettingType.TOGGLE,
        description = "Use the same font size for search results as home screen"
    )
    val searchResultsUseHomeFont: Boolean = false,

    @Setting(
        title = "Search Results Font Size",
        category = SettingCategory.APPEARANCE,
        type = SettingType.SLIDER,
        min = 0.5f,
        max = 2.0f,
        step = 0.1f,
//        dependsOn = "searchResultsUseHomeFont"
    )
    val searchResultsFontSize: Float = 1.0f
) {
    companion object {
        // Helper method to get default settings
        fun getDefault(): AppSettings = AppSettings()
    }
}