package app.cclauncher.ui

import app.cclauncher.data.AppModel

/**
 * Home screen UI state
 */
//data class HomeScreenUiState(
//    val homeAppsNum: Int = 0,
//    val homeScreenColumns: Int = 1,
//    val homeAlignment: Int = Gravity.CENTER,
//    val homeBottomAlignment: Boolean = false,
//    val homeApps: List<AppModel?> = emptyList(),
//    val isLoading: Boolean = false,
//    val error: String? = null
//)

/**
 * App drawer UI state
 */
data class AppDrawerUiState(
    val apps: List<AppModel> = emptyList(),
    val filteredApps: List<AppModel> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

//data class AppUiModel(
//    val id: String,
//    val label: String,
//    val packageName: String,
//    val icon: ImageBitmap? = null,
//    val isHidden: Boolean = false,
//    val activityClassName: String? = null,
//    val userHandle: UserHandle
//)
//
//data class HomeAppUiModel(
//    val id: String,
//    val label: String,
//    val packageName: String,
//    val activityClassName: String? = null,
//    val userHandle: UserHandle,
//    val isInstalled: Boolean = true
//)