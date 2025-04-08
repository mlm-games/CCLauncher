package app.cclauncher.helper

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import app.cclauncher.data.ExternalWidgetModel

/**
 * Helper class to manage external widgets
 */
class WidgetHelper(private val context: Context) {
    companion object {
        private const val HOST_ID = 1024
        private const val REQUEST_CREATE_APPWIDGET = 5
        private const val REQUEST_CONFIGURE_APPWIDGET = 6
    }

    private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    private val appWidgetHost: AppWidgetHost = AppWidgetHost(context, HOST_ID)

    init {
        // Start listening for widget updates
        appWidgetHost.startListening()
    }

    /**
     * Get a list of all available widgets from installed apps
     */
    fun getAvailableWidgets(): List<AppWidgetProviderInfo> {
        return appWidgetManager.installedProviders
    }

    /**
     * Allocate a widget ID for a new widget
     */
    fun allocateAppWidgetId(): Int {
        return appWidgetHost.allocateAppWidgetId()
    }

    /**
     * Create a widget binding
     */
    fun bindAppWidgetIdIfAllowed(appWidgetId: Int, providerInfo: AppWidgetProviderInfo): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, providerInfo.provider)
        } else {
            appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, providerInfo.provider, null)
        }
    }

    /**
     * Create intent to request permission to bind widget
     */
    fun createBindWidgetIntent(appWidgetId: Int, providerInfo: AppWidgetProviderInfo): Intent {
        // For Android O and above, we need to use a different approach
        return Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, providerInfo.provider)
        }
    }

    /**
     * Check if a widget requires configuration
     */
    fun needsConfiguration(widgetId: Int): Boolean {
        val providerInfo = appWidgetManager.getAppWidgetInfo(widgetId)
        return providerInfo?.configure != null
    }

    /**
     * Create configuration intent for a widget
     */
    fun createConfigurationIntent(widgetId: Int): Intent? {
        val providerInfo = appWidgetManager.getAppWidgetInfo(widgetId) ?: return null
        if (providerInfo.configure == null) return null

        return Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
            component = providerInfo.configure
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Create a widget view
     */
    fun createWidget(widgetId: Int): android.view.View {
        return appWidgetHost.createView(context, widgetId, appWidgetManager.getAppWidgetInfo(widgetId))
    }

    /**
     * Update widget options
     */
    fun updateWidgetOptions(widgetId: Int, width: Int, height: Int) {
        val options = Bundle()
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, width)
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, width)
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height)
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, height)
        appWidgetManager.updateAppWidgetOptions(widgetId, options)
    }

    /**
     * Delete a widget
     */
    fun deleteWidget(widgetId: Int) {
        appWidgetHost.deleteAppWidgetId(widgetId)
    }

    /**
     * Stop listening for widget updates
     */
    fun stopListening() {
        appWidgetHost.stopListening()
    }

    fun getWidgetInfo(widgetId: Int): AppWidgetProviderInfo? {
        return try {
            appWidgetManager.getAppWidgetInfo(widgetId)
        } catch (e: Exception) {
            null
        }
    }
}