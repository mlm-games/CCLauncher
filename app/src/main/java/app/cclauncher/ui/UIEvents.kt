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

    data class ConfigureWidget(val widgetId: Int) : UiEvent()



}

enum class AppSelectionType {
    SWIPE_LEFT_APP,
    SWIPE_RIGHT_APP,
    SWIPE_UP_APP,
    SWIPE_DOWN_APP,
}
