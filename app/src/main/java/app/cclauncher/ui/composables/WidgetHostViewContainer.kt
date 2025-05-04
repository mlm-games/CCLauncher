package app.cclauncher.ui.composables

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun WidgetHostViewContainer(
    modifier: Modifier = Modifier,
    appWidgetId: Int,
    providerInfo: AppWidgetProviderInfo,
    appWidgetHost: AppWidgetHost,
    widgetSizeData: WidgetSizeData, // Pass size info
    onLongPress: () -> Unit = {},
) {
    val context = LocalContext.current
    val widgetManager = AppWidgetManager.getInstance(context)
    var errorLoading by remember { mutableStateOf(false) }

    if (errorLoading) {
        // Optionally display an error placeholder
        Box(modifier = modifier) { /* Error state */ }
        return
    }

    // Calculate size in Dp based on cell spans (you might need a more robust calculation)
    val widthDp = with(LocalDensity.current) { widgetSizeData.width.toDp() }
    val heightDp = with(LocalDensity.current) { widgetSizeData.height.toDp() }

    val widgetView = remember(appWidgetId) {
        try {
            appWidgetHost.createView(context, appWidgetId, providerInfo)?.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // Apply size options
                val options = Bundle().apply {
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widgetSizeData.minWidthDp.value.toInt())
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widgetSizeData.maxWidthDp.value.toInt())
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, widgetSizeData.minHeightDp.value.toInt())
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, widgetSizeData.maxHeightDp.value.toInt())
                }
                widgetManager.updateAppWidgetOptions(appWidgetId, options)
            }
        } catch (e: Exception) {
            Log.e("WidgetHost", "Error creating widget view for ID $appWidgetId", e)
            errorLoading = true
            null
        }
    }

    if (widgetView != null) {
        AndroidView(
            factory = { widgetView },
            modifier = modifier
                .fillMaxSize() // Fill the constrained space
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { onLongPress() })
                }
        )
    } else {
        // Handle case where view creation failed but didn't immediately throw
        Box(modifier = modifier) { /* Error placeholder or empty */ }
    }

    DisposableEffect(appWidgetId) {
        onDispose {
            // Optionally cleanup view, though AppWidgetHost usually handles it
        }
    }
}

// Helper class to bundle widget size info
data class WidgetSizeData(
    val width: Int, // Size in pixels for layout
    val height: Int,
    val minWidthDp: Dp, // Size in Dp for widget options
    val maxWidthDp: Dp,
    val minHeightDp: Dp,
    val maxHeightDp: Dp
)