package app.cclauncher.ui.screens

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cclauncher.MainViewModel
import app.cclauncher.data.ExternalWidgetModel
import app.cclauncher.data.WidgetConfig
import java.util.*

@Composable
fun WidgetConfigSizeScreen(
    viewModel: MainViewModel,
    widgetInfo: AppWidgetProviderInfo,
    widgetId: Int,
    onNavigateBack: () -> Unit,
    onSaveWidget: (ExternalWidgetModel) -> Unit
) {
    var widthCells by remember { mutableIntStateOf(2) }
    var heightCells by remember { mutableIntStateOf(1) }
    var backgroundColor by remember { mutableIntStateOf(0x33000000) }
    var cornerRadius by remember { mutableFloatStateOf(16f) }
    var padding by remember { mutableIntStateOf(8) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Text(
                text = "Configure Widget",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    // Create widget model and save
                    val widget = ExternalWidgetModel(
                        id = UUID.randomUUID().toString(),
                        appWidgetId = widgetId,
                        providerClassName = widgetInfo.provider.className,
                        packageName = widgetInfo.provider.packageName,
                        label = widgetInfo.loadLabel(viewModel.appContext.packageManager),
                        width = widthCells,
                        height = heightCells,
                        config = WidgetConfig(
                            backgroundColor = backgroundColor.toLong(),
                            cornerRadius = cornerRadius,
                            padding = padding
                        )
                    )
                    onSaveWidget(widget)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Widget size preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(widthCells.toFloat() / heightCells.toFloat())
                .background(Color(backgroundColor), RoundedCornerShape(cornerRadius.dp))
                .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(cornerRadius.dp))
                .padding(padding.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${widthCells}x${heightCells}",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Width slider
        Text(
            text = "Width: $widthCells cells",
            style = MaterialTheme.typography.headlineMedium
        )
        Slider(
            value = widthCells.toFloat(),
            onValueChange = { widthCells = it.toInt() },
            valueRange = 1f..4f,
            steps = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Height slider
        Text(
            text = "Height: $heightCells cells",
            style = MaterialTheme.typography.headlineMedium
        )
        Slider(
            value = heightCells.toFloat(),
            onValueChange = { heightCells = it.toInt() },
            valueRange = 1f..4f,
            steps = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Background transparency slider
        Text(
            text = "Background Transparency",
            style = MaterialTheme.typography. headlineMedium
        )
        Slider(
            value = (backgroundColor shr 24 and 0xFF) / 255f,
            onValueChange = {
                backgroundColor = ((it * 255).toInt() shl 24) or (backgroundColor and 0x00FFFFFF)
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Corner radius slider
        Text(
            text = "Corner Radius: ${cornerRadius.toInt()} dp",
            style = MaterialTheme.typography. headlineMedium
        )
        Slider(
            value = cornerRadius,
            onValueChange = { cornerRadius = it },
            valueRange = 0f..32f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Padding slider
        Text(
            text = "Padding: $padding dp",
            style = MaterialTheme.typography. headlineMedium
        )
        Slider(
            value = padding.toFloat(),
            onValueChange = { padding = it.toInt() },
            valueRange = 0f..16f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}