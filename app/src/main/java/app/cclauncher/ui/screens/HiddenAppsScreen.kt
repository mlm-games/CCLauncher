package app.cclauncher.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cclauncher.MainViewModel
import app.cclauncher.ui.components.AppListItem
import app.cclauncher.ui.theme.AnimationConfig

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HiddenAppsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val hiddenApps by viewModel.hiddenApps.collectAsState()
    val isLoading by remember { mutableStateOf(false) }
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Load hidden apps when screen is shown
    LaunchedEffect(Unit) {
        viewModel.getHiddenApps()
    }

    // Handle errors
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            // Show error toast or message
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hidden Apps") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .animateContentSize(
                    animationSpec = AnimationConfig.standardIntTween
                )
        ) {
            if (isLoading) {
                // Show loading indicator
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (hiddenApps.isEmpty()) {
                // Show empty state with animation
                AnimatedVisibility(
                    visible = true,
                    enter = AnimationConfig.enterTransition(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No hidden apps",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Long-press on any app in the app drawer to hide it",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = hiddenApps,
                        key = { app -> "${app.appPackage}/${app.activityClassName ?: ""}/${app.user.hashCode()}" }
                    ) { app ->
                        AppListItem(
                            appLabel = app.appLabel,
                            appIcon = app.appIcon,
                            showIcon = app.appIcon != null,
                            onClick = { viewModel.launchApp(app) },
                            onLongClick = {
                                // Unhide app and refresh list
                                viewModel.toggleAppHidden(app)
                            },
                            modifier = Modifier.animateItem(
                                fadeInSpec = null,
                                fadeOutSpec = null,
                                placementSpec = AnimationConfig.listItemAnimationSpec
                            )
                        )
                    }
                }
            }
        }
    }
}
