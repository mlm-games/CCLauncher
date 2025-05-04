package app.cclauncher.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserManager
import app.cclauncher.data.AppModel
import app.cclauncher.helper.IconCache
import app.cclauncher.helper.getAppsList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

/**
 * Repository for app-related operations
 */
class AppRepository(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    private val iconCache = IconCache(context)

    private val _appListAll = MutableStateFlow<List<AppModel>>(emptyList())
    val appListAll: StateFlow<List<AppModel>> = _appListAll.asStateFlow()

    private val _appList = MutableStateFlow<List<AppModel>>(emptyList())
    val appList: StateFlow<List<AppModel>> = _appList.asStateFlow()

    private val _hiddenApps = MutableStateFlow<List<AppModel>>(emptyList())
    val hiddenApps: StateFlow<List<AppModel>> = _hiddenApps.asStateFlow()

    suspend fun loadAllApps() {
        withContext(Dispatchers.IO) {
            try {
                val apps = getAppsList(context, settingsRepository, includeRegularApps = true, includeHiddenApps = true, includeAppIcons = true)
                _appListAll.value = apps
            } catch (e: Exception) {
                throw e
            }
        }
    }

    /**
     * Load all visible apps
     */
    suspend fun loadApps() {
        withContext(Dispatchers.IO) {
            try {
                val apps = getAppsList(context, settingsRepository, includeRegularApps = true, includeHiddenApps = false, includeAppIcons = true)
                _appList.value = apps
            } catch (e: Exception) {
                throw e
            }
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
                val appKey = "${app.appPackage}/${app.user.hashCode()}"

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

    /**
     * Search apps by query
     */
    suspend fun searchApps(query: String): List<AppModel> {
        return withContext(Dispatchers.Default) {
            if (query.isBlank()) {
                _appList.value
            } else {
                _appList.value.filter {
                    it.appLabel.contains(query, ignoreCase = true)
                }
            }
        }
    }

    /**
     * Check if app is hidden
     */
    suspend fun isAppHidden(app: AppModel): Boolean {
        val settings = settingsRepository.settings.first()
        val appKey = "${app.appPackage}/${app.user}"
        return settings.hiddenApps.contains(appKey)
    }

    /**
     * Clear app cache
     */
    fun clearCache() {
        iconCache.clearCache()
    }

    /**
     * Exception for app launch failures
     */
    class AppLaunchException(message: String, cause: Throwable? = null) : Exception(message, cause)
}