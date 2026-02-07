package app.cclauncher.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.asImageBitmap
import app.cclauncher.data.AppModel
import app.cclauncher.data.AppKey
import app.cclauncher.settings.AppKeyMigration
import app.cclauncher.helper.BitmapUtils
import app.cclauncher.settings.AppSettingsRepository
import app.cclauncher.helper.PrivateSpaceHelper
import app.cclauncher.helper.getAppsList
import app.cclauncher.data.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Repository for app-related operations
 */
class AppRepository(
    private val context: Context,
    private val settingsRepository: AppSettingsRepository,
    coroutineScope: CoroutineScope
) {
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

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
                    loadApps()
                    loadHiddenApps()
                }
        }
    }

    /**
     * Load all visible apps
     */
    suspend fun loadApps() {
        withContext(Dispatchers.IO) {
            try {
                val settings = settingsRepository.settings.first()
                val sortOrder = settings.searchSortOrder

                val allMobileApps = getAppsList(context, settingsRepository, includeRegularApps = true, includeHiddenApps = true)
                
                val visibleMobileApps = allMobileApps.filter { !it.isHidden }
                val hiddenMobileApps = allMobileApps.filter { it.isHidden }
                
                val systemShortcuts = loadSystemShortcuts()

                val combinedVisible = visibleMobileApps + systemShortcuts
                val combinedAll = allMobileApps + systemShortcuts

                val visibleList = sortApps(combinedVisible, sortOrder)
                val fullList = sortApps(combinedAll, sortOrder)

                Log.d("AppRepository", "Loaded ${visibleList.size} visible, ${hiddenMobileApps.size} hidden")

                var finalVisibleList = visibleList
                var finalFullList = fullList

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    val privateSpaceHelper = PrivateSpaceHelper(context)
                    if (privateSpaceHelper.isPrivateSpaceLocked()) {
                        val privateSpaceUser = privateSpaceHelper.getPrivateSpaceUser()
                        if (privateSpaceUser != null) {
                            finalVisibleList = visibleList.filter { it.user != privateSpaceUser }
                            finalFullList = fullList.filter { it.user != privateSpaceUser }
                        }
                    }
                }

                _appList.value = finalVisibleList
                _appListAll.value = finalFullList
                _hiddenApps.value = hiddenMobileApps
                
            } catch (e: Exception) {
                Log.e("AppRepository", "Error loading apps", e)
                e.printStackTrace()
            }
        }
    }

    private fun sortApps(list: List<AppModel>, sortOrder: Int): List<AppModel> {
        return when (sortOrder) {
            Constants.SortOrder.REVERSE_ALPHABETICAL -> 
                list.sortedByDescending { it.appLabel.lowercase() }
            
            Constants.SortOrder.RECENT_FIRST -> 
                list.sortedWith(
                    compareByDescending<AppModel> { it.lastLaunchTime }
                        .thenBy { it.appLabel.lowercase() }
                )
            
            else -> // ALPHABETICAL
                list.sortedBy { it.appLabel.lowercase() }
        }
    }

    private fun loadSystemShortcuts(): List<AppModel> {
        val list = mutableListOf<AppModel>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            try {
                val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

                if (!launcherApps.hasShortcutHostPermission()) {
                    Log.d("AppRepository", "No shortcut host permission (not default launcher?)")
                    return emptyList()
                }

                val query = LauncherApps.ShortcutQuery()
                query.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)

                val userManager = context.getSystemService(Context.USER_SERVICE) as android.os.UserManager
                for (user in userManager.userProfiles) {
                    try {
                        val shortcuts = launcherApps.getShortcuts(query, user) ?: emptyList()

                        for (shortcut in shortcuts) {
                            val iconDrawable = launcherApps.getShortcutIconDrawable(shortcut, context.resources.displayMetrics.densityDpi)
                            val iconBitmap = BitmapUtils.drawableToBitmap(iconDrawable)?.asImageBitmap()

                            list.add(
                                AppModel(
                                    appLabel = shortcut.shortLabel?.toString() ?: shortcut.id,
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
                    } catch (_: SecurityException) {
                        // if not the default launcher
                    }
                }
            } catch (e: Exception) {
                Log.e("AppRepository", "Error loading system shortcuts", e)
            }
        }
        return list
    }

    fun getDefaultAppLabel(app: AppModel): String? {
        if (app.isSystemShortcut) return null
        return try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val activities = launcherApps.getActivityList(app.appPackage, app.user)
            val target = activities.firstOrNull {
                it.componentName.className == app.activityClassName
            } ?: activities.firstOrNull()
            target?.label?.toString()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Load hidden apps
     */
    suspend fun loadHiddenApps() {
        withContext(Dispatchers.IO) {
            try {
                val hiddenApps = getAppsList(context, settingsRepository, includeRegularApps = false, includeHiddenApps = true)
                _hiddenApps.value = hiddenApps
            } catch (e: Exception) {
                throw e
            }
        }
    }

    /**
     * Toggle app hidden state
     */
    suspend fun toggleAppHidden(app: AppModel) {
        withContext(Dispatchers.IO) {
            try {
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

                loadApps()
                loadHiddenApps()
            } catch (e: Exception) {
                println("Error toggling app hidden state: ${e.message}")
                e.printStackTrace()
                throw e
            }
        }
    }

    /**
     * Launch an app
     */
    suspend fun launchApp(appModel: AppModel) {
        withContext(Dispatchers.Main) {
            try {
                val component = ComponentName(
                    appModel.appPackage,
                    appModel.activityClassName ?: ""
                )
                launcherApps.startMainActivity(component, appModel.user, null, null)
            } catch (e: SecurityException) {
                throw AppLaunchException("Security error launching ${appModel.appLabel}", e)
            } catch (e: NullPointerException) {
                throw AppLaunchException("App component not found for ${appModel.appLabel}", e)
            } catch (e: Exception) {
                throw AppLaunchException("Failed to launch ${appModel.appLabel}", e)
            }
        }
    }

    fun deletePinnedShortcut(packageName: String, shortcutId: String, user: android.os.UserHandle) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return

        if (!launcherApps.hasShortcutHostPermission()) {
            Log.w("AppRepository", "No shortcut host permission")
            return
        }

        try {
            val query = LauncherApps.ShortcutQuery()
                .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
                .setPackage(packageName)

            val pinned = launcherApps.getShortcuts(query, user).orEmpty()
            val remainingIds = pinned.mapNotNull { it.id }.filter { it != shortcutId }.toMutableList()

            launcherApps.pinShortcuts(packageName, remainingIds, user)
            Log.d("AppRepository", "Deleted pinned shortcut: $shortcutId from $packageName")
        } catch (e: Exception) {
            Log.e("AppRepository", "Error deleting pinned shortcut", e)
            throw e
        }
    }

    /**
     * Exception for app launch failures
     */
    class AppLaunchException(message: String, cause: Throwable? = null) : Exception(message, cause)
}
