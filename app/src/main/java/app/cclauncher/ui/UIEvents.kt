package app.cclauncher.ui

import app.cclauncher.data.AppModel
import app.cclauncher.data.ExternalWidgetModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

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

    // App events
    data class LaunchApp(val app: AppModel) : UiEvent()
    data class SetHomeApp(val app: AppModel, val position: Int) : UiEvent()
    data class SetSwipeApp(val app: AppModel, val isLeft: Boolean) : UiEvent()
    data class ToggleAppHidden(val app: AppModel) : UiEvent()

    // System events
    object ResetLauncher : UiEvent()
    data class ShowToast(val message: String) : UiEvent()
    data class ShowError(val message: String) : UiEvent()
    data class NavigateToAppSelection(val selectionType: AppSelectionType) : UiEvent()
    data class ShowAppSelectionDialog(val selectionType: AppSelectionType) : UiEvent()

    data object NavigateToWidgetPicker : UiEvent()
    data class NavigateToWidgetSizeConfig(val appWidgetId: Int) : UiEvent()
    data class NavigateToWidgetConfig(val widget: ExternalWidgetModel) : UiEvent()
    data object NavigateToWidgetManager : UiEvent()




}


/**
 * Class to manage events
 */
class EventsManager {
    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events

    suspend fun emitEvent(event: UiEvent) {
        _events.emit(event)
    }
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
    CLOCK_APP,
    CALENDAR_APP,
}
