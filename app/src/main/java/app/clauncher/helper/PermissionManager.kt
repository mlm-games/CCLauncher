package app.clauncher.helper

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi

/**
 * Manager for handling permission-related operations
 */
class PermissionManager(private val context: Context) {

    /**
     * Check if the app has usage stats permission
     */
    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE)
                as AppOpsManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            ) == AppOpsManager.MODE_ALLOWED
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            ) == AppOpsManager.MODE_ALLOWED
        }
    }

    /**
     * Check if the app has accessibility service permission
     */
    fun hasAccessibilityPermission(): Boolean {
        return isAccessServiceEnabled(context)
    }

    /**
     * Open the usage access settings screen
     */
    fun openUsageAccessSettings() {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback if specific settings page not available
            openAppSettings()
        }
    }

    /**
     * Open the accessibility settings screen
     */
    fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            openAppSettings()
        }
    }

    /**
     * Open the app info settings screen
     */
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", context.packageName, null)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}