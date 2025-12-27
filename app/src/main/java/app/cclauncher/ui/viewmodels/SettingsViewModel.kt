package app.cclauncher.ui.viewmodels

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.cclauncher.settings.AppSettingsRepository
import app.cclauncher.settings.AppSettings
import app.cclauncher.ui.UiEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class SettingsViewModel(application: Application) : AndroidViewModel(application), KoinComponent {
    internal val settingsRepository: AppSettingsRepository by inject()
    // UI state for settings
    private val _settingsState = MutableStateFlow(AppSettings())
    val settingsState: StateFlow<AppSettings> = _settingsState.asStateFlow()

    // Loading state
    val isLoading = mutableStateOf(true)

    // Events manager for UI events
    private val _eventsFlow = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _eventsFlow.asSharedFlow()

    private val _isLocked = MutableStateFlow(false)

    private val _showLockDialog = MutableStateFlow(false)
    val showLockDialog: StateFlow<Boolean> = _showLockDialog

    private val _isSettingPin = MutableStateFlow(false)
    val isSettingPin: StateFlow<Boolean> = _isSettingPin

    private val _isTemporarilyUnlocked = MutableStateFlow(false)

    val effectiveLockState: StateFlow<Boolean> = combine(_isLocked, _isTemporarilyUnlocked) { locked, tempUnlocked ->
        locked && !tempUnlocked
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val customFontInfo: StateFlow<Pair<String, Long>?> = settingsRepository.settings
        .map { settings ->
            val path = settings.customFontPath
            if (path.isNotEmpty()) {
                try {
                    val file = File(path)
                    if (file.exists()) {
                        Pair(file.name, file.length())
                    } else {
                        null // File path is saved but file doesn't exist
                    }
                } catch (_: Exception) {
                    null // Error accessing file
                }
            } else {
                null // No custom font path
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )



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

    fun setCustomFont(uri: Uri) {
        viewModelScope.launch {
            settingsRepository.setCustomFont(uri)
        }
    }

    fun clearCustomFont() {
        viewModelScope.launch {
            settingsRepository.clearCustomFont()
        }
    }

    /**
     * Update a setting by property name
     */
    suspend fun updateSetting(propertyName: String, value: Any) {
        settingsRepository.updateSetting(propertyName, value)
    }

    suspend fun updateGridSize(propertyName: String, newValue: Int) {
        updateSetting(propertyName, newValue)
    }

    /**
     * Check if grid size change will affect existing items
     */
    suspend fun willGridChangeAffectItems(propertyName: String, newValue: Int): Boolean {
        val currentSettings = settingsState.value
        val currentLayout = settingsRepository.getHomeLayout().first()

        val newRows = if (propertyName == "homeScreenRows") newValue else currentSettings.homeScreenRows
        val newColumns = if (propertyName == "homeScreenColumns") newValue else currentSettings.homeScreenColumns

        // Check if any items would be out of bounds
        return currentLayout.items.any { item ->
            item.row + item.rowSpan > newRows || item.column + item.columnSpan > newColumns
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

    fun setShowLockDialog(show: Boolean, isSettingPin: Boolean = false) {
        _showLockDialog.value = show
        _isSettingPin.value = isSettingPin
    }

    suspend fun validatePin(pin: String): Boolean {
        val ok = settingsRepository.validateSettingsPin(pin)
        if (ok) {
            _isTemporarilyUnlocked.value = true
            _showLockDialog.value = false
        }
        return ok
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