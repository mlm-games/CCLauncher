package app.cclauncher.settings

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import app.cclauncher.data.Constants
import app.cclauncher.data.HomeLayout
import io.github.mlmgames.settings.core.SettingsRepository
import io.github.mlmgames.settings.core.backup.DeviceInfo
import io.github.mlmgames.settings.core.backup.ExportResult
import io.github.mlmgames.settings.core.backup.ImportOptions
import io.github.mlmgames.settings.core.backup.ImportResult
import io.github.mlmgames.settings.core.backup.SettingsBackupManager
import io.github.mlmgames.settings.core.backup.ValidationResult
import io.github.mlmgames.settings.core.datastore.createSettingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import java.io.File
import java.io.FileOutputStream
import kotlin.time.Clock

class AppSettingsRepository(private val context: Context): KoinComponent {

    private val dataStore = createSettingsDataStore(context, name = "app.cclauncher.settings")
    private val repo = SettingsRepository(dataStore, AppSettingsSchema)

    private val backupManager by lazy {
        val packageInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: Exception) {
            null
        }
        val versionName = packageInfo?.versionName ?: "unknown"

        SettingsBackupManager(
            dataStore = dataStore,
            schema = AppSettingsSchema,
            appId = "app.cclauncher",
            schemaVersion = 1,
            deviceInfoProvider = {
                DeviceInfo(
                    platform = "Android",
                    osVersion = "API ${Build.VERSION.SDK_INT}",
                    appVersion = versionName
                )
            }
        )
    }

    val settings: Flow<AppSettings> = repo.flow

    suspend fun updateSetting(propertyName: String, value: Any) {
        repo.set(propertyName, value)
    }

    suspend fun updateSetting(update: (AppSettings) -> AppSettings) {
        repo.update(update)
    }

    fun getHomeLayout(): Flow<HomeLayout> =
        repo.observeField("homeLayout")

    suspend fun saveHomeLayout(layout: HomeLayout) {
        repo.set("homeLayout", layout)
    }

    suspend fun triggerHomeLayoutRefresh() {
        val currentLayout = getHomeLayout().first()
        saveHomeLayout(currentLayout)
    }

    suspend fun setSwipeLeftApp(app: AppPreference) = repo.set("swipeLeftApp", app)
    suspend fun setSwipeRightApp(app: AppPreference) = repo.set("swipeRightApp", app)
    suspend fun setSwipeUpApp(app: AppPreference) = repo.set("swipeUpApp", app)
    suspend fun setSwipeDownApp(app: AppPreference) = repo.set("swipeDownApp", app)

    suspend fun getSwipeLeftApp(): AppPreference = settings.first().swipeLeftApp
    suspend fun getSwipeRightApp(): AppPreference = settings.first().swipeRightApp

    suspend fun setSettingsLock(locked: Boolean) = repo.set("lockSettings", locked)
    suspend fun setSettingsLockPin(pin: String) = repo.set("settingsLockPin", pin)
    suspend fun validateSettingsPin(pin: String): Boolean = settings.first().settingsLockPin == pin

    suspend fun setCustomFont(uri: Uri) {
        try {
            val fontFile = File(context.filesDir, Constants.CUSTOM_FONT_FILENAME)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(fontFile).use { output -> input.copyTo(output) }
            }
            repo.set("customFontPath", fontFile.absolutePath)
        } catch (e: Exception) {
            Log.e("SettingsRepo", "Failed to copy font file", e)
        }
    }

    suspend fun clearCustomFont() {
        val currentPath = settings.first().customFontPath
        if (currentPath.isNotEmpty()) {
            try { File(currentPath).delete() }
            catch (e: Exception) { Log.e("SettingsRepo", "Error deleting old font file", e) }
        }
        repo.set("customFontPath", "")
    }

    suspend fun toggleAppHidden(packageKey: String) {
        repo.update { s ->
            val set = s.hiddenApps.toMutableSet()
            if (set.contains(packageKey)) set.remove(packageKey) else set.add(packageKey)
            s.copy(hiddenApps = set)
        }
    }

    suspend fun setAppCustomName(appKey: String, customName: String) {
        repo.update { s ->
            val map = s.renamedApps.toMutableMap()
            if (customName.isBlank()) map.remove(appKey) else map[appKey] = customName
            s.copy(renamedApps = map)
        }
    }

    suspend fun removeAppCustomName(appKey: String) {
        repo.update { s ->
            val map = s.renamedApps.toMutableMap()
            map.remove(appKey)
            s.copy(renamedApps = map)
        }
    }

    suspend fun updateAppLaunchTime(appKey: String) {
        repo.update { s ->
            val history = s.recentAppHistory.toMutableMap()
            history[appKey] = Clock.System.now().toEpochMilliseconds()
            if (history.size > 100) {
                val oldest = history.entries.sortedBy { it.value }.take(20)
                oldest.forEach { history.remove(it.key) }
            }
            s.copy(recentAppHistory = history)
        }
    }

    suspend fun setFirstOpen(value: Boolean) = repo.set("firstOpen", value)
    suspend fun setAppTheme(value: Int) = repo.set("appTheme", value)

    // Import/Export functionality
    suspend fun exportSettings(): ExportResult {
        return backupManager.export()
    }

    suspend fun importSettings(jsonString: String, options: ImportOptions = ImportOptions()): ImportResult {
        return backupManager.import(jsonString, options)
    }

    fun validateSettingsBackup(jsonString: String): ValidationResult {
        return backupManager.validate(jsonString)
    }

    suspend fun exportSettingsToUri(uri: Uri): Result<Unit> {
        return try {
            when (val result = exportSettings()) {
                is ExportResult.Success -> {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(result.json.toByteArray(Charsets.UTF_8))
                    } ?: return Result.failure(Exception("Could not open output stream"))
                    Result.success(Unit)
                }
                is ExportResult.Error -> {
                    Result.failure(Exception(result.message))
                }
            }
        } catch (e: Exception) {
            Log.e("SettingsRepo", "Failed to export settings to URI", e)
            Result.failure(e)
        }
    }

    suspend fun importSettingsFromUri(uri: Uri): ImportResult {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader().readText()
            } ?: return ImportResult.Error(
                io.github.mlmgames.settings.core.backup.ImportError.PARSE_ERROR,
                "Could not read file"
            )
            importSettings(jsonString)
        } catch (e: Exception) {
            Log.e("SettingsRepo", "Failed to import settings from URI", e)
            ImportResult.Error(
                io.github.mlmgames.settings.core.backup.ImportError.PARSE_ERROR,
                e.message ?: "Unknown error"
            )
        }
    }
}