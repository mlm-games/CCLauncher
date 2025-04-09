package app.cclauncher.ui.screens

import android.appwidget.AppWidgetProviderInfo
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cclauncher.MainViewModel
import app.cclauncher.helper.WidgetHelper

@Composable
fun ExternalWidgetPickerScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onWidgetSelected: (AppWidgetProviderInfo) -> Unit
) {
    val context = LocalContext.current
    val widgetHelper = remember { WidgetHelper(context) }

    // Get all available widgets
    val availableWidgets by remember {
        mutableStateOf(widgetHelper.getAvailableWidgets().sortedBy { it.loadLabel(context.packageManager) })
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredWidgets = remember(searchQuery, availableWidgets) {
        if (searchQuery.isEmpty()) {
            availableWidgets
        } else {
            availableWidgets.filter {
                it.loadLabel(context.packageManager).contains(searchQuery, ignoreCase = true)
            }
        }
    }

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
                text = "Add Widget",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            placeholder = { Text("Search widgets") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Widgets grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredWidgets) { widgetInfo ->
                ExternalWidgetItem(
                    widgetInfo = widgetInfo,
                    onClick = { onWidgetSelected(widgetInfo) }
                )
            }
        }
    }
}

@Composable
fun ExternalWidgetItem(
    widgetInfo: AppWidgetProviderInfo,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val label = widgetInfo.loadLabel(context.packageManager)

    // Get preview image if available
    val previewImage = remember {
        try {
            val drawable = widgetInfo.loadPreviewImage(context, context.resources.displayMetrics.densityDpi)
                ?: widgetInfo.loadIcon(context, context.resources.displayMetrics.densityDpi)

            (drawable as? BitmapDrawable)?.bitmap?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.elevatedCardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            previewImage?.let {
                Image(
                    bitmap = it,
                    contentDescription = label,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )
        }
    }
}