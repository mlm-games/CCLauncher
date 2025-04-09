package app.cclauncher.helper

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import app.cclauncher.data.ExternalWidgetModel

/**
 * Helper class to manage external widgets
 */
class WidgetHelper(private val context: Context) {
    companion object {
        private const val HOST_ID = 1024
        private const val TAG = "WidgetHelper"
    }

    private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    private val appWidgetHost: AppWidgetHost = AppWidgetHost(context, HOST_ID)

    init {
        // Start listening for widget updates
        try {
            appWidgetHost.startListening()
            Log.d(TAG, "Widget host started listening")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting widget host: ${e.message}")
        }
    }

    /**
     * Get a list of all available widgets from installed apps
     */
    fun getAvailableWidgets(): List<AppWidgetProviderInfo> {
        return try {
            appWidgetManager.installedProviders
        } catch (e: Exception) {
            Log.e(TAG, "Error getting available widgets: ${e.message}")
            emptyList()
        }
    }

    /**
     * Allocate a widget ID for a new widget
     */
    fun allocateAppWidgetId(): Int {
        return try {
            val id = appWidgetHost.allocateAppWidgetId()
            Log.d(TAG, "Allocated widget ID: $id")
            id
        } catch (e: Exception) {
            Log.e(TAG, "Error allocating widget ID: ${e.message}")
            -1
        }
    }

    /**
     * Create a widget binding
     */
    fun bindAppWidgetIdIfAllowed(appWidgetId: Int, providerInfo: AppWidgetProviderInfo): Boolean {
        return try {
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, providerInfo.provider)
            } else {
                appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, providerInfo.provider, null)
            }
            Log.d(TAG, "Widget binding result: $result for ID: $appWidgetId")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error binding widget: ${e.message}")
            false
        }
    }

    /**
     * Create intent to request permission to bind widget
     */
    fun createBindWidgetIntent(appWidgetId: Int, providerInfo: AppWidgetProviderInfo): Intent {
        Log.d(TAG, "Creating bind widget intent for ID: $appWidgetId")
        return Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, providerInfo.provider)
            // Add the flag to ensure we get a result back
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Check if a widget requires configuration
     */
    fun needsConfiguration(widgetId: Int): Boolean {
        return try {
            val providerInfo = appWidgetManager.getAppWidgetInfo(widgetId)
            val needsConfig = providerInfo?.configure != null
            Log.d(TAG, "Widget ID: $widgetId needs configuration: $needsConfig")
            needsConfig
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if widget needs configuration: ${e.message}")
            false
        }
    }

    /**
     * Create configuration intent for a widget
     */
    fun createConfigurationIntent(widgetId: Int): Intent? {
        return try {
            val providerInfo = appWidgetManager.getAppWidgetInfo(widgetId) ?: return null
            if (providerInfo.configure == null) return null

            Log.d(TAG, "Creating configuration intent for widget ID: $widgetId")
            Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = providerInfo.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating configuration intent: ${e.message}")
            null
        }
    }

    /**
     * Create a widget view
     */
    fun createWidget(widgetId: Int): android.view.View {
        return try {
            Log.d(TAG, "Creating widget view for ID: $widgetId")
            val info = appWidgetManager.getAppWidgetInfo(widgetId)
            val view = appWidgetHost.createView(context, widgetId, info)
            Log.d(TAG, "Widget view created successfully")
            view
        } catch (e: Exception) {
            Log.e(TAG, "Error creating widget view: ${e.message}")
            // Create a fallback view
            android.view.View(context).apply {
                setBackgroundColor(android.graphics.Color.RED)
            }
        }
    }

    /**
     * Update widget options
     */
    fun updateWidgetOptions(widgetId: Int, width: Int, height: Int) {
        try {
            Log.d(TAG, "Updating widget options for ID: $widgetId, width: $width, height: $height")
            val options = Bundle()
            options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, width)
            options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, width)
            options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height)
            options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, height)
            appWidgetManager.updateAppWidgetOptions(widgetId, options)
            Log.d(TAG, "Widget options updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widget options: ${e.message}")
        }
    }

    /**
     * Delete a widget
     */
    fun deleteWidget(widgetId: Int) {
        try {
            Log.d(TAG, "Deleting widget with ID: $widgetId")
            appWidgetHost.deleteAppWidgetId(widgetId)
            Log.d(TAG, "Widget deleted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting widget: ${e.message}")
        }
    }

    /**
     * Stop listening for widget updates
     */
    fun stopListening() {
        try {
            Log.d(TAG, "Stopping widget host listening")
            appWidgetHost.stopListening()
            Log.d(TAG, "Widget host stopped listening")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping widget host: ${e.message}")
        }
    }

    /**
     * Get widget info
     */
    fun getWidgetInfo(widgetId: Int): AppWidgetProviderInfo? {
        return try {
            val info = appWidgetManager.getAppWidgetInfo(widgetId)
            Log.d(TAG, "Got widget info for ID: $widgetId, provider: ${info?.provider}")
            info
        } catch (e: Exception) {
            Log.e(TAG, "Error getting widget info: ${e.message}")
            null
        }
    }
}