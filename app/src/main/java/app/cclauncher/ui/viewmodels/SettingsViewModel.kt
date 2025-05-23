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


    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked

    private val _showLockDialog = MutableStateFlow(false)
    val showLockDialog: StateFlow<Boolean> = _showLockDialog

    private val _isSettingPin = MutableStateFlow(false)
    val isSettingPin: StateFlow<Boolean> = _isSettingPin

    private val _isTemporarilyUnlocked = MutableStateFlow(false)
    val isTemporarilyUnlocked: StateFlow<Boolean> = _isTemporarilyUnlocked

    val effectiveLockState: StateFlow<Boolean> = combine(_isLocked, _isTemporarilyUnlocked) { locked, tempUnlocked ->
        locked && !tempUnlocked
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)



    init {
        // Load settings from repository
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _settingsState.value = settings
                isLoading.value = false
                _isLocked.value = settings.lockSettings
            }
        }
    }

    /**
     * Update a setting by property name
     */
    suspend fun updateSetting(propertyName: String, value: Any) {
        settingsRepository.updateSetting(propertyName, value)
    }

    /**
     * Emit UI event
     */
    fun emitEvent(event: UiEvent) {
        viewModelScope.launch {
            _eventsFlow.emit(event)
        }
    }

    fun setShowLockDialog(show: Boolean, isSettingPin: Boolean = false) {
        _showLockDialog.value = show
        _isSettingPin.value = isSettingPin
    }

    fun validatePin(pin: String): Boolean {
        var isValid = false
        viewModelScope.launch {
            isValid = settingsRepository.validateSettingsPin(pin)
            if (isValid) {
//                _isLocked.value = false
                _isTemporarilyUnlocked.value = true
                _showLockDialog.value = false
            }
        }
        return isValid
    }

    fun setPin(pin: String) {
        viewModelScope.launch {
            settingsRepository.setSettingsLockPin(pin)
        }
    }

    fun toggleLockSettings(locked: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSettingsLock(locked)
            if (!locked) {
                // When unlocking, reset the PIN to empty
                settingsRepository.setSettingsLockPin("")
            }
        }
    }

    fun resetUnlockState() {
        _isTemporarilyUnlocked.value = false
    }
}