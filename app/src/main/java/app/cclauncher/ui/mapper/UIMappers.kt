package app.cclauncher.ui.mapper

import android.content.Context
import android.os.UserHandle
import app.cclauncher.data.AppModel
import app.cclauncher.data.HomeAppPreference
import app.cclauncher.helper.getUserHandleFromString
import app.cclauncher.helper.isPackageInstalled
import app.cclauncher.ui.state.AppUiModel
import app.cclauncher.ui.state.HomeAppUiModel
import app.cclauncher.ui.state.HomeScreenUiState

/**
 * Mapper functions for converting between domain models and UI models
 */

/**
 * Convert AppModel to AppUiModel
 */
fun AppModel.toUiModel(): AppUiModel {
    return AppUiModel(
        id = this.getKey(),
        label = this.appLabel,
        packageName = this.appPackage,
        icon = this.appIcon,
        isHidden = this.isHidden,
        activityClassName = this.activityClassName,
        userHandle = this.user
    )
}

/**
 * Convert AppUiModel back to domain model
 */
fun AppUiModel.toDomainModel(): AppModel {
    return AppModel(
        appLabel = this.label,
        key = null, // Would need to recreate the collation key
        appPackage = this.packageName,
        activityClassName = this.activityClassName,
        user = this.userHandle,
        appIcon = this.icon,
        isHidden = this.isHidden
    )
}

/**
 * Convert AppModel to HomeAppUiModel
 */
fun AppModel.toHomeAppUiModel(context: Context): HomeAppUiModel {
    val isInstalled = isPackageInstalled(context, this.appPackage, this.user.toString())

    return HomeAppUiModel(
        id = this.getKey(),
        label = this.appLabel,
        packageName = this.appPackage,
        activityClassName = this.activityClassName,
        userHandle = this.user,
        isInstalled = isInstalled
    )
}

/**
 * Convert HomeAppUiModel back to AppModel
 */
fun HomeAppUiModel.toDomainModel(): AppModel {
    return AppModel(
        appLabel = this.label,
        key = null,
        appPackage = this.packageName,
        activityClassName = this.activityClassName,
        user = this.userHandle
    )
}

/**
 * Convert HomeAppPreference to AppModel
 */
fun HomeAppPreference.toDomainModel(context: Context): AppModel? {
    if (this.packageName.isEmpty()) return null

    val userHandle = getUserHandleFromString(context, this.userString)
    return AppModel(
        appLabel = this.label,
        key = null,
        appPackage = this.packageName,
        activityClassName = this.activityClassName,
        user = userHandle
    )
}

/**
 * Convert AppModel to HomeAppPreference
 */
fun AppModel.toHomeAppPreference(): HomeAppPreference {
    return HomeAppPreference(
        label = this.appLabel,
        packageName = this.appPackage,
        activityClassName = this.activityClassName,
        userString = this.user.toString()
    )
}

/**
 * Map a list of AppModels to HomeScreenUiState
 */
fun mapToHomeScreenState(
    apps: List<AppModel>,
    homeAppsNum: Int,
    dateTimeVisibility: Int,
    homeAlignment: Int,
    homeBottomAlignment: Boolean,
    context: Context
): HomeScreenUiState {
    return HomeScreenUiState(
        homeAppsNum = homeAppsNum,
        dateTimeVisibility = dateTimeVisibility,
        homeAlignment = homeAlignment,
        homeBottomAlignment = homeBottomAlignment,
        homeApps = apps.take(homeAppsNum).map {
            it.toHomeAppUiModel(context).takeIf { model ->
                model.isInstalled
            }?.toDomainModel()
        }
    )
}