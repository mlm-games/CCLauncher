package app.cclauncher.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cclauncher.MainViewModel
import app.cclauncher.data.ExternalWidgetModel
import app.cclauncher.data.WidgetConfig
import app.cclauncher.ui.components.ExternalWidget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "WidgetConfigScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigSizeScreen(
    viewModel: MainViewModel,
    providerClassName: String,
    label: String,
    packageName: String,
    widgetId: Int,
    existingWidget: ExternalWidgetModel? = null,
    onNavigateBack: () -> Unit,
    onSaveWidget: (ExternalWidgetModel) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Initialize state from existing widget if available
    var widthCells by remember { mutableStateOf(existingWidget?.width ?: 2) }
    var heightCells by remember { mutableStateOf(existingWidget?.height ?: 1) }
    var backgroundColor by remember { mutableLongStateOf(existingWidget?.config?.backgroundColor ?: 0x33000000) }
    var cornerRadius by remember { mutableStateOf(existingWidget?.config?.cornerRadius ?: 16f) }
    var padding by remember { mutableStateOf(existingWidget?.config?.padding ?: 8) }

    // Create a preview widget model
    val previewWidget = remember(widthCells, heightCells, backgroundColor, cornerRadius, padding) {
        existingWidget?.copy(
            width = widthCells,
            height = heightCells,
            config = WidgetConfig(
                backgroundColor = backgroundColor,
                cornerRadius = cornerRadius,
                padding = padding
            )
        ) ?: ExternalWidgetModel(
            id = UUID.randomUUID().toString(),
            appWidgetId = widgetId,
            providerClassName = providerClassName,
            packageName = packageName,
            label = label,
            width = widthCells,
            height = heightCells,
            config = WidgetConfig(
                backgroundColor = backgroundColor,
                cornerRadius = cornerRadius,
                padding = padding
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(existingWidget?.let { "Edit Widget" } ?: "Configure Widget") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    // Create the final widget model
                                    val finalWidget = existingWidget?.copy(
                                        width = widthCells,
                                        height = heightCells,
                                        config = WidgetConfig(
                                            backgroundColor = backgroundColor,
                                            cornerRadius = cornerRadius,
                                            padding = padding
                                        )
                                    ) ?: run {
                                        // For new widgets, get position from current widget count
                                        val position = viewModel.prefsDataStore.preferences.first().externalWidgets.size

                                        ExternalWidgetModel(
                                            id = UUID.randomUUID().toString(),
                                            appWidgetId = widgetId,
                                            providerClassName = providerClassName,
                                            packageName = packageName,
                                            label = label,
                                            width = widthCells,
                                            height = heightCells,
                                            position = position,
                                            config = WidgetConfig(
                                                backgroundColor = backgroundColor,
                                                cornerRadius = cornerRadius,
                                                padding = padding
                                            )
                                        )
                                    }

                                    Log.d(TAG, "Saving widget: id=${finalWidget.id}, size=${finalWidget.width}x${finalWidget.height}")
                                    onSaveWidget(finalWidget)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error saving widget: ${e.message}", e)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Widget preview
            Text(
                text = "Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Use our ExternalWidget component for preview
                ExternalWidget(
                    widget = previewWidget,
                    editMode = false
                )
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Size configuration
            Text(
                text = "Size",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp)
            )

            // Width slider
            Text(
                text = "Width: $widthCells cells",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 16.dp, bottom = 4.dp)
            )

            Slider(
                value = widthCells.toFloat(),
                onValueChange = { widthCells = it.toInt() },
                valueRange = 1f..4f,
                steps = 2,
                modifier = Modifier.fillMaxWidth()
            )

            // Height slider
            Text(
                text = "Height: $heightCells cells",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 16.dp, bottom = 4.dp)
            )

            Slider(
                value = heightCells.toFloat(),
                onValueChange = { heightCells = it.toInt() },
                valueRange = 1f..4f,
                steps = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Appearance configuration
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp)
            )

            // Background transparency slider
            Text(
                text = "Background Transparency",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 16.dp, bottom = 4.dp)
            )

            Slider(
                value = (backgroundColor shr 24 and 0xFF) / 255f,
                onValueChange = {
                    backgroundColor = ((it * 255).toLong() shl 24) or (backgroundColor and 0x00FFFFFF)
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Corner radius slider
            Text(
                text = "Corner Radius: ${cornerRadius.toInt()} dp",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 16.dp, bottom = 4.dp)
            )

            Slider(
                value = cornerRadius,
                onValueChange = { cornerRadius = it },
                valueRange = 0f..32f,
                modifier = Modifier.fillMaxWidth()
            )

            // Padding slider
            Text(
                text = "Padding: $padding dp",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 16.dp, bottom = 4.dp)
            )

            Slider(
                value = padding.toFloat(),
                onValueChange = { padding = it.toInt() },
                valueRange = 0f..16f,
                modifier = Modifier.fillMaxWidth()
            )

            // Save button for easy access at bottom of screen
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            // Create the final widget model (same as in TopAppBar action)
                            val finalWidget = existingWidget?.copy(
                                width = widthCells,
                                height = heightCells,
                                config = WidgetConfig(
                                    backgroundColor = backgroundColor,
                                    cornerRadius = cornerRadius,
                                    padding = padding
                                )
                            ) ?: run {
                                val position = viewModel.prefsDataStore.preferences.first().externalWidgets.size

                                ExternalWidgetModel(
                                    id = UUID.randomUUID().toString(),
                                    appWidgetId = widgetId,
                                    providerClassName = providerClassName,
                                    packageName = packageName,
                                    label = label,
                                    width = widthCells,
                                    height = heightCells,
                                    position = position,
                                    config = WidgetConfig(
                                        backgroundColor = backgroundColor,
                                        cornerRadius = cornerRadius,
                                        padding = padding
                                    )
                                )
                            }

                            Log.d(TAG, "Saving widget: id=${finalWidget.id}, size=${finalWidget.width}x${finalWidget.height}")
                            onSaveWidget(finalWidget)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving widget: ${e.message}", e)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Text("Save Widget")
            }
        }
    }
}