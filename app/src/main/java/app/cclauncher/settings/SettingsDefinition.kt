package app.cclauncher.settings

import androidx.appcompat.app.AppCompatDelegate
import app.cclauncher.data.Constants
import kotlinx.serialization.Serializable
import app.cclauncher.data.HomeLayout
import io.github.mlmgames.settings.core.annotations.ActionHandler
import io.github.mlmgames.settings.core.annotations.CategoryDefinition
import io.github.mlmgames.settings.core.annotations.Persisted
import io.github.mlmgames.settings.core.annotations.SchemaVersion
import io.github.mlmgames.settings.core.annotations.Serialized
import io.github.mlmgames.settings.core.annotations.Setting
import io.github.mlmgames.settings.core.annotations.SettingAction
import io.github.mlmgames.settings.core.types.Button
import io.github.mlmgames.settings.core.types.Dropdown
import io.github.mlmgames.settings.core.types.SettingTypeMarker
import io.github.mlmgames.settings.core.types.Slider
import io.github.mlmgames.settings.core.types.Toggle

@SchemaVersion(1)
data class AppSettings(

    @Setting(
        title = "Show App Names",
        titleKey = CClauncherSettingsKeys.SETTING_SHOW_APP_NAMES,
        category = General::class,
        type = Toggle::class,
        key = "SHOW_APP_NAMES",
    )
    val showAppNames: Boolean = false,

    @Setting(
        title = "Show Names in Search After",
        titleKey = CClauncherSettingsKeys.SETTING_SHOW_NAMES_IN_SEARCH_AFTER,
        description = "Show app names in search results after typing this many characters. Set to 0 to use the 'Show App Names' setting instead.",
        descriptionKey = CClauncherSettingsKeys.SETTING_SHOW_APP_NAMES_IN_SEARCH_RESULTS_AFTER_TYPING_THIS_MANY_CHARACTERS_SET_TO_0_TO_USE_THE_SHOW_APP_NAMES_SETTING_INSTEAD_DESC,
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
        titleKey = CClauncherSettingsKeys.SETTING_SHOW_PINNED_SHORTCUTS,
        description = "Display pinned app shortcuts in the app drawer.",
        descriptionKey = CClauncherSettingsKeys.SETTING_DISPLAY_PINNED_APP_SHORTCUTS_IN_THE_APP_DRAWER_DESC,
        category = General::class,
        type = Toggle::class,
        key = "SHOW_PINNED_SHORTCUTS",
    )
    val showPinnedShortcuts: Boolean = true,

    @Setting(
        title = "Show App Drawer Icons",
        titleKey = CClauncherSettingsKeys.SETTING_SHOW_APP_DRAWER_ICONS,
        category = General::class,
        type = Toggle::class,
        key = "SHOW_APP_ICONS")
    val showAppIcons: Boolean = true,

    @Setting(
        title = "Auto Show Keyboard",
        titleKey = CClauncherSettingsKeys.SETTING_AUTO_SHOW_KEYBOARD,
        category = General::class,
        type = Toggle::class,
        key = "AUTO_SHOW_KEYBOARD",
    )
    val autoShowKeyboard: Boolean = true,

    @Setting(
        title = "Show Hidden in Search",
        titleKey = CClauncherSettingsKeys.SETTING_SHOW_HIDDEN_IN_SEARCH,
        category = General::class,
        type = Toggle::class,
        key = "SHOW_HIDDEN_APPS_IN_SEARCH",
    )
    val showHiddenAppsOnSearch: Boolean = false,

    @Setting(
        title = "Auto Open Single Matches",
        titleKey = CClauncherSettingsKeys.SETTING_AUTO_OPEN_SINGLE_MATCHES,
        category = General::class,
        type = Toggle::class,
        key = "AUTO_OPEN_FILTERED_APP",
    )
    val autoOpenFilteredApp: Boolean = true,

    @Setting(
        title = "Search Type",
        titleKey = CClauncherSettingsKeys.SETTING_SEARCH_TYPE,
        category = General::class,
        type = Dropdown::class,
        optionsKey = CClauncherSettingsKeys.SETTING_SEARCHTYPE_OPTIONS,
        key = "SEARCH_TYPE",
    )
    val searchType: Int = Constants.SearchType.CONTAINS,

    @Setting(
        title = "Return to Home After App",
        titleKey = CClauncherSettingsKeys.SETTING_RETURN_TO_HOME_AFTER_APP,
        description = "Return to home screen instead of search after closing an app",
        descriptionKey = CClauncherSettingsKeys.SETTING_RETURN_TO_HOME_SCREEN_INSTEAD_OF_SEARCH_AFTER_CLOSING_AN_APP_DESC,
        category = General::class,
        type = Toggle::class,
        key = "RETURN_TO_HOME_AFTER_APP",
    )
    val returnToHomeAfterApp: Boolean = false,

    @Setting(
        title = "Default Screen",
        titleKey = CClauncherSettingsKeys.SETTING_DEFAULT_SCREEN,
        description = "Choose which screen to show when opening the launcher",
        descriptionKey = CClauncherSettingsKeys.SETTING_CHOOSE_WHICH_SCREEN_TO_SHOW_WHEN_OPENING_THE_LAUNCHER_DESC,
        category = General::class,
        type = Dropdown::class,
        optionsKey = CClauncherSettingsKeys.SETTING_DEFAULTSCREEN_OPTIONS,
        key = "DEFAULT_SCREEN",
    )
    val defaultScreen: Int = 0,

    @Setting(
        title = "Search Sort Order",
        titleKey = CClauncherSettingsKeys.SETTING_SEARCH_SORT_ORDER,
        category = General::class,
        type = Dropdown::class,
        optionsKey = CClauncherSettingsKeys.SETTING_SEARCHSORTORDER_OPTIONS,
        key = "SEARCH_SORT_ORDER",
    )
    val searchSortOrder: Int = Constants.SortOrder.ALPHABETICAL,

    @Setting(
        title = "Search Aliases",
        titleKey = CClauncherSettingsKeys.SETTING_SEARCH_ALIASES,
        description = "Match app names across transliterations and keyboard layouts (e.g. Африка ↔ Afrika). May slightly increase CPU use on older devices.",
        descriptionKey = CClauncherSettingsKeys.SETTING_MATCH_APP_NAMES_ACROSS_TRANSLITERATIONS_AND_KEYBOARD_LAYOUTS_E_G_AFRIKA_MAY_SLIGHTLY_INCREASE_CPU_USE_ON_OLDER_DEVICES_DESC,
        category = General::class,
        type = Dropdown::class,
        optionsKey = CClauncherSettingsKeys.SETTING_SEARCHALIASESMODE_OPTIONS,
        key = "SEARCH_ALIASES_MODE",
    )
    val searchAliasesMode: Int = 0,

    @Persisted(key = "SEARCH_INCLUDE_PACKAGE_NAMES")
    val searchIncludePackageNames: Boolean = false,

    @Setting(
        title = "Theme",
        titleKey = CClauncherSettingsKeys.SETTING_THEME,
        category = Appearance::class,
        type = Dropdown::class,
        optionsKey = CClauncherSettingsKeys.SETTING_APPTHEME_OPTIONS,
        key = "APP_THEME",
    )
    val appTheme: Int = AppCompatDelegate.MODE_NIGHT_YES,

    @Setting(
        title = "Home Text Size",
        titleKey = CClauncherSettingsKeys.SETTING_HOME_TEXT_SIZE,
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
        titleKey = CClauncherSettingsKeys.SETTING_FONT_WEIGHT,
        category = Appearance::class,
        type = Dropdown::class,
        optionsKey = CClauncherSettingsKeys.SETTING_FONTWEIGHT_OPTIONS,
        key = "FONT_WEIGHT",
    )
    val fontWeight: Int = 2,

    @Setting(
        title = "Use System Font",
        titleKey = CClauncherSettingsKeys.SETTING_USE_SYSTEM_FONT,
        category = Appearance::class,
        type = Toggle::class,
        key = "USE_SYSTEM_FONT",
    )
    val useSystemFont: Boolean = true,

    @Setting(
        title = "Custom Font",
        titleKey = CClauncherSettingsKeys.SETTING_CUSTOM_FONT,
        description = "Select a custom font file",
        descriptionKey = CClauncherSettingsKeys.SETTING_SELECT_A_CUSTOM_FONT_FILE_DESC,
        category = Appearance::class,
        type = FontPicker::class,
        key = "CUSTOM_FONT_PATH",
    )
    val customFontPath: String = "",

    @Setting(
        title = "Use Dynamic Theme",
        titleKey = CClauncherSettingsKeys.SETTING_USE_DYNAMIC_THEME,
        category = Appearance::class,
        type = Toggle::class,
        key = "USE_DYNAMIC_THEME",
    )
    val useDynamicTheme: Boolean = false,

    @Setting(
        title = "Screen Orientation",
        titleKey = CClauncherSettingsKeys.SETTING_SCREEN_ORIENTATION,
        type = Dropdown::class,
        optionsKey = CClauncherSettingsKeys.SETTING_SCREENORIENTATION_OPTIONS,
        category = Appearance::class
    )
    val screenOrientation: Int = 0,

    @Setting(
        title = "Item Spacing",
        titleKey = CClauncherSettingsKeys.SETTING_ITEM_SPACING,
        category = Appearance::class,
        type = Dropdown::class,
        optionsKey = CClauncherSettingsKeys.SETTING_ITEMSPACING_OPTIONS,
    )
    val itemSpacing: Int = 1,

    @Setting(
        title = "Search Results Use Home Font Size",
        titleKey = CClauncherSettingsKeys.SETTING_SEARCH_RESULTS_USE_HOME_FONT_SIZE,
        category = Appearance::class,
        type = Toggle::class,
        description = "Use the same font size for search results as home screen",
        descriptionKey = CClauncherSettingsKeys.SETTING_USE_THE_SAME_FONT_SIZE_FOR_SEARCH_RESULTS_AS_HOME_SCREEN_DESC,
    )
    val searchResultsUseHomeFont: Boolean = false,

    @Setting(
        title = "Search Results Font Size",
        titleKey = CClauncherSettingsKeys.SETTING_SEARCH_RESULTS_FONT_SIZE,
        category = Appearance::class,
        type = Slider::class,
        min = 0.5f,
        max = 2.0f,
        step = 0.1f,
    )
    val searchResultsFontSize: Float = 1.0f,

    @Setting(
        title = "Icon Corner Radius",
        titleKey = CClauncherSettingsKeys.SETTING_ICON_CORNER_RADIUS,
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
        titleKey = CClauncherSettingsKeys.SETTING_TEXT_COLOR,
        description = "Customize text color for better visibility",
        descriptionKey = CClauncherSettingsKeys.SETTING_CUSTOMIZE_TEXT_COLOR_FOR_BETTER_VISIBILITY_DESC,
        category = Appearance::class,
        type = ColorPicker::class,
        key = "TEXT_COLOR",
    )
    val textColor: Int = 0,

    @Setting(
        title = "Use Custom Text Color",
        titleKey = CClauncherSettingsKeys.SETTING_USE_CUSTOM_TEXT_COLOR,
        description = "Override theme text color with custom color",
        descriptionKey = CClauncherSettingsKeys.SETTING_OVERRIDE_THEME_TEXT_COLOR_WITH_CUSTOM_COLOR_DESC,
        category = Appearance::class,
        type = Toggle::class,
        key = "USE_CUSTOM_TEXT_COLOR",
    )
    val useCustomTextColor: Boolean = false,

    @Setting(
        title = "Icon Pack",
        titleKey = CClauncherSettingsKeys.SETTING_ICON_PACK,
        category = Appearance::class,
        type = IconPackPicker::class,
        description = "Choose custom icon pack for apps",
        descriptionKey = CClauncherSettingsKeys.SETTING_CHOOSE_CUSTOM_ICON_PACK_FOR_APPS_DESC,
        key = "SELECTED_ICON_PACK",
    )
    val selectedIconPack: String = "default",

    // TODO: This is an action
    @Setting(
        title = "Set Plain Wallpaper",
        titleKey = CClauncherSettingsKeys.SETTING_SET_PLAIN_WALLPAPER,
        description = "Set a plain black/white wallpaper based on theme",
        descriptionKey = CClauncherSettingsKeys.SETTING_SET_A_PLAIN_BLACK_WHITE_WALLPAPER_BASED_ON_THEME_DESC,
        category = Appearance::class,
        type = Button::class,
    )
    @ActionHandler(SetPlainWallpaperAction::class)
    val plainWallpaper: Unit = Unit,

    @Setting(
        title = "Long Press in App Drawer",
        titleKey = CClauncherSettingsKeys.SETTING_LONG_PRESS_IN_APP_DRAWER,
        description = "Long press on apps shows options menu",
        descriptionKey = CClauncherSettingsKeys.SETTING_LONG_PRESS_ON_APPS_SHOWS_OPTIONS_MENU_DESC,
        category = Gestures::class,
        type = Toggle::class,
        key = "APP_DRAWER_LONG_PRESS_ENABLED",
    )
    val appDrawerLongPressEnabled: Boolean = true,

    @Setting(
        title = "Auto Update Wallpaper",
        titleKey = CClauncherSettingsKeys.SETTING_AUTO_UPDATE_WALLPAPER,
        description = "Automatically update plain wallpaper when system theme changes",
        descriptionKey = CClauncherSettingsKeys.SETTING_AUTOMATICALLY_UPDATE_PLAIN_WALLPAPER_WHEN_SYSTEM_THEME_CHANGES_DESC,
        category = Appearance::class,
        type = Toggle::class,
        key = "AUTO_UPDATE_WALLPAPER",
    )
    val autoUpdateWallpaper: Boolean = false,

    @Setting(
        title = "Show Status Bar",
        titleKey = CClauncherSettingsKeys.SETTING_SHOW_STATUS_BAR,
        category = Layout::class,
        type = Toggle::class,
        key = "STATUS_BAR",
    )
    val statusBar: Boolean = false,

    @Setting(
        title = "Tap to Open in App Drawer",
        titleKey = CClauncherSettingsKeys.SETTING_TAP_TO_OPEN_IN_APP_DRAWER,
        description = "When disabled, tapping an app drawer app does nothing. Long‑press still opens the menu.",
        descriptionKey = CClauncherSettingsKeys.SETTING_WHEN_DISABLED_TAPPING_AN_APP_DRAWER_APP_DOES_NOTHING_LONG_PRESS_STILL_OPENS_THE_MENU_DESC,
        category = General::class,
        type = Toggle::class,
        key = "APP_DRAWER_TAP_TO_OPEN",
    )
    val appDrawerTapToOpen: Boolean = true,

    @Setting(
        title = "Scale Home Apps",
        titleKey = CClauncherSettingsKeys.SETTING_SCALE_HOME_APPS,
        category = Layout::class,
        type = Toggle::class,
        key = "SCALE_HOME_APPS",
    )
    val scaleHomeApps: Boolean = true,

    @Setting(
        title = "Show Web Search Option",
        titleKey = CClauncherSettingsKeys.SETTING_SHOW_WEB_SEARCH_OPTION,
        description = "Show 'Search Web' button when no apps match",
        descriptionKey = CClauncherSettingsKeys.SETTING_SHOW_SEARCH_WEB_BUTTON_WHEN_NO_APPS_MATCH_DESC,
        category = General::class,
        type = Toggle::class,
        key = "SHOW_WEB_SEARCH_OPTION",
    )
    val showWebSearchOption: Boolean = true,

    @Setting(
        title = "Home Screen Rows",
        titleKey = CClauncherSettingsKeys.SETTING_HOME_SCREEN_ROWS,
        description = "Number of rows in the home screen grid",
        descriptionKey = CClauncherSettingsKeys.SETTING_NUMBER_OF_ROWS_IN_THE_HOME_SCREEN_GRID_DESC,
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
        titleKey = CClauncherSettingsKeys.SETTING_HOME_SCREEN_COLUMNS,
        description = "Number of columns in the home screen grid",
        descriptionKey = CClauncherSettingsKeys.SETTING_NUMBER_OF_COLUMNS_IN_THE_HOME_SCREEN_GRID_DESC,
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
        titleKey = CClauncherSettingsKeys.SETTING_HOME_SCREEN_PAGES,
        description = "Number of home screen pages",
        descriptionKey = CClauncherSettingsKeys.SETTING_NUMBER_OF_HOME_SCREEN_PAGES_DESC,
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
        titleKey = CClauncherSettingsKeys.SETTING_SHOW_PAGE_INDICATOR,
        description = "Show page dots at the bottom of the home screen",
        descriptionKey = CClauncherSettingsKeys.SETTING_SHOW_PAGE_DOTS_AT_THE_BOTTOM_OF_THE_HOME_SCREEN_DESC,
        category = Layout::class,
        type = Toggle::class,
        key = "SHOW_PAGE_INDICATOR",
    )
    val showPageIndicator: Boolean = true,

    @Setting(
        title = "Show App Icons on Home Screen",
        titleKey = CClauncherSettingsKeys.SETTING_SHOW_APP_ICONS_ON_HOME_SCREEN,
        description = "Display app icons on the home screen",
        descriptionKey = CClauncherSettingsKeys.SETTING_DISPLAY_APP_ICONS_ON_THE_HOME_SCREEN_DESC,
        category = Appearance::class,
        type = Toggle::class,
        key = "SHOW_HOME_SCREEN_ICONS",
    )
    val showHomeScreenIcons: Boolean = false,

    @Setting(
        title = "Show App Icons in Landscape",
        titleKey = CClauncherSettingsKeys.SETTING_SHOW_APP_ICONS_IN_LANDSCAPE,
        category = Layout::class,
        type = Toggle::class,
        key = "SHOW_ICONS_IN_LANDSCAPE",
//        dependsOn = "showAppIcons"
    )
    val showIconsInLandscape: Boolean = false,

    @Setting(
        title = "Show App Icons in Portrait",
        titleKey = CClauncherSettingsKeys.SETTING_SHOW_APP_ICONS_IN_PORTRAIT,
        category = Layout::class,
        type = Toggle::class,
        key = "SHOW_ICONS_IN_PORTRAIT",
//        dependsOn = "showAppIcons"
    )
    val showIconsInPortrait: Boolean = false,

    @Setting(
        title = "Home App Label Alignment",
        titleKey = CClauncherSettingsKeys.SETTING_HOME_APP_LABEL_ALIGNMENT,
        description = "Align app names on the home screen",
        descriptionKey = CClauncherSettingsKeys.SETTING_ALIGN_APP_NAMES_ON_THE_HOME_SCREEN_DESC,
        category = Appearance::class,
        type = Dropdown::class,
        optionsKey = CClauncherSettingsKeys.SETTING_APPLABELALIGNMENT_OPTIONS,
        key = "APP_LABEL_ALIGNMENT",
    )
    val appLabelAlignment: Int = 0,

    @Setting(
        title = "Search Bar Position",
        titleKey = CClauncherSettingsKeys.SETTING_SEARCH_BAR_POSITION,
        description = "Position of the search bar in the app drawer",
        descriptionKey = CClauncherSettingsKeys.SETTING_POSITION_OF_THE_SEARCH_BAR_IN_THE_APP_DRAWER_DESC,
        category = Layout::class,
        type = Dropdown::class,
        optionsKey = CClauncherSettingsKeys.SETTING_SEARCHBARPOSITION_OPTIONS,
        key = "SEARCH_BAR_POSITION",
    )
    val searchBarPosition: Int = 0,

    @Setting(
        title = "App Drawer Results Alignment",
        titleKey = CClauncherSettingsKeys.SETTING_APP_DRAWER_RESULTS_ALIGNMENT,
        description = "Horizontal alignment of app names in app drawer results",
        descriptionKey = CClauncherSettingsKeys.SETTING_HORIZONTAL_ALIGNMENT_OF_APP_NAMES_IN_APP_DRAWER_RESULTS_DESC,
        category = Appearance::class,
        type = Dropdown::class,
        optionsKey = CClauncherSettingsKeys.SETTING_SEARCHRESULTSALIGNMENT_OPTIONS,
        key = "SEARCH_RESULTS_ALIGNMENT",
    )
    val searchResultsAlignment: Int = 0,

    @Setting(
        title = "Reverse Search Results",
        titleKey = CClauncherSettingsKeys.SETTING_REVERSE_SEARCH_RESULTS,
        description = "Display results bottom-to-top",
        descriptionKey = CClauncherSettingsKeys.SETTING_DISPLAY_RESULTS_BOTTOM_TO_TOP_DESC,
        category = Layout::class,
        type = Toggle::class,
        key = "REVERSE_SEARCH_RESULTS",
    )
    val reverseSearchResults: Boolean = false,

    @Setting(
        title = "Gesture Sensitivity",
        titleKey = CClauncherSettingsKeys.SETTING_GESTURE_SENSITIVITY,
        description = "Adjust how easily swipe gestures are triggered",
        descriptionKey = CClauncherSettingsKeys.SETTING_ADJUST_HOW_EASILY_SWIPE_GESTURES_ARE_TRIGGERED_DESC,
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
        titleKey = CClauncherSettingsKeys.SETTING_DOUBLE_TAP_TO_LOCK_SCREEN,
        category = Gestures::class,
        type = Toggle::class,
        key = "DOUBLE_TAP_TO_LOCK",
    )
    val doubleTapToLock: Boolean = false,

    @Setting(
        title = "Swipe Down Action",
        titleKey = CClauncherSettingsKeys.SETTING_SWIPE_DOWN_ACTION,
        category = Gestures::class,
        type = Dropdown::class,
        optionsKey = CClauncherSettingsKeys.SETTING_SWIPEDOWNACTION_OPTIONS,
        key = "SWIPE_DOWN_ACTION",
    )
    val swipeDownAction: Int = Constants.SwipeAction.NOTIFICATIONS,

    @Setting(
        title = "Swipe Down App",
        titleKey = CClauncherSettingsKeys.SETTING_SWIPE_DOWN_APP,
        category = Gestures::class,
        type = AppPicker::class,
        key = "SWIPE_DOWN_APP_JSON",
    )
    @Serialized
    val swipeDownApp: AppPreference = AppPreference(),

    @Setting(
        title = "Swipe Up Action",
        titleKey = CClauncherSettingsKeys.SETTING_SWIPE_UP_ACTION,
        category = Gestures::class,
        type = Dropdown::class,
        optionsKey = CClauncherSettingsKeys.SETTING_SWIPEUPACTION_OPTIONS,
        key = "SWIPE_UP_ACTION",
    )
    val swipeUpAction: Int = Constants.SwipeAction.SEARCH,

    @Setting(
        title = "Swipe Up App",
        titleKey = CClauncherSettingsKeys.SETTING_SWIPE_UP_APP,
        category = Gestures::class,
        type = AppPicker::class,
        key = "SWIPE_UP_APP_JSON",
    )
    @Serialized
    val swipeUpApp: AppPreference = AppPreference(),

    @Setting(
        title = "Swipe Left Action",
        titleKey = CClauncherSettingsKeys.SETTING_SWIPE_LEFT_ACTION,
        category = Gestures::class,
        type = Dropdown::class,
        optionsKey = CClauncherSettingsKeys.SETTING_SWIPELEFTACTION_OPTIONS,
        key = "SWIPE_LEFT_ACTION",
    )
    val swipeLeftAction: Int = Constants.SwipeAction.NULL, // Remain until needed?

    @Setting(
        title = "Left Swipe App",
        titleKey = CClauncherSettingsKeys.SETTING_LEFT_SWIPE_APP,
        category = Gestures::class,
        type = AppPicker::class,
        key = "SWIPE_LEFT_APP_JSON",
    )
    @Serialized
    val swipeLeftApp: AppPreference = AppPreference(label = "Not set"),

    @Setting(
        title = "Swipe Right Action",
        titleKey = CClauncherSettingsKeys.SETTING_SWIPE_RIGHT_ACTION,
        category = Gestures::class,
        type = Dropdown::class,
        optionsKey = CClauncherSettingsKeys.SETTING_SWIPERIGHTACTION_OPTIONS,
        key = "SWIPE_RIGHT_ACTION",
    )
    val swipeRightAction: Int = Constants.SwipeAction.NULL,

    @Setting(
        title = "Right Swipe App",
        titleKey = CClauncherSettingsKeys.SETTING_RIGHT_SWIPE_APP,
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
@CategoryDefinition(order = 0, titleKey = CClauncherSettingsKeys.CATEGORY_GENERAL) object General
@CategoryDefinition(order = 1, titleKey = CClauncherSettingsKeys.CATEGORY_APPEARANCE) object Appearance
@CategoryDefinition(order = 2, titleKey = CClauncherSettingsKeys.CATEGORY_LAYOUT) object Layout
@CategoryDefinition(order = 3, titleKey = CClauncherSettingsKeys.CATEGORY_GESTURES) object Gestures
@CategoryDefinition(order = 4, titleKey = CClauncherSettingsKeys.CATEGORY_SYSTEM) object System

object FontPicker : SettingTypeMarker
object AppPicker : SettingTypeMarker
object IconPackPicker : SettingTypeMarker
object ColorPicker : SettingTypeMarker

object SetPlainWallpaperAction : SettingAction
