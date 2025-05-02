package app.cclauncher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cclauncher.MainViewModel
import app.cclauncher.data.ExternalWidgetModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(
    viewModel: MainViewModel,
    widget: ExternalWidgetModel,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure Widget") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                .padding(16.dp)
        ) {
            // Widget size configuration
            var width by remember { mutableIntStateOf(widget.width) }
            var height by remember { mutableIntStateOf(widget.height) }

            Text("Width: $width", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = width.toFloat(),
                onValueChange = { width = it.toInt() },
                valueRange = 1f..4f,
                steps = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Height: $height", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = height.toFloat(),
                onValueChange = { height = it.toInt() },
                valueRange = 1f..4f,
                steps = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val updatedWidget = widget.copy(
                        width = width,
                        height = height
                    )
                    viewModel.updateExternalWidget(updatedWidget)
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Changes")
            }
        }
    }
}