package app.cclauncher.ui.screens

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import app.cclauncher.helper.BitmapUtils.drawableToBitmap

data class WidgetListItem(
    val appName: String,
    val appPackage: String,
    val widgets: List<AppWidgetProviderInfo>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerScreen(
    onWidgetSelected: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val widgetManager = AppWidgetManager.getInstance(context)
    var widgetList by remember { mutableStateOf<List<WidgetListItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        widgetList = loadInstalledWidgets(context, widgetManager)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Widget") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (widgetList.isEmpty()) {
                Text("No widgets found.", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    widgetList.forEach { group ->
                        item {
                            Text(
                                text = group.appName,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                            )
                        }
                        items(group.widgets, key = { it.provider.flattenToString() }) { widgetInfo ->
                            WidgetInfoItem(
                                context = context,
                                widgetInfo = widgetInfo,
                                onClick = { onWidgetSelected(widgetInfo) }
                            )
                        }
                        item { HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = Dp.Hairline,
                            color = Color.Transparent
                        ) }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetInfoItem(
    context: Context,
    widgetInfo: AppWidgetProviderInfo,
    onClick: () -> Unit
) {
    val pm = context.packageManager
    var previewImage by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(widgetInfo) {
        previewImage = loadWidgetPreview(context, widgetInfo)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Show preview or app icon
        val imageBitmap = previewImage?.asImageBitmap()
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Widget preview",
                modifier = Modifier
                    .size(64.dp) // Adjust size as needed
                    .padding(end = 16.dp)
            )
        } else {
            // Fallback: Show App Icon - requires IconCache modification or direct load
            // Placeholder:
            Spacer(modifier = Modifier.size(64.dp).padding(end = 16.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = widgetInfo.loadLabel(pm),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
            Text(
                text = "${widgetInfo.minWidth}x${widgetInfo.minHeight}dp", // Show min size info
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Helper to load widgets and group them
private suspend fun loadInstalledWidgets(context: Context, widgetManager: AppWidgetManager): List<WidgetListItem> {
    return withContext(Dispatchers.IO) {
        val pm = context.packageManager
        widgetManager.installedProviders
            .groupBy { it.provider.packageName }
            .mapNotNull { (packageName, widgets) ->
                try {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    WidgetListItem(appName, packageName, widgets.sortedBy { it.loadLabel(pm) })
                } catch (_: Exception) {
                    null // Skip if app info not found
                }
            }
            .sortedBy { it.appName }
    }
}

// Helper to load widget preview (can be slow)
private suspend fun loadWidgetPreview(
    context: Context,
    widgetInfo: AppWidgetProviderInfo
): android.graphics.Bitmap? {
    return withContext(Dispatchers.IO) {
        widgetInfo.loadPreviewImage(context, 0)?.let { drawable ->
            drawableToBitmap(drawable, defaultSize = 100)
        }
    }
}