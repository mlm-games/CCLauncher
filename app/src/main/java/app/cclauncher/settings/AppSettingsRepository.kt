package app.cclauncher.settings

import android.content.Context
import android.content.pm.PackageManager
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.time.Clock

class AppSettingsRepository(private val context: Context) {

    companion object {
        private const val TAG = "SettingsRepo"
        private const val MAX_FONT_BYTES = 5 * 1024 * 1024L
        private const val MAX_IMPORT_BYTES = 2 * 1024 * 1024L
    }

    private val appContext = context.applicationContext
    private val dataStore = createSettingsDataStore(appContext, name = "app.cclauncher.settings")
    private val repo = SettingsRepository(dataStore, AppSettingsSchema)

    private val backupManager by lazy {
        val packageInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.packageManager.getPackageInfo(
                    appContext.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            }
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

    suspend fun setSwipeLeftApp(app: AppPreference) { repo.update { it.copy(swipeLeftApp = app) } }

    suspend fun setSwipeRightApp(app: AppPreference) { repo.update { it.copy(swipeRightApp = app) } }

    suspend fun setSwipeUpApp(app: AppPreference) { repo.update { it.copy(swipeUpApp = app) } }

    suspend fun setSwipeDownApp(app: AppPreference) { repo.update { it.copy(swipeDownApp = app) } }

    suspend fun getSwipeLeftApp(): AppPreference = settings.first().swipeLeftApp
    suspend fun getSwipeRightApp(): AppPreference = settings.first().swipeRightApp

    suspend fun setSettingsLock(locked: Boolean) = repo.set("lockSettings", locked)

    /**
     * Store only a salted SHA-256 hash (format "saltHex:hashHex"), never the plaintext PIN.
     * Breaking change vs old plaintext values — old PINs will no longer validate (acceptable pre-launch).
     */
    suspend fun setSettingsLockPin(pin: String) {
        if (pin.isEmpty()) {
            repo.set("settingsLockPin", "")
            return
        }
        repo.set("settingsLockPin", hashPin(pin))
    }

    suspend fun validateSettingsPin(pin: String): Boolean {
        if (pin.isEmpty()) return false
        val stored = settings.first().settingsLockPin
        if (stored.isEmpty()) return false
        // Back-compat: very old installs stored plaintext; migrate on successful match.
        if (!stored.contains(":")) {
            val matches = MessageDigest.isEqual(
                stored.toByteArray(Charsets.UTF_8),
                pin.toByteArray(Charsets.UTF_8)
            )
            if (matches) setSettingsLockPin(pin)
            return matches
        }
        return verifyPin(pin, stored)
    }

    private fun hashPin(pin: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = sha256(salt + pin.toByteArray(Charsets.UTF_8))
        return "${salt.toHex()}:${hash.toHex()}"
    }

    private fun verifyPin(pin: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 2) return false
        val salt = parts[0].hexToBytes() ?: return false
        val expected = parts[1].hexToBytes() ?: return false
        val actual = sha256(salt + pin.toByteArray(Charsets.UTF_8))
        return MessageDigest.isEqual(expected, actual)
    }

