package app.cclauncher.ui.viewmodels

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.cclauncher.settings.AppSettingsRepository
import app.cclauncher.settings.AppSettings
import app.cclauncher.ui.UiEvent
import io.github.mlmgames.settings.core.backup.ImportResult
import io.github.mlmgames.settings.core.backup.ValidationResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class SettingsViewModel(application: Application) : AndroidViewModel(application), KoinComponent {
    internal val settingsRepository: AppSettingsRepository by inject()

    private val _settingsState = MutableStateFlow(AppSettings())
    val settingsState: StateFlow<AppSettings> = _settingsState.asStateFlow()

    val isLoading = mutableStateOf(true)

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

    // Import/Export states
    private val _importExportState = MutableStateFlow<ImportExportState>(ImportExportState.Idle)
    val importExportState: StateFlow<ImportExportState> = _importExportState.asStateFlow()

    val customFontInfo: StateFlow<Pair<String, Long>?> = settingsRepository.settings
        .map { settings ->
            val path = settings.customFontPath
            if (path.isNotEmpty()) {
                try {
                    val file = File(path)
                    if (file.exists()) {
                        Pair(file.name, file.length())
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    init {
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

    suspend fun updateSetting(propertyName: String, value: Any) {
        settingsRepository.updateSetting(propertyName, value)
    }

    suspend fun updateGridSize(propertyName: String, newValue: Int) {
        updateSetting(propertyName, newValue)
    }

    suspend fun willGridChangeAffectItems(propertyName: String, newValue: Int): Boolean {
        val currentSettings = settingsState.value
        val currentLayout = settingsRepository.getHomeLayout().first()

        val newRows = if (propertyName == "homeScreenRows") newValue else currentSettings.homeScreenRows
        val newColumns = if (propertyName == "homeScreenColumns") newValue else currentSettings.homeScreenColumns

        return currentLayout.items.any { item ->
            item.row + item.rowSpan > newRows || item.column + item.columnSpan > newColumns
        }
    }

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
                settingsRepository.setSettingsLockPin("")
            }
        }
    }

    fun resetUnlockState() {
        _isTemporarilyUnlocked.value = false
    }

    fun exportSettings(uri: Uri) {
        viewModelScope.launch {
            _importExportState.value = ImportExportState.Loading

            val result = settingsRepository.exportSettingsToUri(uri)

            _importExportState.value = result.fold(
                onSuccess = { ImportExportState.ExportSuccess },
                onFailure = { ImportExportState.Error(it.message ?: "Failed to export settings") }
            )
        }
    }

    fun importSettings(uri: Uri) {
        viewModelScope.launch {
            _importExportState.value = ImportExportState.Loading

            when (val result = settingsRepository.importSettingsFromUri(uri)) {
                is ImportResult.Success -> {
                    _importExportState.value = ImportExportState.ImportSuccess(
                        appliedCount = result.appliedCount,
                        skippedCount = result.skippedCount,
                        errors = result.errors
                    )
                }
                is ImportResult.Error -> {
                    _importExportState.value = ImportExportState.Error(result.message)
                }
            }
        }
    }

    fun validateBackup(uri: Uri): ValidationResult? {
        return try {
            val context = getApplication<Application>()
            val jsonString = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader().readText()
            } ?: return null
            settingsRepository.validateSettingsBackup(jsonString)
        } catch (_: Exception) {
            null
        }
    }

    fun resetImportExportState() {
        _importExportState.value = ImportExportState.Idle
    }

    suspend fun willPageChangeAffectItems(newPageCount: Int): Boolean {
        val currentLayout = settingsRepository.getHomeLayout().first()
        if (newPageCount >= currentLayout.pageCount) return false
        return currentLayout.items.any { it.page >= newPageCount }
    }
}

sealed class ImportExportState {
    object Idle : ImportExportState()
    object Loading : ImportExportState()
    object ExportSuccess : ImportExportState()
    data class ImportSuccess(
        val appliedCount: Int,
        val skippedCount: Int,
        val errors: List<Pair<String, String>>
    ) : ImportExportState()
    data class Error(val message: String) : ImportExportState()
}