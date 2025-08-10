package app.cclauncher.ui

import app.cclauncher.data.AppModel


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
