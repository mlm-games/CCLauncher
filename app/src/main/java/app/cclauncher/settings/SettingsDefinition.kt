package app.cclauncher.settings

import androidx.appcompat.app.AppCompatDelegate
import app.cclauncher.data.Constants
import kotlinx.serialization.Serializable
import app.cclauncher.data.HomeLayout
import io.github.mlmgames.settings.core.annotations.CategoryDefinition
import io.github.mlmgames.settings.core.annotations.Persisted
import io.github.mlmgames.settings.core.annotations.SchemaVersion
import io.github.mlmgames.settings.core.annotations.Serialized
import io.github.mlmgames.settings.core.annotations.Setting
import io.github.mlmgames.settings.core.types.Button
import io.github.mlmgames.settings.core.types.Dropdown
import io.github.mlmgames.settings.core.types.SettingTypeMarker
import io.github.mlmgames.settings.core.types.Slider
import io.github.mlmgames.settings.core.types.Toggle

@SchemaVersion(1)
data class AppSettings(

    @Setting(
        title = "Show App Names",
        category = General::class,
        type = Toggle::class,
        key = "SHOW_APP_NAMES",
    )
    val showAppNames: Boolean = false,

    @Setting(
        title = "Show Names in Search After",
        description = "Show app names in search results after typing this many characters. Set to 0 to use the 'Show App Names' setting instead.",
        category = General::class,
        type = Slider::class,
        min = 0f,
        max = 7f,
        step = 1f,
        key = "SHOW_APP_NAMES_IN_SEARCH_AFTER",
    )
    val showAppNamesInSearchAfter: Int = 0,

    @Setting(
        title = "Show Pinned Shortcuts",
        description = "Display pinned app shortcuts in the app drawer.",
        category = General::class,
        type = Toggle::class,
        key = "SHOW_PINNED_SHORTCUTS",
    )
    val showPinnedShortcuts: Boolean = true,

    @Setting(
        title = "Show App Drawer Icons",
        category = General::class,
        type = Toggle::class,
        key = "SHOW_APP_ICONS")
    val showAppIcons: Boolean = true,

    @Setting(
        title = "Auto Show Keyboard",
        category = General::class,
        type = Toggle::class,
        key = "AUTO_SHOW_KEYBOARD",
    )
    val autoShowKeyboard: Boolean = true,

    @Setting(
        title = "Show Hidden in Search",
        category = General::class,
        type = Toggle::class,
        key = "SHOW_HIDDEN_APPS_IN_SEARCH",
    )
    val showHiddenAppsOnSearch: Boolean = false,

    @Setting(
        title = "Auto Open Single Matches",
        category = General::class,
        type = Toggle::class,
        key = "AUTO_OPEN_FILTERED_APP",
    )
    val autoOpenFilteredApp: Boolean = true,

    @Setting(
        title = "Search Type",
        category = General::class,
        type = Dropdown::class,
        options = ["Contains", "Fuzzy Match", "Starts With", "Exact Match"],
        key = "SEARCH_TYPE",
    )
    val searchType: Int = Constants.SearchType.CONTAINS,

    @Setting(
        title = "Return to Home After App",
        description = "Return to home screen instead of search after closing an app",
        category = General::class,
        type = Toggle::class,
        key = "RETURN_TO_HOME_AFTER_APP",
    )
    val returnToHomeAfterApp: Boolean = false,

    @Setting(
        title = "Default Screen",
        description = "Choose which screen to show when opening the launcher",
        category = General::class,
        type = Dropdown::class,
        options = ["Home", "App Drawer"],
        key = "DEFAULT_SCREEN",
    )
    val defaultScreen: Int = 0,

    @Setting(
        title = "Search Sort Order",
        category = General::class,
        type = Dropdown::class,
        options = ["Alphabetical", "Reverse Alpha", "Last Launch"],
        key = "SEARCH_SORT_ORDER",
    )
    val searchSortOrder: Int = Constants.SortOrder.ALPHABETICAL,

    @Setting(
        title = "Search Aliases",
        description = "Match app names across transliterations and keyboard layouts (e.g. Африка ↔ Afrika). May slightly increase CPU use on older devices.",
        category = General::class,
        type = Dropdown::class,
        options = ["Off", "Transliteration", "Keyboard layout swap", "Both"],
        key = "SEARCH_ALIASES_MODE",
    )
    val searchAliasesMode: Int = 0,

    @Persisted(key = "SEARCH_INCLUDE_PACKAGE_NAMES")
    val searchIncludePackageNames: Boolean = false,

    @Setting(
        title = "Theme",
        category = Appearance::class,
        type = Dropdown::class,
        options = ["System", "Light", "Dark"],
        key = "APP_THEME",
    )
    val appTheme: Int = AppCompatDelegate.MODE_NIGHT_YES,

    @Setting(
        title = "Home Text Size",
        category = Appearance::class,
        type = Slider::class,
        min = 0.5f,
        max = 2.0f,
        step = 0.1f,
        key = "TEXT_SIZE_SCALE",
    )
    val textSizeScale: Float = 1.0f,

    @Persisted(key = "ANIMATION_SPEED")
    val animationSpeed: Float = 1.0f,

    @Setting(
        title = "Font Weight",
        category = Appearance::class,
        type = Dropdown::class,
        options = ["Thin", "Light", "Normal", "Medium", "Bold", "Black"],
        key = "FONT_WEIGHT",
    )
    val fontWeight: Int = 2,

    @Setting(
        title = "Use System Font",
        category = Appearance::class,
        type = Toggle::class,
        key = "USE_SYSTEM_FONT",
    )
    val useSystemFont: Boolean = true,

    @Setting(
        title = "Custom Font",
        description = "Select a custom font file",
        category = Appearance::class,
        type = FontPicker::class,
        key = "CUSTOM_FONT_PATH",
    )
    val customFontPath: String = "",

    @Setting(
        title = "Use Dynamic Theme",
        category = Appearance::class,
        type = Toggle::class,
        key = "USE_DYNAMIC_THEME",
    )
    val useDynamicTheme: Boolean = false,

    @Setting(
        title = "Screen Orientation",
        type = Dropdown::class,
        options = ["System Default", "Force Portrait", "Force Landscape"],
        category = Appearance::class
    )
    var screenOrientation: Int = 0,

    @Setting(
        title = "Item Spacing",
        category = Appearance::class,
        type = Dropdown::class,
        options = ["None", "Small", "Medium", "Large"]
    )
    val itemSpacing: Int = 1,

    @Setting(
        title = "Search Results Use Home Font Size",
        category = Appearance::class,
        type = Toggle::class,
        description = "Use the same font size for search results as home screen"
    )
    val searchResultsUseHomeFont: Boolean = false,

    @Setting(
        title = "Search Results Font Size",
        category = Appearance::class,
        type = Slider::class,
        min = 0.5f,
        max = 2.0f,
        step = 0.1f
    )
    val searchResultsFontSize: Float = 1.0f,

    @Setting(
        title = "Icon Corner Radius",
        category = Appearance::class,
        type = Slider::class,
        min = 0f,
        max = 50f,
        step = 1f,
        key = "ICON_CORNER_RADIUS",
    )
    val iconCornerRadius: Int = 0,

    @Setting(
        title = "Text Color",
        description = "Customize text color for better visibility",
        category = Appearance::class,
        type = ColorPicker::class,
        key = "TEXT_COLOR",
    )
    val textColor: Int = 0,

    @Setting(
        title = "Use Custom Text Color",
        description = "Override theme text color with custom color",
        category = Appearance::class,
        type = Toggle::class,
        key = "USE_CUSTOM_TEXT_COLOR",
    )
    val useCustomTextColor: Boolean = false,

    @Setting(
        title = "Icon Pack",
        category = Appearance::class,
        type = IconPackPicker::class,
        description = "Choose custom icon pack for apps",
        key = "SELECTED_ICON_PACK",
    )
    val selectedIconPack: String = "default",

    // TODO: This is an action
    @Setting(
        title = "Set Plain Wallpaper",
        description = "Set a plain black/white wallpaper based on theme",
        category = Appearance::class,
        type = Button::class,
        key = "PLAIN_WALLPAPER",
    )
    val plainWallpaper: Boolean = false,

    @Setting(
        title = "Long Press in App Drawer",
        description = "Long press on apps shows options menu",
        category = Gestures::class,
        type = Toggle::class,
        key = "APP_DRAWER_LONG_PRESS_ENABLED",
    )
    val appDrawerLongPressEnabled: Boolean = true,

    @Setting(
        title = "Auto Update Wallpaper",
        description = "Automatically update plain wallpaper when system theme changes",
        category = Appearance::class,
        type = Toggle::class,
        key = "AUTO_UPDATE_WALLPAPER",
    )
    val autoUpdateWallpaper: Boolean = false,

    @Setting(
        title = "Show Status Bar",
        category = Layout::class,
        type = Toggle::class,
        key = "STATUS_BAR",
    )
    val statusBar: Boolean = false,

    @Setting(
        title = "Tap to Open in App Drawer",
        description = "When disabled, tapping an app drawer app does nothing. Long‑press still opens the menu.",
        category = General::class,
        type = Toggle::class,
        key = "APP_DRAWER_TAP_TO_OPEN",
    )
    val appDrawerTapToOpen: Boolean = true,

    @Setting(
        title = "Scale Home Apps",
        category = Layout::class,
        type = Toggle::class,
        key = "SCALE_HOME_APPS",
    )
    val scaleHomeApps: Boolean = true,

    @Setting(
        title = "Show Web Search Option",
        description = "Show 'Search Web' button when no apps match",
        category = General::class,
        type = Toggle::class,
        key = "SHOW_WEB_SEARCH_OPTION",
    )
    val showWebSearchOption: Boolean = true,

    @Setting(
        title = "Home Screen Rows",
        description = "Number of rows in the home screen grid",
        category = Layout::class,
        type = Slider::class,
        min = 4f,
        max = 12f,
        step = 1f,
        key = "HOME_SCREEN_ROWS",
    )
    val homeScreenRows: Int = 8,

    @Setting(
        title = "Home Screen Columns",
        description = "Number of columns in the home screen grid",
        category = Layout::class,
        type = Slider::class,
        min = 2f,
        max = 8f,
        step = 1f,
        key = "HOME_SCREEN_COLUMNS",
    )
    val homeScreenColumns: Int = 4,

    @Setting(
        title = "Home Screen Pages",
        description = "Number of home screen pages",
        category = Layout::class,
        type = Slider::class,
        min = 1f,
        max = 5f,
        step = 1f,
        key = "HOME_SCREEN_PAGES",
    )
    val homeScreenPages: Int = 1,

    @Setting(
        title = "Show Page Indicator",
        description = "Show page dots at the bottom of the home screen",
        category = Layout::class,
        type = Toggle::class,
        key = "SHOW_PAGE_INDICATOR",
    )
    val showPageIndicator: Boolean = true,

    @Setting(
        title = "Show App Icons on Home Screen",
        description = "Display app icons on the home screen",
        category = Appearance::class,
        type = Toggle::class,
        key = "SHOW_HOME_SCREEN_ICONS",
    )
    val showHomeScreenIcons: Boolean = false,

    @Setting(
        title = "Show App Icons in Landscape",
        category = Layout::class,
        type = Toggle::class,
        key = "SHOW_ICONS_IN_LANDSCAPE",
//        dependsOn = "showAppIcons"
    )
    val showIconsInLandscape: Boolean = false,

    @Setting(
        title = "Show App Icons in Portrait",
        category = Layout::class,
        type = Toggle::class,
        key = "SHOW_ICONS_IN_PORTRAIT",
//        dependsOn = "showAppIcons"
    )
    val showIconsInPortrait: Boolean = false,

    @Setting(
        title = "Home App Label Alignment",
        description = "Align app names on the home screen",
        category = Appearance::class,
        type = Dropdown::class,
        options = ["Left", "Center", "Right"],
        key = "APP_LABEL_ALIGNMENT",
    )
    val appLabelAlignment: Int = 0,

    @Setting(
        title = "Gesture Sensitivity",
        description = "Adjust how easily swipe gestures are triggered",
        category = Gestures::class,
        type = Slider::class,
        min = 0.1f,
        max = 2.0f,
        step = 0.1f,
        key = "GESTURE_SENSITIVITY",
    )
    val gestureSensitivity: Float = 1.0f,

    @Setting(
        title = "Double Tap to Lock Screen",
        category = Gestures::class,
        type = Toggle::class,
        key = "DOUBLE_TAP_TO_LOCK",
    )
    val doubleTapToLock: Boolean = false,

    @Setting(
        title = "Swipe Down Action",
        category = Gestures::class,
        type = Dropdown::class,
        options = ["None", "Search", "Notifications", "App", "Next Page", "Previous Page"],
        key = "SWIPE_DOWN_ACTION",
    )
    val swipeDownAction: Int = Constants.SwipeAction.NOTIFICATIONS,

    @Setting(
        title = "Swipe Down App",
        category = Gestures::class,
        type = AppPicker::class,
        key = "SWIPE_DOWN_APP_JSON",
    )
    @Serialized
    val swipeDownApp: AppPreference = AppPreference(),

    @Setting(
        title = "Swipe Up Action",
        category = Gestures::class,
        type = Dropdown::class,
        options = ["None", "Search", "Notifications", "App", "Next Page", "Previous Page"],
        key = "SWIPE_UP_ACTION",
    )
    val swipeUpAction: Int = Constants.SwipeAction.SEARCH,

    @Setting(
        title = "Swipe Up App",
        category = Gestures::class,
        type = AppPicker::class,
        key = "SWIPE_UP_APP_JSON",
    )
    @Serialized
    val swipeUpApp: AppPreference = AppPreference(),

    @Setting(
        title = "Swipe Left Action",
        category = Gestures::class,
        type = Dropdown::class,
        options = ["None", "Search", "Notifications", "App", "Next Page", "Previous Page"],
        key = "SWIPE_LEFT_ACTION",
    )
    val swipeLeftAction: Int = Constants.SwipeAction.NULL, // Remain until needed?

    @Setting(
        title = "Left Swipe App",
        category = Gestures::class,
        type = AppPicker::class,
        key = "SWIPE_LEFT_APP_JSON",
    )
    @Serialized
    val swipeLeftApp: AppPreference = AppPreference(label = "Not set"),

    @Setting(
        title = "Swipe Right Action",
        category = Gestures::class,
        type = Dropdown::class,
        options = ["None", "Search", "Notifications", "App", "Next Page", "Previous Page"],
        key = "SWIPE_RIGHT_ACTION",
    )
    val swipeRightAction: Int = Constants.SwipeAction.NULL,

    @Setting(
        title = "Right Swipe App",
        category = Gestures::class,
        type = AppPicker::class,
        key = "SWIPE_RIGHT_APP_JSON",
    )
    @Serialized
    val swipeRightApp: AppPreference = AppPreference(label = "Not set"),

    @Persisted(key = "FIRST_OPEN") val firstOpen: Boolean = true,
    @Persisted(key = "FIRST_OPEN_TIME") val firstOpenTime: Long = 0L,
    @Persisted(key = "FIRST_SETTINGS_OPEN") val firstSettingsOpen: Boolean = true,
    @Persisted(key = "FIRST_HIDE") val firstHide: Boolean = true,
    @Persisted(key = "USER_STATE") val userState: String = Constants.UserState.START,
    @Persisted(key = "LOCK_MODE") val lockMode: Boolean = false,
    @Persisted(key = "KEYBOARD_MESSAGE") val keyboardMessage: Boolean = false,

    // These were JSON strings before; kmp-settings has Map fields that will read/write the same JSON format.
    @Persisted(key = "RENAMED_APPS_JSON") val renamedApps: Map<String, String> = emptyMap(),
    @Persisted(key = "RECENT_APP_HISTORY") val recentAppHistory: Map<String, Long> = emptyMap(),

    @Persisted(key = "HIDDEN_APPS") val hiddenApps: Set<String> = emptySet(),
    @Persisted(key = "HIDDEN_APPS_UPDATED") val hiddenAppsUpdated: Boolean = false,

    @Persisted(key = "SHOW_HINT_COUNTER") val showHintCounter: Int = 1,
    @Persisted(key = "ABOUT_CLICKED") val aboutClicked: Boolean = false,
    @Persisted(key = "RATE_CLICKED") val rateClicked: Boolean = false,
    @Persisted(key = "SHARE_SHOWN_TIME") val shareShownTime: Long = 0L,

    @Persisted(key = "ACCESSIBILITY_CONSENT") val accessibilityConsent: Boolean = false,

    // Settings lock is currently implemented in your app; keep persisted for now
    @Persisted(key = "LOCK_SETTINGS") val lockSettings: Boolean = false,
    @Persisted(key = "SETTINGS_LOCK_PIN") val settingsLockPin: String = "",

    // Home layout: move into kmp-settings (requires HomeLayout to be @Serializable)
    @Persisted(key = "HOME_LAYOUT_JSON")
    @Serialized
    val homeLayout: HomeLayout = HomeLayout(),
)

@Serializable
data class AppPreference(
    val label: String = "",
    val packageName: String = "",
    val activityClassName: String? = null,
    val userString: String = "",
    val isSystemShortcut: Boolean = false,
    val systemShortcutId: String? = null,
    val systemShortcutPackage: String? = null
)

data class AppKeyMigration(
    val newKey: String,
    val moveKeys: Set<String> = emptySet(),
    val copyKeys: Set<String> = emptySet()
)
@CategoryDefinition(order = 0) object General
@CategoryDefinition(order = 1) object Appearance
@CategoryDefinition(order = 2) object Layout
@CategoryDefinition(order = 3) object Gestures
@CategoryDefinition(order = 4) object System

object FontPicker : SettingTypeMarker
object AppPicker : SettingTypeMarker
object IconPackPicker : SettingTypeMarker
object ColorPicker : SettingTypeMarker
