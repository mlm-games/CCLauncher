package app.cclauncher.ui.viewmodels

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.cclauncher.data.repository.SettingsRepository
import app.cclauncher.data.settings.AppSettings
import app.cclauncher.ui.UiEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.reflect.full.memberProperties

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    internal val settingsRepository = SettingsRepository(application.applicationContext)

    // UI state for settings
    private val _settingsState = MutableStateFlow(AppSettings())
    val settingsState: StateFlow<AppSettings> = _settingsState.asStateFlow()

    // Loading state
    val isLoading = mutableStateOf(true)

    // Events manager for UI events
    private val _eventsFlow = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _eventsFlow.asSharedFlow()

    init {
        // Load settings from repository
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _settingsState.value = settings
                isLoading.value = false
            }
        }
    }

    /**
     * Update a setting by property name
     */
    suspend fun updateSetting(propertyName: String, value: Any) {
        settingsRepository.updateSetting { currentSettings ->
            // Create a mutable map of all current property values
            val propertyMap = mutableMapOf<String, Any?>()

            // Fill the map with all current property values
            AppSettings::class.memberProperties.forEach { prop ->
                propertyMap[prop.name] = prop.get(currentSettings)
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
            constructor.callBy(parameterValues)
        }
    }

    /**
     * Emit UI event
     */
    fun emitEvent(event: UiEvent) {
        viewModelScope.launch {
            _eventsFlow.emit(event)
        }
    }

    /**
     * Convenience methods for common settings updates
     */

    fun setHomeAppsNum(value: Int) {
        viewModelScope.launch {
            settingsRepository.setHomeAppsNum(value)
        }
    }

    fun setShowAppNames(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowAppNames(value)
        }
    }

    fun setShowAppIcons(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowAppIcons(value)
        }
    }

    fun setTextSizeScale(value: Float) {
        viewModelScope.launch {
            settingsRepository.setTextSizeScale(value)
        }
    }

    fun setFontWeight(value: Int) {
        viewModelScope.launch {
            settingsRepository.setFontWeight(value)
        }
    }

    fun setForceLandscapeMode(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setForceLandscapeMode(value)
        }
    }

    fun setShowIconsInLandscape(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowIconsInLandscape(value)
        }
    }

    fun setShowIconsInPortrait(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowIconsInPortrait(value)
        }
    }

    fun setIconCornerRadius(value: Int) {
        viewModelScope.launch {
            settingsRepository.setIconCornerRadius(value)
        }
    }

    fun setItemSpacing(value: Int) {
        viewModelScope.launch {
            settingsRepository.setItemSpacing(value)
        }
    }
}