package app.cclauncher.data.settings

import kotlin.annotation.AnnotationRetention
import kotlin.annotation.AnnotationTarget

import android.view.Gravity
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.Preferences
import app.cclauncher.data.Constants
import kotlinx.serialization.Serializable
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

/**
 * Annotation for app settings
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
 * Definition for a setting that maps between AppSettings property and DataStore key
 */
sealed class SettingDefinition<T> {
    abstract val key: Preferences.Key<T>
    abstract val getValue: (AppSettings) -> T
    abstract val propertyName: String

    data class BooleanSetting(
        override val propertyName: String,
        override val key: Preferences.Key<Boolean>,
        override val getValue: (AppSettings) -> Boolean
    ) : SettingDefinition<Boolean>()

    data class IntSetting(
        override val propertyName: String,
        override val key: Preferences.Key<Int>,
        override val getValue: (AppSettings) -> Int
    ) : SettingDefinition<Int>()

    data class FloatSetting(
        override val propertyName: String,
        override val key: Preferences.Key<Float>,
        override val getValue: (AppSettings) -> Float
    ) : SettingDefinition<Float>()

    data class StringSetting(
        override val propertyName: String,
        override val key: Preferences.Key<String>,
        override val getValue: (AppSettings) -> String
    ) : SettingDefinition<String>()

    data class LongSetting(
        override val propertyName: String,
        override val key: Preferences.Key<Long>,
        override val getValue: (AppSettings) -> Long
    ) : SettingDefinition<Long>()

    data class StringSetSetting(
        override val propertyName: String,
        override val key: Preferences.Key<Set<String>>,
        override val getValue: (AppSettings) -> Set<String>
    ) : SettingDefinition<Set<String>>()
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
 * Types of settings
 */
enum class SettingType {
    TOGGLE,
    SLIDER,
    DROPDOWN,
    BUTTON,
//    COLOR_PICKER,
    FONT_PICKER,
    APP_PICKER,
    ICON_PACK_PICKER
}

/**
 * Central data class for all application settings
 */
data class AppSettings(

    @Setting(
        title = "Show App Names",
        category = SettingCategory.GENERAL,
        type = SettingType.TOGGLE
    )
    val showAppNames: Boolean = false,

//    @Setting(
//        title = "Show App Icons",
//        category = SettingCategory.GENERAL,
//        type = SettingType.TOGGLE
//    )
    val showAppIcons: Boolean = true,

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
        options = ["Thin", "Light", "Normal", "Medium", "Bold", "Black"],
        dependsOn = "isSystemFont"
    )
    val fontWeight: Int = 2,

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
        max = 50f,
        step = 1f
    )
    val iconCornerRadius: Int = 0,

    @Setting(
        title = "Item Spacing",
        category = SettingCategory.APPEARANCE,
        type = SettingType.DROPDOWN,
        options = ["None", "Small", "Medium", "Large"]
    )
    val itemSpacing: Int = 1,

    @Setting(
        title = "Show Status Bar",
        category = SettingCategory.LAYOUT,
        type = SettingType.TOGGLE
    )
    val statusBar: Boolean = false,

    @Setting(
        title = "Scale Home Apps",
        category = SettingCategory.LAYOUT,
        type = SettingType.TOGGLE
    )
    val scaleHomeApps: Boolean = true,

    @Setting(
        title = "Home Screen Rows",
        category = SettingCategory.LAYOUT,
        type = SettingType.SLIDER,
        min = 4f,
        max = 12f,
        step = 1f,
        description = "Number of rows in the home screen grid"
    )
    val homeScreenRows: Int = 8,

    @Setting(
        title = "Home Screen Columns",
        category = SettingCategory.LAYOUT,
        type = SettingType.SLIDER,
        min = 2f,
        max = 8f,
        step = 1f,
        description = "Number of columns in the home screen grid"
    )
    val homeScreenColumns: Int = 4,

    @Setting(
        title = "Screen Orientation",
        type = SettingType.DROPDOWN,
        options = ["System Default", "Force Portrait", "Force Landscape"],
        category = SettingCategory.APPEARANCE
    )
    var screenOrientation: Int = 0,

    @Setting(
        title = "Show App Icons on Home Screen",
        category = SettingCategory.APPEARANCE,
        type = SettingType.TOGGLE,
        description = "Display app icons on the home screen"
    )
    val showHomeScreenIcons: Boolean = false,

    @Setting(
        title = "Icon Pack",
        category = SettingCategory.APPEARANCE,
        type = SettingType.ICON_PACK_PICKER,
        description = "Choose custom icon pack for apps"
    )
    val selectedIconPack: String = "default",

