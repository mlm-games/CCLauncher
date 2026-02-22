package app.cclauncher.helper

import android.app.Activity
import android.app.ActivityOptions
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import android.util.Log

/**
 * Helper class to manage external widgets
 */
class WidgetHelper(private val context: Context, private val appWidgetManager: AppWidgetManager, private val appWidgetHost: AppWidgetHost) {
    companion object {
        private const val TAG = "WidgetHelper"
    }

    /**
     * Check if a widget has a configuration activity declared.
     */
    fun hasConfigurationActivity(widgetId: Int): Boolean {
        return try {
            val providerInfo = appWidgetManager.getAppWidgetInfo(widgetId)
            providerInfo?.configure != null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking widget configuration: ${e.message}")
            false
        }
    }

    /**
     * Check if a widget requires initial configuration.
     */
    fun needsConfiguration(widgetId: Int): Boolean {
        return hasConfigurationActivity(widgetId)
    }

    /**
     * Check if a widget supports reconfiguration (Android 9+).
     * Widgets must declare WIDGET_FEATURE_RECONFIGURABLE for this.
     */
    fun isReconfigurable(widgetId: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        return try {
            val providerInfo = appWidgetManager.getAppWidgetInfo(widgetId) ?: return false
            providerInfo.configure != null &&
                    (providerInfo.widgetFeatures and
                            android.appwidget.AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE) != 0
        } catch (e: Exception) {
            Log.e(TAG, "Error checking reconfigurable: ${e.message}")
            false
        }
    }

    /**
     * Build the ActivityOptions bundle needed for Android 14+ to allow
     * background activity starts from the widget host.
     */
    private fun buildConfigureOptions(): android.os.Bundle? {
        return if (Build.VERSION.SDK_INT >= 34) {
            try {
                ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    )
                    .toBundle()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to create ActivityOptions for API 34+", e)
                null
            }
        } else {
            null
        }
    }

    /**
     * Starts the configuration activity for a widget using the CORRECT system-mediated approach.
     *
     * DO NOT use direct Intent + startActivityForResult — that will fail with
     * SecurityException for any widget whose configure activity is not exported.
     *
     * @param activity The hosting Activity (needed for startActivityForResult)
     * @param widgetId The app widget ID to configure
     * @param requestCode The request code for onActivityResult
     * @return true if the configuration activity was started successfully
     */
    fun startWidgetConfiguration(
        activity: Activity,
        widgetId: Int,
        requestCode: Int
    ): Boolean {
        return try {
            val providerInfo = appWidgetManager.getAppWidgetInfo(widgetId)
            if (providerInfo?.configure == null) {
                Log.w(TAG, "Widget $widgetId has no configure activity")
                return false
            }

            Log.d(TAG, "Starting widget configure via AppWidgetHost for widget $widgetId " +
                    "(configure=${providerInfo.configure.flattenToShortString()})")

            appWidgetHost.startAppWidgetConfigureActivityForResult(
                activity,
                widgetId,
                0,              // intentFlags (unused in current AOSP implementation)
                requestCode,
                buildConfigureOptions()  // ActivityOptions bundle for API 34+
            )

            Log.d(TAG, "Successfully launched configure activity for widget $widgetId")
            true
        } catch (e: android.content.ActivityNotFoundException) {
            Log.e(TAG, "Configure activity not found for widget $widgetId", e)
            false
        } catch (e: SecurityException) {
            // This should NOT happen with AppWidgetHost, but handle gracefully
            Log.e(TAG, "SecurityException starting widget configuration — this is unexpected " +
                    "when using AppWidgetHost", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start widget configuration for widget $widgetId", e)
            false
        }
    }
}
