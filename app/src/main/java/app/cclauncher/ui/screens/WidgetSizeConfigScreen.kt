package app.cclauncher.ui.screens

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.cclauncher.MainViewModel
import app.cclauncher.data.ExternalWidgetModel
import app.cclauncher.helper.WidgetHelper
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSizeConfigScreen(
    viewModel: MainViewModel,
    appWidgetId: Int,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val widgetHelper = remember { WidgetHelper(context) }
    val widgetInfo = remember { mutableStateOf<AppWidgetProviderInfo?>(null) }

    // Widget dimensions
    var widthMultiplier by remember { mutableStateOf(2) }
    var heightMultiplier by remember { mutableStateOf(1) }

    // Load widget info
    LaunchedEffect(appWidgetId) {
        widgetInfo.value = widgetHelper.getWidgetInfo(appWidgetId)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top bar
        TopAppBar(
            title = { Text("Configure Widget Size") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )

        // Widget preview
        widgetInfo.value?.let { info ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Widget label
                Text(
                    text = info.loadLabel(context.packageManager).toString(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Size preview
                val baseUnit = 80.dp
                val previewWidth = baseUnit * widthMultiplier
                val previewHeight = baseUnit * heightMultiplier

                Box(
                    modifier = Modifier
                        .width(previewWidth)
                        .height(previewHeight)
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${widthMultiplier}x${heightMultiplier}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Width slider
                Text(
                    text = "Width: $widthMultiplier",
                    style = MaterialTheme.typography.bodyMedium
                )

                Slider(
                    value = widthMultiplier.toFloat(),
                    onValueChange = { widthMultiplier = it.toInt() },
                    valueRange = 1f..4f,
                    steps = 2,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Height slider
                Text(
                    text = "Height: $heightMultiplier",
                    style = MaterialTheme.typography.bodyMedium
                )

                Slider(
                    value = heightMultiplier.toFloat(),
                    onValueChange = { heightMultiplier = it.toInt() },
                    valueRange = 1f..4f,
                    steps = 2,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Add button
                Button(
                    onClick = {
                        // Create a new widget model and add it
                        val newWidget = ExternalWidgetModel(
                            id = UUID.randomUUID().toString(),
                            appWidgetId = appWidgetId,
                            providerClassName = info.provider.className,
                            packageName = info.provider.packageName,
                            label = info.loadLabel(context.packageManager).toString(),
                            width = widthMultiplier,
                            height = heightMultiplier,
                            position = Int.MAX_VALUE // Will be placed at the end
                        )

                        viewModel.addExternalWidget(newWidget)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Widget")
                }
            }
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Widget not found")
        }
    }
}