package app.cclauncher.ui

import android.appwidget.AppWidgetProviderInfo
import android.content.Intent

/**
 * UI Events for navigation and actions
 */
sealed class UiEvent {
    // Navigation events
    object NavigateToAppDrawer : UiEvent()
    object NavigateToSettings : UiEvent()
    object NavigateToHiddenApps : UiEvent()
    object NavigateBack : UiEvent()

    // Dialog events
    data class ShowDialog(val dialogType: String) : UiEvent()

    data class LaunchWidgetBindIntent(val intent: Intent) : UiEvent()

    // System events
    data class ShowToast(val message: String) : UiEvent()
    data class NavigateToAppSelection(val selectionType: AppSelectionType) : UiEvent()


    data object NavigateToWidgetPicker : UiEvent()
    data class StartActivityForResult(val intent: Intent, val requestCode: Int) : UiEvent()

    data class ConfigureWidget(val widgetId: Int, val providerInfo: AppWidgetProviderInfo) : UiEvent()



}

enum class AppSelectionType {
    HOME_APP_1,
    HOME_APP_2,
    HOME_APP_3,
    HOME_APP_4,
    HOME_APP_5,
    HOME_APP_6,
    HOME_APP_7,
    HOME_APP_8,
    HOME_APP_9,
    HOME_APP_10,
    HOME_APP_11,
    HOME_APP_12,
    HOME_APP_13,
    HOME_APP_14,
    HOME_APP_15,
    HOME_APP_16,
    SWIPE_LEFT_APP,
    SWIPE_RIGHT_APP,
    SWIPE_UP_APP,
    SWIPE_DOWN_APP,
}
