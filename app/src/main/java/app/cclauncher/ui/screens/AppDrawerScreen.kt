package app.cclauncher.ui.screens

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.cclauncher.MainViewModel
import app.cclauncher.data.AppModel
import app.cclauncher.data.Constants
import app.cclauncher.helper.openSearch
import app.cclauncher.helper.openUrl
import app.cclauncher.ui.BackHandler
import app.cclauncher.ui.util.detectSwipeGestures
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import app.cclauncher.data.repository.SettingsRepository
import app.cclauncher.data.settings.AppSettings
import app.cclauncher.ui.viewmodels.SettingsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerScreen(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel = viewModel(), // Add this parameter
    onAppClick: (AppModel) -> Unit,
    selectionMode: Boolean = false,
    selectionTitle: String = "",
    onSwipeDown: () -> Unit,
) {
    BackHandler(onBack = onSwipeDown)

    val context = LocalContext.current
    val uiState by viewModel.appDrawerState.collectAsState()
    val settings by settingsViewModel.settingsState.collectAsState() // Use the settings state
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val autoShowKeyboard = settings.autoShowKeyboard
    val showAppNames = settings.showAppNames
    val showAppIcons = settings.showAppIcons
    val autoOpenFilteredApp = settings.autoOpenFilteredApp

    // Get current orientation to decide whether to show icons
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Determine if icons should be shown based on orientation settings
    val shouldShowIcons = if (showAppIcons) {
        if (isLandscape) settings.showIconsInLandscape else settings.showIconsInPortrait
    } else {
        false
    }

    // Get the item spacing
    val itemSpacing = when (settings.itemSpacing) {
        0 -> 0.dp
        1 -> 4.dp
        2 -> 8.dp
        3 -> 16.dp
        else -> 4.dp
    }

    // Get font size for search results
    val searchResultsFontSize = if (settings.searchResultsUseHomeFont) {
        settings.textSizeScale
    } else {
        settings.searchResultsFontSize
    }

    // Get font weight
    val fontWeight = when (settings.fontWeight) {
        0 -> androidx.compose.ui.text.font.FontWeight.Thin
        1 -> androidx.compose.ui.text.font.FontWeight.Light
        2 -> androidx.compose.ui.text.font.FontWeight.Normal
        3 -> androidx.compose.ui.text.font.FontWeight.Medium
        4 -> androidx.compose.ui.text.font.FontWeight.Bold
        5 -> androidx.compose.ui.text.font.FontWeight.Black
        else -> androidx.compose.ui.text.font.FontWeight.Normal
    }

    var selectedApp by remember { mutableStateOf<AppModel?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }

    // Force landscape if setting is enabled
    LaunchedEffect(settings.forceLandscapeMode) {
        (context as? Activity)?.let { activity ->
            if (settings.forceLandscapeMode) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }
    }

    // Load apps when screen is shown
    LaunchedEffect(Unit) {
        viewModel.loadApps()
    }

    // Update search results when query changes
    LaunchedEffect(searchQuery) {
        viewModel.searchApps(searchQuery)
    }

    // Auto-focus search field and show keyboard
    LaunchedEffect(Unit) {
        if (autoShowKeyboard) {
            delay(100) // Small delay to ensure UI is ready
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val scrollState = rememberLazyListState()
    var lastScrollIndex by remember { mutableIntStateOf(0) }
    var keyboardVisible by remember { mutableStateOf(autoShowKeyboard) }
    var lastScrollOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(scrollState) {
        snapshotFlow {
            Pair(scrollState.firstVisibleItemIndex, scrollState.firstVisibleItemScrollOffset)
        }.collect { (currentIndex, scrollOffset) ->
            if (currentIndex > lastScrollIndex) {
                // Scrolling down
                keyboardController?.hide()
                keyboardVisible = false
            } else if (currentIndex < lastScrollIndex) {
                // Scrolling up
                keyboardController?.show()
                keyboardVisible = true
            } else if (currentIndex == 0 && scrollOffset < -50 && lastScrollOffset >= -50) {
                // trying to scroll down further (overscroll)
                // Go back to home screen (for some reason, only works with search bar)
                onSwipeDown()
            }

            lastScrollIndex = currentIndex
            lastScrollOffset = scrollOffset
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .detectSwipeGestures(onSwipeDown = onSwipeDown)
        .statusBarsPadding()) {

        if (selectionMode) {
            TopAppBar(
                title = { Text(selectionTitle) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }

        // Search field
        AppDrawerSearch(
            searchQuery = searchQuery,
            onSearchChanged = { query ->
                searchQuery = query
                if (query.isEmpty()) {
                    coroutineScope.launch {
                        delay(10) // Updation delay
                        scrollState.scrollToItem(0)
                    }
                }
            },
            modifier = Modifier.focusRequester(focusRequester),
            onEnterPressed = {
                val appsToShow = if (searchQuery.isEmpty()) uiState.apps else uiState.filteredApps
                if (appsToShow.isNotEmpty()) {
                    onAppClick(appsToShow[0])
                }
            }
        )

        when {
            // Loading state
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Error state
            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error: ${uiState.error}")
                }
            }

            // Empty app list
            uiState.apps.isEmpty() && searchQuery.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No apps found")
                }
            }

            // Empty search results
            uiState.filteredApps.isEmpty() && searchQuery.isNotEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No apps found matching \"$searchQuery\"")

                        Button(
                            onClick = {
                                if (searchQuery.startsWith("!")) {
                                    context.openSearch(Constants.URL_DUCK_SEARCH + searchQuery.substring(1).replace(" ", "%20"))
                                } else {
                                    context.openSearch(searchQuery.trim())
                                }
                            },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Search Web")
                        }
                    }
                }
            }

            // Show filtered app list
            else -> {
                val appsToShow = if (searchQuery.isEmpty()) uiState.apps else uiState.filteredApps

                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(itemSpacing) // Apply item spacing
                ) {
                    items(
                        items = appsToShow,
                        key = { app -> "${app.appPackage}/${app.activityClassName ?: ""}/${app.user.hashCode()}" }
                    ) { app ->
                        AppListItem(
                            app = app,
                            showAppIcon = shouldShowIcons, // Use conditional icon display
                            showAppNames = showAppNames,
                            fontScale = searchResultsFontSize, // Apply font scaling
                            fontWeight = fontWeight, // Apply font weight
                            iconCornerRadius = settings.iconCornerRadius.dp, // Apply icon corner radius
                            onClick = {
                                if (appsToShow.size == 1 && searchQuery.isNotEmpty()) {
                                    onAppClick(appsToShow[0])
                                } else {
                                    onAppClick(app)
                                }
                            },
                            onLongClick = {
                                selectedApp = app
                                showContextMenu = true
                            },
                        )
                    }
                }

                if ((appsToShow.size == 1) and autoOpenFilteredApp) {
                    onAppClick(appsToShow[0])
                }
            }
        }
    }

    // App context menu
    if (showContextMenu && selectedApp != null) {
        val app = selectedApp!!
        val hiddenApps by viewModel.hiddenApps.collectAsState()
        val isHidden = hiddenApps.any { it.getKey() == app.getKey() }

        AlertDialog(
            onDismissRequest = {
                showContextMenu = false
                selectedApp = null
            },
            title = { Text(app.appLabel) },
            text = {
                Column {
                    // App actions
                    ContextMenuItem(
                        text = "Open App",
                        icon = Icons.Default.Info
                    ) {
                        onAppClick(app)
                        showContextMenu = false
                    }

                    ContextMenuItem(
                        text = if (isHidden) "Unhide App" else "Hide App",
                        icon = Icons.Default.Settings
                    ) {
                        viewModel.toggleAppHidden(app)
                        showContextMenu = false
                    }

                    ContextMenuItem(
                        text = "App Info",
                        icon = Icons.Default.Info
                    ) {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", app.appPackage, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        showContextMenu = false
                    }

                    ContextMenuItem(
                        text = "Add to Home Screen",
                        icon = Icons.Default.Add
                    ) {
                        coroutineScope.launch {
                            val homeAppsNum = settings.homeAppsNum
                            for (i in 0 until homeAppsNum) {
                                val homeApps = settingsViewModel.settingsRepository.getHomeApps()
                                val homeApp = homeApps[i]
                                if (homeApp.packageName.isEmpty()) {
                                    viewModel.selectedApp(app, Constants.FLAG_SET_HOME_APP_1 + i)
                                    break
                                }
                            }
                        }
                        showContextMenu = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContextMenu = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListItem(
    app: AppModel,
    showAppNames: Boolean,
    showAppIcon: Boolean,
    fontScale: Float = 1.0f,
    fontWeight: FontWeight = FontWeight.Normal,
    iconCornerRadius: Dp = 8.dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showAppIcon && app.appIcon != null) {
            Surface(
                shape = RoundedCornerShape(iconCornerRadius),
                modifier = Modifier.padding(end = 16.dp)
            ) {
                androidx.compose.foundation.Image(
                    bitmap = app.appIcon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        val textLabelShown = if (showAppNames) app.appLabel else ""

        Text(
            text = textLabelShown,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = MaterialTheme.typography.bodyLarge.fontSize * fontScale,
                fontWeight = fontWeight
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ContextMenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(end = 16.dp)
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun AppDrawerSearch(
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    onEnterPressed: () -> Unit = {},
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var isFocused by remember { mutableStateOf(false) }

    TextField(
        value = searchQuery,
        onValueChange = onSearchChanged,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .onFocusChanged {
                isFocused = it.isFocused
                if (isFocused) {
                    keyboardController?.show()
                }
            },
        placeholder = { Text("Search apps...") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                keyboardController?.hide()
                onEnterPressed()
            }
        ),
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}