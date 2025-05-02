package app.cclauncher.ui.screens

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import app.cclauncher.MainActivity
import app.cclauncher.MainViewModel
import app.cclauncher.helper.WidgetHelper

// Create WidgetPickerScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val widgetHelper = remember { WidgetHelper(context) }
    val availableWidgets = remember { mutableStateOf<List<AppWidgetProviderInfo>>(emptyList()) }

    // Load available widgets
    LaunchedEffect(Unit) {
        availableWidgets.value = widgetHelper.getAvailableWidgets()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top bar
        TopAppBar(
            title = { Text("Add Widget") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )

        // Widget list
        if (availableWidgets.value.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(availableWidgets.value.size) { index ->
                    val widgetInfo = availableWidgets.value[index]
                    WidgetInfoItem(
                        widgetInfo = widgetInfo,
                        onClick = {
                            // Request to add this widget
                            (context as? MainActivity)?.addExternalWidget(widgetInfo)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WidgetInfoItem(
    widgetInfo: AppWidgetProviderInfo,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val label = remember { widgetInfo.loadLabel(context.packageManager) }
    val icon = remember {
        widgetInfo.loadIcon(context, context.resources.displayMetrics.densityDpi)?.toBitmap()?.asImageBitmap()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Widget icon
        icon?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .padding(end = 16.dp)
            )
        } ?: Box(
            modifier = Modifier
                .size(48.dp)
                .padding(end = 16.dp)
                .background(Color.Gray.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null
            )
        }

        // Widget info
        Column {
            Text(
                text = label.toString(),
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = widgetInfo.provider.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}