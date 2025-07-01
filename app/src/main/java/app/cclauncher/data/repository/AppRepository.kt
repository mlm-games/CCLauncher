package app.cclauncher.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import app.cclauncher.data.AppModel
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
    private val settingsRepository: SettingsRepository,
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
                val apps = getAppsList(context, settingsRepository, includeRegularApps = true, includeHiddenApps = false)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    val privateSpaceHelper = PrivateSpaceHelper(context)

                    // Only filter private space apps if private space is locked
                    if (privateSpaceHelper.isPrivateSpaceLocked()) {
                        val privateSpaceUser = privateSpaceHelper.getPrivateSpaceUser()
                        if (privateSpaceUser != null) {
                            _appList.value = apps.filter { app ->
                                app.user != privateSpaceUser
                            }
                            return@withContext
                        }
                    }
                }

                // If we reach here, either private space isn't relevant or we couldn't filter
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
     * Exception for app launch failures
     */
    class AppLaunchException(message: String, cause: Throwable? = null) : Exception(message, cause)
}