//    @Setting( // Already present in SettingsScreen
//        title = "Lock Settings",
//        category = SettingCategory.SYSTEM,
//        type = SettingType.TOGGLE,
//        description = "Prevent changes to settings without unlocking"
//    )
    val lockSettings: Boolean = false,

    val settingsLockPin: String = "",

    @Setting(
        title = "Show App Icons in Landscape",
        category = SettingCategory.LAYOUT,
        type = SettingType.TOGGLE,
//        dependsOn = "showHomeScreenIcons"
    )
    val showIconsInLandscape: Boolean = false,

    @Setting(
        title = "Show App Icons in Portrait",
        category = SettingCategory.LAYOUT,
        type = SettingType.TOGGLE,
//        dependsOn = "showHomeScreenIcons"
    )
    val showIconsInPortrait: Boolean = false,

    @Setting(
        title = "Swipe Down Action",
        category = SettingCategory.GESTURES,
        type = SettingType.DROPDOWN,
        options = ["None", "Search", "Notifications", "App"]
    )
    val swipeDownAction: Int = Constants.SwipeAction.NOTIFICATIONS,

    @Setting(
        title = "Swipe Down App",
        category = SettingCategory.GESTURES,
        type = SettingType.APP_PICKER,
    )
    val swipeDownApp: AppPreference = AppPreference(),

    @Setting(
        title = "Swipe Up Action",
        category = SettingCategory.GESTURES,
        type = SettingType.DROPDOWN,
        options = ["None", "Search", "Notifications", "App"]
    )
    val swipeUpAction: Int = Constants.SwipeAction.SEARCH,


    @Setting(
        title = "Swipe Up App",
        category = SettingCategory.GESTURES,
        type = SettingType.APP_PICKER
    )
    val swipeUpApp: AppPreference = AppPreference(),


    @Setting(
        title = "Double Tap to Lock Screen",
        category = SettingCategory.GESTURES,
        type = SettingType.TOGGLE
    )
    val doubleTapToLock: Boolean = false,

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
        step = 0.1f
    )
    val searchResultsFontSize: Float = 1.0f,

    @Setting(
        title = "Swipe Left Action",
        category = SettingCategory.GESTURES,
        type = SettingType.DROPDOWN,
        options = ["None", "Search", "Notifications", "App"]
    )
    val swipeLeftAction: Int = Constants.SwipeAction.NULL,

    @Setting(
        title = "Left Swipe App",
        category = SettingCategory.GESTURES,
        type = SettingType.APP_PICKER,
        dependsOn = "swipeLeftEnabled"
    )
    val swipeLeftApp: AppPreference = AppPreference(label = "Not set"),


    @Setting(
        title = "Swipe Right Action",
        category = SettingCategory.GESTURES,
        type = SettingType.DROPDOWN,
        options = ["None", "Search", "Notifications", "App"]
    )
    val swipeRightAction: Int = Constants.SwipeAction.NULL,


    @Setting(
        title = "Right Swipe App",
        category = SettingCategory.GESTURES,
        type = SettingType.APP_PICKER,
        dependsOn = "swipeRightEnabled"
    )
    val swipeRightApp: AppPreference = AppPreference(label = "Not set"),


    @Setting(
        title = "Set Plain Wallpaper",
        category = SettingCategory.APPEARANCE,
        type = SettingType.BUTTON,
        description = "Set a plain black/white wallpaper based on theme"
    )
    val plainWallpaper: Boolean = false,

    @Setting(
        title = "Search Sort Order",
        category = SettingCategory.GENERAL,
        type = SettingType.DROPDOWN,
        options = ["Alphabetical", "Reverse Alpha", "Last Launch"]
    )
    val searchSortOrder: Int = Constants.SortOrder.ALPHABETICAL,

    @Setting(
        title = "Custom Font",
        description = "Select a custom font file",
        type = SettingType.FONT_PICKER,
        category = SettingCategory.APPEARANCE
    )
    val customFontPath: String = "",



    val homeApps: List<HomeAppPreference> = List(Constants.HomeAppCount.NUM) { HomeAppPreference() }, // Changed from NUM to actual count needed, ensure constant is correct


    // Non-UI settings (not annotated)
    val firstOpen: Boolean = true,
    val firstOpenTime: Long = 0L,
    val firstSettingsOpen: Boolean = true,
    val firstHide: Boolean = true,
    val userState: String = Constants.UserState.START,
    val lockMode: Boolean = false,
    val keyboardMessage: Boolean = false,
    val renamedApps: Map<String, String> = mapOf(),
    val recentAppHistory: Map<String, Long> = emptyMap(),
    val appLabelAlignment: Int = Gravity.START,
    val hiddenApps: Set<String> = emptySet(),
    val hiddenAppsUpdated: Boolean = false,
    val showHintCounter: Int = 1,
    val aboutClicked: Boolean = false,
    val rateClicked: Boolean = false,
    val shareShownTime: Long = 0L,

) {
    @Suppress("unused") // Used for disabling font weight
    val isSystemFont: Boolean
        get() = customFontPath.isEmpty()
    companion object {
        // Helper method to get default settings
        fun getDefault(): AppSettings = AppSettings()
    }
}

