package app.cclauncher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.cclauncher.MainViewModel
import app.cclauncher.helper.WidgetHelper

@Composable
fun WidgetContainer(
    viewModel: MainViewModel,
    onAddWidget: () -> Unit
) {
    val context = LocalContext.current
    val widgetHelper = remember { WidgetHelper(context) }

    // Get widgets from preferences
    val preferences by viewModel.prefsDataStore.preferences.collectAsState(initial = null)
    val widgets = preferences?.externalWidgets ?: emptyList()

    // Edit mode state
    var editMode by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(widgets) { widget ->
                ExternalWidget(
                    widget = widget,
                    editMode = editMode,
                    onConfigureWidget = {
                        // Open configuration for existing widget
                        viewModel.configureExistingWidget(it)
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

            // Add extra space at bottom for FAB
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        // FAB for adding new widgets
        FloatingActionButton(
            onClick = onAddWidget,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Widget"
            )
        }
    }
}