package app.cclauncher.ui.compose.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cclauncher.MainViewModel
import app.cclauncher.ui.compose.components.AppItem

@OptIn(ExperimentalMaterial3Api::class)
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
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                // Show loading indicator
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (hiddenApps.isEmpty()) {
                // Show empty state
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
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
            } else {
                // Show list of hidden apps
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = hiddenApps,
                        key = { app -> "${app.appPackage}/${app.activityClassName ?: ""}/${app.user.hashCode()}" }
                    ) { app ->
                        AppItem(
                            app = app,
                            onClick = {
                                viewModel.launchApp(app)
                            },
                            onLongClick = {
                                // Unhide app and refresh list
                                viewModel.toggleAppHidden(app)
                            }
                        )
                    }
                }
            }
        }
    }
}