    private fun sha256(input: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(input)

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray? = runCatching {
        require(length % 2 == 0)
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }.getOrNull()

    suspend fun setCustomFont(uri: Uri) = withContext(Dispatchers.IO) {
        try {
            val type = appContext.contentResolver.getType(uri)
            if (type != null && !type.startsWith("font/") &&
                !type.startsWith("application/") && !type.startsWith("text/")
            ) {
                Log.w(TAG, "Rejected font with unexpected mime type: $type")
                return@withContext
            }
            val fontFile = File(appContext.filesDir, Constants.CUSTOM_FONT_FILENAME)
            // Bounded copy — reject >5MB to avoid zip-bomb/OOM.
            var total = 0L
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(fontFile).use { output ->
                    val buf = ByteArray(8 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        total += n
                        if (total > MAX_FONT_BYTES) throw IllegalArgumentException("Font file too large")
                        output.write(buf, 0, n)
                    }
                }
            } ?: return@withContext
            repo.set("customFontPath", fontFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy font file", e)
        }
    }

    suspend fun clearCustomFont() {
        val currentPath = settings.first().customFontPath
        if (currentPath.isNotEmpty()) {
            try { File(currentPath).delete() }
            catch (e: Exception) { Log.e(TAG, "Error deleting old font file", e) }
        }
        repo.set("customFontPath", "")
    }

    suspend fun toggleAppHidden(packageKey: String) {
        toggleAppHidden(packageKey, emptySet())
    }

    suspend fun toggleAppHidden(appKey: String, legacyKeys: Set<String>) {
        repo.update { s ->
            val set = s.hiddenApps.toMutableSet()
            val candidates = (setOf(appKey) + legacyKeys)
                .filter { it.isNotBlank() }
                .distinct()

            val shouldUnhide = candidates.any { set.contains(it) }
            if (shouldUnhide) {
                candidates.forEach { set.remove(it) }
            } else {
                set.add(appKey)
            }
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

    suspend fun removeAppCustomNames(appKeys: Set<String>) {
        if (appKeys.isEmpty()) return
        repo.update { s ->
            val map = s.renamedApps.toMutableMap()
            appKeys.forEach { map.remove(it) }
            s.copy(renamedApps = map)
        }
    }

    suspend fun migrateAppKeys(migrations: List<AppKeyMigration>) {
        if (migrations.isEmpty()) return
        repo.update { s ->
            val renamed = s.renamedApps.toMutableMap()
            val hidden = s.hiddenApps.toMutableSet()
            val history = s.recentAppHistory.toMutableMap()

            val renamedOriginal = s.renamedApps
            val hiddenOriginal = s.hiddenApps
            val historyOriginal = s.recentAppHistory

            for (migration in migrations) {
                val newKey = migration.newKey
                val sourceKeys = (migration.moveKeys + migration.copyKeys)
                    .filter { it.isNotBlank() && it != newKey }
                    .distinct()

                if (sourceKeys.isEmpty()) continue

                val renameCandidate = sourceKeys.firstNotNullOfOrNull { renamedOriginal[it] }
                if (!renamed.containsKey(newKey) && renameCandidate != null) {
                    renamed[newKey] = renameCandidate
                }

                if (!hidden.contains(newKey) && sourceKeys.any { hiddenOriginal.contains(it) }) {
                    hidden.add(newKey)
                }

                val maxLaunchTime = sourceKeys.mapNotNull { historyOriginal[it] }.maxOrNull()
                if (maxLaunchTime != null) {
                    val existing = history[newKey]
                    if (existing == null || maxLaunchTime > existing) {
                        history[newKey] = maxLaunchTime
                    }
                }

                for (oldKey in migration.moveKeys) {
                    if (oldKey == newKey) continue
                    renamed.remove(oldKey)
                    hidden.remove(oldKey)
                    history.remove(oldKey)
                }
            }

            s.copy(
                renamedApps = renamed,
                hiddenApps = hidden,
                recentAppHistory = history
            )
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
                    appContext.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(result.json.toByteArray(Charsets.UTF_8))
                    } ?: return Result.failure(Exception("Could not open output stream"))
                    Result.success(Unit)
                }
                is ExportResult.Error -> {
                    Result.failure(Exception(result.message))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export settings to URI", e)
            Result.failure(e)
        }
    }

    suspend fun importSettingsFromUri(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        try {
            var total = 0L
            val sb = StringBuilder()
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                val reader = input.bufferedReader()
                val buf = CharArray(8 * 1024)
                while (true) {
                    val n = reader.read(buf)
                    if (n <= 0) break
                    total += n * 2L
                    if (total > MAX_IMPORT_BYTES) {
                        return@withContext ImportResult.Error(
                            io.github.mlmgames.settings.core.backup.ImportError.PARSE_ERROR,
                            "Backup file too large"
                        )
                    }
                    sb.append(buf, 0, n)
                }
            } ?: return@withContext ImportResult.Error(
                io.github.mlmgames.settings.core.backup.ImportError.PARSE_ERROR,
                "Could not read file"
            )
            importSettings(sb.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import settings from URI", e)
            ImportResult.Error(
                io.github.mlmgames.settings.core.backup.ImportError.PARSE_ERROR,
                e.message ?: "Unknown error"
            )
        }
    }
}
