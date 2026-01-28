package app.cclauncher.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.asImageBitmap
import app.cclauncher.data.AppModel
import app.cclauncher.helper.BitmapUtils
import app.cclauncher.settings.AppSettingsRepository
import app.cclauncher.helper.PrivateSpaceHelper
import app.cclauncher.helper.getAppsList
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
                val allMobileApps = getAppsList(context, settingsRepository, includeRegularApps = true, includeHiddenApps = true)

                val visibleMobileApps = allMobileApps.filter { !it.isHidden }
                val systemShortcuts = loadSystemShortcuts()

                val visibleList = (visibleMobileApps + systemShortcuts).sortedBy { it.appLabel.lowercase() }

                val fullList = (allMobileApps + systemShortcuts).sortedBy { it.appLabel.lowercase() }

                Log.d("AppRepository", "Loaded ${visibleList.size} visible, ${fullList.size} total apps/shortcuts")

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

            } catch (e: Exception) {
                Log.e("AppRepository", "Error loading apps", e)
                e.printStackTrace()
            }
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

                settingsRepository.toggleAppHidden(appKey)

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