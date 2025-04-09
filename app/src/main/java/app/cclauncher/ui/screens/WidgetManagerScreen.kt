package app.cclauncher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.cclauncher.MainViewModel
import app.cclauncher.helper.WidgetHelper
import app.cclauncher.ui.UiEvent
import app.cclauncher.ui.components.ExternalWidget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetManagerScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onAddWidget: () -> Unit
) {
    val context = LocalContext.current
    val widgetHelper = remember { WidgetHelper(context) }

    // Get widgets from preferences
    val preferences by viewModel.prefsDataStore.preferences.collectAsState(initial = null)
    val widgets = preferences?.externalWidgets ?: emptyList()

    // Edit mode state
    var editMode by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Widgets") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Toggle edit mode
                    IconButton(onClick = { editMode = !editMode }) {
                        Icon(
                            imageVector = if (editMode) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = if (editMode) "Done Editing" else "Edit Widgets"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddWidget) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Widget"
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (widgets.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No widgets added. Tap + to add a widget.")
                    }
                }
            } else {
                items(widgets) { widget ->
                    ExternalWidget(
                        widget = widget,
                        editMode = editMode,
                        onConfigureWidget = {
                            viewModel.emitEvent(UiEvent.NavigateToWidgetConfig(it))
                        },
                        onRemoveWidget = { widgetId ->
                            // Find the widget by ID
                            widgets.find { it.id == widgetId }?.let { widgetToRemove ->
                                // Delete the widget from AppWidgetHost
                                widgetHelper.deleteWidget(widgetToRemove.appWidgetId)
                                // Remove from preferences
                                viewModel.removeExternalWidget(widgetId)
                            }
                        }
                    )
                }
            }

            // Add extra space at bottom for FAB
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}