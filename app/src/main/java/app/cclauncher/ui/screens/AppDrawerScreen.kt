package app.cclauncher.ui.screens

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cclauncher.MainViewModel
import app.cclauncher.data.AppModel
import app.cclauncher.data.Constants
import app.cclauncher.helper.openSearch
import app.cclauncher.ui.BackHandler
import app.cclauncher.ui.util.detectSwipeGestures
import app.cclauncher.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.delay
// import kotlinx.coroutines.launch // Not directly used, but by LaunchedEffect
import kotlinx.coroutines.yield

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerScreen(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel = viewModel(),
    onAppClick: (AppModel) -> Unit,
    selectionMode: Boolean = false,
    selectionTitle: String = "",
    onSwipeDown: () -> Unit, // This is the primary action to go "home" or navigate back
) {
    BackHandler(onBack = onSwipeDown)

    val context = LocalContext.current
    val uiState by viewModel.appDrawerState.collectAsState()
    val settings by settingsViewModel.settingsState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var isSearchFocused by remember { mutableStateOf(false) }

    val autoShowKeyboard = settings.autoShowKeyboard
    val showAppNames = settings.showAppNames
    val showAppIcons = settings.showAppIcons
    val autoOpenFilteredApp = settings.autoOpenFilteredApp

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val shouldShowIcons = if (showAppIcons) {
        if (isLandscape) settings.showIconsInLandscape else settings.showIconsInPortrait
    } else { false }

    val itemSpacing = when (settings.itemSpacing) {
        0 -> 0.dp; 1 -> 4.dp; 2 -> 8.dp; 3 -> 16.dp; else -> 4.dp
    }

    val searchResultsFontSize = if (settings.searchResultsUseHomeFont) {
        settings.textSizeScale
    } else { settings.searchResultsFontSize }

    val fontWeight = when (settings.fontWeight) {
        0 -> FontWeight.Thin; 1 -> FontWeight.Light; 2 -> FontWeight.Normal
        3 -> FontWeight.Medium; 4 -> FontWeight.Bold; 5 -> FontWeight.Black
        else -> FontWeight.Normal
    }

    var selectedApp by remember { mutableStateOf<AppModel?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }

    LaunchedEffect(settings.forceLandscapeMode, context) {
        (context as? Activity)?.let { activity ->
            if (settings.forceLandscapeMode) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }
    }

    LaunchedEffect(Unit) { viewModel.loadApps() }
    LaunchedEffect(searchQuery) { viewModel.searchApps(searchQuery) }

    LaunchedEffect(autoShowKeyboard, focusRequester) {
        if (autoShowKeyboard) {
            yield()
            focusRequester.requestFocus()
        }
    }

    val scrollState = rememberLazyListState()

    LaunchedEffect(searchQuery, scrollState) {
        // Scroll to top when search query is cleared, if not already at top
        if (searchQuery.isEmpty() && (scrollState.firstVisibleItemIndex != 0 || scrollState.firstVisibleItemScrollOffset != 0) ) {
            scrollState.scrollToItem(0)
        }
    }

    // Keyboard and scroll interaction logic
    LaunchedEffect(scrollState, keyboardController, focusManager, focusRequester, isSearchFocused) {
        var previousIndex = scrollState.firstVisibleItemIndex
        var previousOffset = scrollState.firstVisibleItemScrollOffset

        snapshotFlow {
            Triple(
                scrollState.firstVisibleItemIndex,
                scrollState.firstVisibleItemScrollOffset,
                scrollState.isScrollInProgress
            )
        }.collect { (currentIndex, currentOffset, isScrolling) ->
            if (isScrolling) {
                val actualScrollHappened = currentIndex != previousIndex || currentOffset != previousOffset
                if (actualScrollHappened) {
                    // Determine scroll direction: positive for down, negative for up
                    var verticalScrollDelta = 0
                    verticalScrollDelta = if (currentIndex > previousIndex) 1 // Major scroll down
                    else if (currentIndex < previousIndex) -1 // Major scroll up
                    else currentOffset - previousOffset // Minor scroll in same item

                    if (verticalScrollDelta > 0) { // User scrolled DOWN (content moved UP)
                        if (isSearchFocused) {
                            focusManager.clearFocus() // Will trigger onFocusStateChanged(false)
                        }
                        keyboardController?.hide()
                    } else if (verticalScrollDelta < 0) { // User scrolled up
                        if (currentIndex == 0 && currentOffset == 0) { // Reached the very top of the list
                            if (!isSearchFocused) {
                                focusRequester.requestFocus() // Will trigger onFocusStateChanged(true) & show keyboard
                            }
                        }
                    }
                }
            }
            previousIndex = currentIndex
            previousOffset = currentOffset
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .detectSwipeGestures(
                onSwipeDown = { // General swipe down (anywhere) to trigger onSwipeDown action (e.g., go home)
                    onSwipeDown()
                },
                onSwipeUp = { // Swipe up when at the very top of the list to trigger onSwipeDown action
                    if (scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset == 0) {
                        onSwipeDown()
                    }
                    // If not at the top, LazyColumn handles the swipe for its own scrolling.
                }
            )
            .statusBarsPadding()
    ) {
        if (selectionMode) {
            TopAppBar(
                title = { Text(selectionTitle) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }

        AppDrawerSearch(
            searchQuery = searchQuery,
            onSearchChanged = { query -> searchQuery = query },
            modifier = Modifier.focusRequester(focusRequester),
            onEnterPressed = {
                val appsToOpen = if (searchQuery.isEmpty()) uiState.apps else uiState.filteredApps
                if (appsToOpen.isNotEmpty()) onAppClick(appsToOpen[0])
                // Keyboard is hidden by AppDrawerSearch's onSearch action
            },
            onFocusStateChanged = { focused ->
                isSearchFocused = focused
                // Keyboard visibility is handled by onFocusChanged in AppDrawerSearch for focus gain,
                // and by scroll logic or IME actions for focus loss/hide.
            }
        )

        val appsToShow = if (searchQuery.isEmpty()) uiState.apps else uiState.filteredApps

        Log.d("AppRename", "Renamed apps: ${settings.renamedApps}")

        LaunchedEffect(appsToShow, autoOpenFilteredApp, searchQuery, onAppClick) {
            delay(300)
            if (searchQuery.isNotEmpty() && appsToShow.size == 1 && autoOpenFilteredApp) {
                onAppClick(appsToShow[0])
            }
        }

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            uiState.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Error: ${uiState.error}") }
            uiState.apps.isEmpty() && searchQuery.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No apps found") }
            uiState.filteredApps.isEmpty() && searchQuery.isNotEmpty() -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
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
                        ) { Text("Search Web") }
                    }
                }
            }
            else -> {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(itemSpacing)
                ) {
                    items(
                        items = appsToShow,
                        key = { app -> "${app.appPackage}/${app.activityClassName ?: ""}/${app.user.hashCode()}" }
                    ) { app ->
                        AppListItem(
                            app = app, showAppIcon = shouldShowIcons, showAppNames = showAppNames,
                            fontScale = searchResultsFontSize, fontWeight = fontWeight,
                            iconCornerRadius = settings.iconCornerRadius.dp,
                            onClick = { onAppClick(app) },
                            onLongClick = { selectedApp = app; showContextMenu = true },
                            modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = tween(durationMillis = 300))
                        )
                    }
                }
            }
        }
    }

    if (showContextMenu && selectedApp != null) {
        val app = selectedApp!!
        val hiddenApps by viewModel.hiddenApps.collectAsState()
        val isHidden = hiddenApps.any { it.getKey() == app.getKey() }

        var renameDialogVisible by remember { mutableStateOf(false) }
        var newAppName by remember { mutableStateOf(app.appLabel) }

        AlertDialog(
            onDismissRequest = { showContextMenu = false; selectedApp = null },
            title = { Text(app.appLabel) },
            text = {
                Column {
                    ContextMenuItem("Open App", Icons.Default.AdsClick) { onAppClick(app); showContextMenu = false; selectedApp = null }
                    ContextMenuItem(if (isHidden) "Unhide App" else "Hide App", Icons.Default.Settings) { viewModel.toggleAppHidden(app); showContextMenu = false; selectedApp = null }
                    ContextMenuItem("Rename App", Icons.Default.DriveFileRenameOutline) { renameDialogVisible = true }
//                    ContextMenuItem("Change Icon", Icons.Default.ChangeCircle) { } // Causes too many performance related problems, and even saving related for images.
                    ContextMenuItem("App Info", Icons.Default.Info) {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", app.appPackage, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                        showContextMenu = false; selectedApp = null
                    }
                    ContextMenuItem("Add to Home Screen", Icons.Default.Add) { viewModel.addAppToHomeScreen(app); showContextMenu = false; selectedApp = null }
                }
            },
            confirmButton = { TextButton({ showContextMenu = false; selectedApp = null }) { Text("Close") } }
        )

        if (renameDialogVisible) {
            AlertDialog(
                onDismissRequest = { renameDialogVisible = false },
                title = { Text("Rename ${app.appLabel}") },
                text = {
                    TextField(
                        value = newAppName,
                        onValueChange = { newAppName = it },
                        label = { Text("New name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.renameApp(app, newAppName)
                        renameDialogVisible = false
                        showContextMenu = false
                        selectedApp = null
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { renameDialogVisible = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListItem(
    app: AppModel,
    showAppNames: Boolean,
    showAppIcon: Boolean,
    fontScale: Float,
    fontWeight: FontWeight,
    iconCornerRadius: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 300))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
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
                    contentDescription = app.appLabel,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Text(
            text = if (showAppNames) app.appLabel else "",
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
private fun ContextMenuItem(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, text, Modifier.padding(end = 16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun AppDrawerSearch(
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    onEnterPressed: () -> Unit = {},
    onFocusStateChanged: (Boolean) -> Unit // Callback to notify parent of focus state
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    TextField(
        value = searchQuery,
        onValueChange = onSearchChanged,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .onFocusChanged { focusState ->
                val focused = focusState.isFocused
                onFocusStateChanged(focused) // Notify parent of focus change
                if (focused) {
                    keyboardController?.show() // Show keyboard when TextField gains focus
                }
                // Keyboard hiding on focus loss is handled by system, IME actions, or explicit calls elsewhere (e.g., scroll logic)
            },
        placeholder = { Text("Search apps...") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            keyboardController?.hide() // Hide keyboard on IME "Search" action
            onEnterPressed()
        }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        )
    )
}