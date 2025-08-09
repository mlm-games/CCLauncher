package app.cclauncher.helper

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.net.toUri

/**
 * Centralized manager for all permission-related operations
 */
class PermissionManager(private val context: Context) {

    /**
     * Data class to hold all permission states
     */
    data class PermissionStatus(
        val isDefaultLauncher: Boolean = false,
        val hasUsageStats: Boolean = false,
        val hasAccessibility: Boolean = false,
        val canShowOnLockScreen: Boolean = false,
        val canDrawOverlays: Boolean = false
    ) {
        val hasAllPermissions: Boolean
            get() = isDefaultLauncher && hasUsageStats && hasAccessibility

        val hasEssentialPermissions: Boolean
            get() = isDefaultLauncher
    }

    /**
     * Check all permissions at once
     */
    fun checkAllPermissions(): PermissionStatus {
        return PermissionStatus(
            isDefaultLauncher = isDefaultLauncher(),
            hasUsageStats = hasUsageStatsPermission(),
            hasAccessibility = hasAccessibilityPermission(),
            canShowOnLockScreen = canShowOnLockScreen(),
            canDrawOverlays = canDrawOverlays()
        )
    }

    /**
     * Check if CCLauncher is the default launcher
     */
    fun isDefaultLauncher(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
            roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        } else {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfo = context.packageManager.resolveActivity(intent, 0)
            resolveInfo?.activityInfo?.packageName == context.packageName
        }
    }

    /**
     * Check if the app has usage stats permission
     */
    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

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
        return try {
            val enabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )

            if (enabled == 1) {
                val enabledServices = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: ""

                val serviceName = "${context.packageName}/${MyAccessibilityService::class.java.name}"
                enabledServices.contains(serviceName)
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Check if the app can show on lock screen
     */
    fun canShowOnLockScreen(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            try {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    "android:system_alert_window",
                    android.os.Process.myUid(),
                    context.packageName
                ) == AppOpsManager.MODE_ALLOWED
            } catch (_: Exception) {
                false
            }
        } else {
            true // Not restricted on older versions
        }
    }

    /**
     * Check if the app can draw overlays
     */
    @SuppressLint("ObsoleteSdkInt")
    fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true // Not restricted on older versions
        }
    }

    // Navigation methods to settings screens

    /**
     * Open launcher selection dialog (Android 10+)
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestDefaultLauncher(): Intent {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
        return if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
            roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
        } else {
            // Fallback to home settings
            Intent(Settings.ACTION_HOME_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    /**
     * Open the usage access settings screen
     */
    fun openUsageAccessSettings() {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback if specific settings page not available
            openAppSettings()
        }
    }

    /**
     * Open the accessibility settings screen
     */
    fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openAppSettings()
        }
    }

    /**
     * Open overlay permission settings (Android 6+)
     */
    @SuppressLint("ObsoleteSdkInt")
    @RequiresApi(Build.VERSION_CODES.M)
    fun openOverlaySettings() {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = "package:${context.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openAppSettings()
        }
    }

    /**
     * Open the app info settings screen
     */
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Open default apps settings
     */
    fun openDefaultAppsSettings() {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openAppSettings()
        }
    }

    /**
     * Get a readable permission summary
     */
    fun getPermissionSummary(): String {
        val status = checkAllPermissions()
        val missing = mutableListOf<String>()

        if (!status.isDefaultLauncher) missing.add("Default Launcher")
        if (!status.hasUsageStats) missing.add("Usage Stats")
        if (!status.hasAccessibility) missing.add("Accessibility Service")

        return if (missing.isEmpty()) {
            "All permissions granted"
        } else {
            "Missing: ${missing.joinToString(", ")}"
        }
    }
}