/**
 * Manager class that handles settings reflection and organization
 */
class SettingsManager {
    /**
     * Get all settings properties with their annotations
     */
    fun getAllSettings(): List<Pair<KProperty1<AppSettings, *>, Setting>> {
        return AppSettings::class.memberProperties
            .mapNotNull { property ->
                val annotation = property.findAnnotation<Setting>()
                if (annotation != null) {
                    property to annotation
                } else {
                    null
                }
            }
    }

    /**
     * Get settings grouped by category
     */
    fun getSettingsByCategory(): Map<SettingCategory, List<Pair<KProperty1<AppSettings, *>, Setting>>> {
        return getAllSettings().groupBy { it.second.category }
    }

    /**
     * Get a setting value from an AppSettings instance
     */
    @Suppress("unused")
    fun getSettingValue(settings: AppSettings, property: KProperty1<AppSettings, *>): Any? {
        return property.get(settings)
    }

    /**
     * Create a new AppSettings instance with an updated value for a property
     */
    fun updateSetting(settings: AppSettings, propertyName: String, value: Any): AppSettings {
        // Create a mutable map of all current property values
        val propertyMap = mutableMapOf<String, Any?>()

        // Fill the map with all current property values
        AppSettings::class.memberProperties.forEach { prop ->
            propertyMap[prop.name] = prop.get(settings)
        }

        // Update the specific property
        propertyMap[propertyName] = value

        // Create a new instance with the updated property
        val constructor = AppSettings::class.constructors.first()
        val parameters = constructor.parameters

        // Map parameter names to values, using the updated property value where applicable
        val parameterValues = parameters.associateWith { param ->
            propertyMap[param.name]
        }

        // Create a new instance with the updated values
        return constructor.callBy(parameterValues)
    }

    /**
     * Check if a setting is enabled based on its dependencies
     */
    fun isSettingEnabled(
        settings: AppSettings,
        property: KProperty1<AppSettings, *>,
        annotation: Setting
    ): Boolean {
        val dependsOn = annotation.dependsOn

        if (dependsOn.isEmpty()) {
            return true
        }

        val dependencyProperty = AppSettings::class.memberProperties.find { it.name == dependsOn }
        return if (dependencyProperty != null) {
            val dependencyValue = dependencyProperty.get(settings)
            when (dependencyValue) {
                is Boolean -> dependencyValue
                else -> true
            }
        } else {
            true
        }
    }
}

@Serializable
data class HomeAppPreference(
    val label: String = "",
    val packageName: String = "",
    val activityClassName: String? = null,
    val userString: String = "",
)

@Serializable
data class AppPreference(
    val label: String = "",
    val packageName: String = "",
    val activityClassName: String? = null,
    val userString: String = ""
)