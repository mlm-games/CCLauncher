package app.cclauncher.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.asImageBitmap
import app.cclauncher.data.AppShortcut
import app.cclauncher.data.AppModel
import app.cclauncher.data.AppKey
import app.cclauncher.settings.AppKeyMigration
import app.cclauncher.helper.BitmapUtils
import app.cclauncher.settings.AppSettingsRepository
import app.cclauncher.helper.IconCache
import app.cclauncher.helper.PrivateSpaceHelper
import app.cclauncher.helper.getAppsList
import app.cclauncher.data.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Repository for app-related operations
 */
class AppRepository(
    private val context: Context,
    private val settingsRepository: AppSettingsRepository,
    private val iconCache: IconCache,
    coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "AppRepository"
    }

    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    private val loadMutex = Mutex()

    private val _appListAll = MutableStateFlow<List<AppModel>>(emptyList())
    val appListAll: StateFlow<List<AppModel>> = _appListAll.asStateFlow()

    private val _appList = MutableStateFlow<List<AppModel>>(emptyList())
    val appList: StateFlow<List<AppModel>> = _appList.asStateFlow()

    private val _hiddenApps = MutableStateFlow<List<AppModel>>(emptyList())
    val hiddenApps: StateFlow<List<AppModel>> = _hiddenApps.asStateFlow()


    init {
        // Reload apps when icon pack changes
        coroutineScope.launch {
            settingsRepository.settings
                .map { it.selectedIconPack }
                .distinctUntilChanged()
                .drop(1) // Skip initial value
                .collect {
                    // Reload apps to get new icons
                    loadApps(forceEmit = true)
                }
        }
    }

    /**
     * Load all visible apps. Mutex is acquired inside IO dispatcher (not held across dispatch).
     */
    suspend fun loadApps(forceEmit: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        loadMutex.withLock {
            try {
                val settings = settingsRepository.settings.first()
                val sortOrder = settings.searchSortOrder

                val allMobileApps = getAppsList(
                    appContext, settingsRepository, iconCache,
                    includeRegularApps = true, includeHiddenApps = true
                )

                val visibleMobileApps = allMobileApps.filter { !it.isHidden }
                val hiddenMobileApps = allMobileApps.filter { it.isHidden }

                val systemShortcuts = if (settings.showPinnedShortcuts) {
                    loadSystemShortcuts(settings.renamedApps)
                } else {
                    emptyList()
                }

                val combinedVisible = visibleMobileApps + systemShortcuts
                val combinedAll = allMobileApps + systemShortcuts

                val visibleList = sortApps(combinedVisible, sortOrder)
                val fullList = sortApps(combinedAll, sortOrder)

                Log.d(TAG, "Loaded ${visibleList.size} visible, ${hiddenMobileApps.size} hidden")

                var finalVisibleList = visibleList
                var finalFullList = fullList

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    val privateSpaceHelper = PrivateSpaceHelper(appContext)
                    if (privateSpaceHelper.isPrivateSpaceLocked()) {
                        val privateSpaceUser = privateSpaceHelper.getPrivateSpaceUser()
                        if (privateSpaceUser != null) {
                            finalVisibleList = visibleList.filter { it.user != privateSpaceUser }
                            finalFullList = fullList.filter { it.user != privateSpaceUser }
                        }
                    }
                }

                var changed = false

                if (forceEmit || !sameAppList(_appList.value, finalVisibleList)) {
                    _appList.value = finalVisibleList
                    changed = true
                }

                if (forceEmit || !sameAppList(_appListAll.value, finalFullList)) {
                    _appListAll.value = finalFullList
                    changed = true
                }

                if (forceEmit || !sameAppList(_hiddenApps.value, hiddenMobileApps)) {
                    _hiddenApps.value = hiddenMobileApps
                    changed = true
                }

                changed

            } catch (e: Exception) {
                Log.e(TAG, "Error loading apps", e)
                false
            }
        }
    }

    private fun List<AppModel>.uiSignature(): List<String> =
        map { app ->
            buildString {
                append(app.getKey())
                append('|')
                append(app.appLabel)
                append('|')
                append(app.isHidden)
                append('|')
                append(app.lastLaunchTime)
                append('|')
                append(app.isNew)
                append('|')
                append(app.isSystemShortcut)
            }
        }

    private fun sameAppList(
        oldList: List<AppModel>,
        newList: List<AppModel>
    ): Boolean = oldList.uiSignature() == newList.uiSignature()

    private fun sortApps(list: List<AppModel>, sortOrder: Int): List<AppModel> {
        return when (sortOrder) {
            Constants.SortOrder.REVERSE_ALPHABETICAL ->
                list.sortedByDescending { it.appLabel.lowercase(Locale.ROOT) }

            Constants.SortOrder.RECENT_FIRST ->
                list.sortedWith(
                    compareByDescending<AppModel> { it.lastLaunchTime }
                        .thenBy { it.appLabel.lowercase(Locale.ROOT) }
                )

            else -> // ALPHABETICAL
                list.sortedBy { it.appLabel.lowercase(Locale.ROOT) }
        }
    }

    /**
     * Batch shortcut query per user (one IPC per profile instead of per shortcut).
     */
    private suspend fun queryPinnedShortcutsByUser(): Map<android.os.UserHandle, List<android.content.pm.ShortcutInfo>> =
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return@withContext emptyMap()
            if (!launcherApps.hasShortcutHostPermission()) {
                Log.d(TAG, "No shortcut host permission (not default launcher?)")
                return@withContext emptyMap()
            }
            val userManager = appContext.getSystemService(Context.USER_SERVICE) as android.os.UserManager
            val out = mutableMapOf<android.os.UserHandle, List<android.content.pm.ShortcutInfo>>()
            for (user in userManager.userProfiles) {
                try {
                    val query = LauncherApps.ShortcutQuery()
                    query.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
                    out[user] = launcherApps.getShortcuts(query, user).orEmpty()
                } catch (_: SecurityException) {
                    // if not the default launcher
                } catch (e: Exception) {
                    Log.e(TAG, "Error querying shortcuts for user $user", e)
                }
            }
            out
        }

    private suspend fun loadSystemShortcuts(renamedApps: Map<String, String>): List<AppModel> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return emptyList()
        val list = mutableListOf<AppModel>()
        try {
            val byUser = queryPinnedShortcutsByUser()
            for ((user, shortcuts) in byUser) {
                val userString = user.toString()
                for (shortcut in shortcuts) {
                    val appKey = AppKey.shortcutKey(shortcut.`package`, shortcut.id, userString)
                    val legacyKey = AppKey.legacyShortcutKey(shortcut.`package`, shortcut.id, user.hashCode())
                    val label = shortcut.shortLabel?.toString() ?: shortcut.id
                    val shownLabel = listOf(appKey, legacyKey)
                        .firstNotNullOfOrNull { renamedApps[it] }
                        ?: label
                    val iconDrawable = runCatching {
                        launcherApps.getShortcutIconDrawable(
                            shortcut,
                            appContext.resources.displayMetrics.densityDpi
                        )
                    }.getOrNull()
                    val iconBitmap = BitmapUtils.drawableToBitmap(iconDrawable)?.asImageBitmap()

                    list.add(
                        AppModel(
                            appLabel = shownLabel,
                            appPackage = shortcut.`package`,
                            activityClassName = null,
                            user = user,
                            appIcon = iconBitmap,
                            isSystemShortcut = true,
                            systemShortcutId = shortcut.id,
                            systemShortcutPackage = shortcut.`package`,
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading system shortcuts", e)
        }
        return list
    }

    suspend fun getAppShortcuts(app: AppModel): List<AppShortcut> {
        return withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return@withContext emptyList()
            if (app.isSystemShortcut) return@withContext emptyList()
            if (!launcherApps.hasShortcutHostPermission()) return@withContext emptyList()

            try {
                val query = LauncherApps.ShortcutQuery()
                    .setPackage(app.appPackage)
                    .setQueryFlags(
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                    )

                val shortcuts = launcherApps.getShortcuts(query, app.user).orEmpty()

                shortcuts.map { shortcut ->
                    val label = shortcut.shortLabel?.toString()
                        ?: shortcut.longLabel?.toString()
                        ?: shortcut.id

                    val iconDrawable = runCatching {
                        launcherApps.getShortcutIconDrawable(
                            shortcut,
                            appContext.resources.displayMetrics.densityDpi
                        )
                    }.getOrNull()
                    val iconBitmap = BitmapUtils.drawableToBitmap(iconDrawable)?.asImageBitmap()

                    AppShortcut(
                        id = shortcut.id,
                        packageName = shortcut.`package`,
                        label = label,
                        user = app.user,
                        icon = iconBitmap
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading shortcuts for ${app.appPackage}", e)
                emptyList()
            }
        }
    }

    /** Binder call — must run on IO. */
    suspend fun getDefaultAppLabel(app: AppModel): String? = withContext(Dispatchers.IO) {
        if (app.isSystemShortcut) return@withContext null
        try {
            val activities = launcherApps.getActivityList(app.appPackage, app.user)
            val target = activities.firstOrNull {
                it.componentName.className == app.activityClassName
            } ?: activities.firstOrNull()
            target?.label?.toString()
        } catch (e: Exception) {
            Log.w(TAG, "getDefaultAppLabel failed for ${app.appPackage}", e)
            null
        }
    }

    /**
     * Load hidden apps
     */
    suspend fun loadHiddenApps() {
        withContext(Dispatchers.IO) {
            val hiddenApps = getAppsList(
                appContext, settingsRepository, iconCache,
                includeRegularApps = false, includeHiddenApps = true
            )
            _hiddenApps.value = hiddenApps
        }
    }

    /**
     * Toggle app hidden state
     */
    suspend fun toggleAppHidden(app: AppModel) {
        withContext(Dispatchers.IO) {
            val appKey = app.getKey()
            val legacyMoveKeys = AppKey.legacyMoveKeysForApp(app)
            val legacyCopyKeys = AppKey.legacyCopyKeysForApp(app)
            val legacyKeys = legacyMoveKeys + legacyCopyKeys

            settingsRepository.toggleAppHidden(appKey, legacyKeys)

            if (legacyMoveKeys.isNotEmpty() || legacyCopyKeys.isNotEmpty()) {
                val settings = settingsRepository.settings.first()
                val hasNewRename = settings.renamedApps.containsKey(appKey)
                val hasNewHidden = settings.hiddenApps.contains(appKey)
                val newHistory = settings.recentAppHistory[appKey]

                val legacyRename = legacyCopyKeys.firstNotNullOfOrNull { settings.renamedApps[it] }
                val legacyHidden = legacyCopyKeys.any { settings.hiddenApps.contains(it) }
                val legacyHistory = legacyCopyKeys.mapNotNull { settings.recentAppHistory[it] }.maxOrNull()

                val shouldCopy = (!hasNewRename && legacyRename != null) ||
                    (!hasNewHidden && legacyHidden) ||
                    (legacyHistory != null && (newHistory == null || legacyHistory > newHistory))

                val copyKeys = if (shouldCopy) legacyCopyKeys else emptySet()

                settingsRepository.migrateAppKeys(
                    listOf(
                        AppKeyMigration(
                            newKey = appKey,
                            moveKeys = legacyMoveKeys,
                            copyKeys = copyKeys
                        )
                    )
                )
            }

            loadApps(forceEmit = true)
        }
    }

    /**
     * Launch an app. Binder IPC runs on IO; callers stay on Main for UI.
     */
    suspend fun launchApp(appModel: AppModel) {
        withContext(Dispatchers.IO) {
            try {
                if (appModel.isSystemShortcut) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
                        throw AppLaunchException("Shortcuts require Android 7.1 or higher")
                    }
                    if (!launcherApps.hasShortcutHostPermission()) {
                        throw AppLaunchException("Set CCLauncher as the default launcher to open shortcuts")
                    }

                    val shortcutId = appModel.systemShortcutId
                    val shortcutPackage = appModel.systemShortcutPackage
                    if (shortcutId.isNullOrBlank() || shortcutPackage.isNullOrBlank()) {
                        throw AppLaunchException("Shortcut not available")
                    }

                    launcherApps.startShortcut(shortcutPackage, shortcutId, null, null, appModel.user)
                    return@withContext
                }

                val activityClassName = appModel.activityClassName
                if (activityClassName.isNullOrBlank()) {
                    throw AppLaunchException("App component not found for ${appModel.appLabel}")
                }

                val component = ComponentName(
                    appModel.appPackage,
                    activityClassName
                )
                launcherApps.startMainActivity(component, appModel.user, null, null)
            } catch (e: SecurityException) {
                throw AppLaunchException("Security error launching ${appModel.appLabel}", e)
            } catch (e: NullPointerException) {
                throw AppLaunchException("App component not found for ${appModel.appLabel}", e)
            } catch (e: AppLaunchException) {
                throw e
            } catch (e: Exception) {
                throw AppLaunchException("Failed to launch ${appModel.appLabel}", e)
            }
        }
    }

    /** Binder call — must run on IO. */
    suspend fun deletePinnedShortcut(
        packageName: String,
        shortcutId: String,
        user: android.os.UserHandle
    ) = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return@withContext

        if (!launcherApps.hasShortcutHostPermission()) {
            Log.w(TAG, "No shortcut host permission")
            return@withContext
        }

        try {
            val query = LauncherApps.ShortcutQuery()
                .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
                .setPackage(packageName)

            val pinned = launcherApps.getShortcuts(query, user).orEmpty()
            val remainingIds = pinned.mapNotNull { it.id }.filter { it != shortcutId }.toMutableList()

            launcherApps.pinShortcuts(packageName, remainingIds, user)
            Log.d(TAG, "Deleted pinned shortcut: $shortcutId from $packageName")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting pinned shortcut", e)
            throw e
        }
    }

    /**
     * Exception for app launch failures
     */
    class AppLaunchException(message: String, cause: Throwable? = null) : Exception(message, cause)
}
