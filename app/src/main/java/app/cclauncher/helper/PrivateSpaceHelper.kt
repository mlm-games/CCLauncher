package app.cclauncher.helper

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * Manages interaction with Android 15's Private Space feature
 */
class PrivateSpaceHelper(private val context: Context) {
    private val TAG = "PrivateSpaceHelper"

    /**
     * Checks whether the device supports private space.
     */
    fun isPrivateSpaceSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
    }

    /**
     * Get the private space user if available.
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun getPrivateSpaceUser(): UserHandle? {
        if (!isPrivateSpaceSupported()) {
            Log.d(TAG, "Private Space not supported on this device")
            return null
        }

        try {
            val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

            Log.d(TAG, "Checking ${userManager.userProfiles.size} user profiles for Private Space")

            val privateSpaceUser = userManager.userProfiles.firstOrNull { userHandle ->
                val userInfo = launcherApps.getLauncherUserInfo(userHandle)
                val isPrivate = userInfo?.userType == UserManager.USER_TYPE_PROFILE_PRIVATE
                Log.d(TAG, "User ${userHandle}: userType=${userInfo?.userType}, isPrivate=$isPrivate")
                isPrivate
            }

            Log.d(TAG, "Private Space user found: ${privateSpaceUser != null}")
            return privateSpaceUser
        } catch (e: Exception) {
            Log.e(TAG, "Error getting private space user", e)
            return null
        }
    }

    /**
     * Check if the given user profile is the private space.
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun isPrivateSpaceProfile(userHandle: UserHandle): Boolean {
        if (!isPrivateSpaceSupported()) {
            return false
        }

        try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val userInfo = launcherApps.getLauncherUserInfo(userHandle)
            return userInfo?.userType == UserManager.USER_TYPE_PROFILE_PRIVATE
        } catch (e: Exception) {
            Log.e(TAG, "Failed to determine if profile is Private Space", e)
            return false
        }
    }

    /**
     * Check whether the user has created a private space and whether CCLauncher can access it.
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun isPrivateSpaceSetUp(): Boolean {
        if (!isPrivateSpaceSupported()) {
            return false
        }

        try {
            val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager

            // Check if any user profile is a Private Space profile
            return userManager.userProfiles.any { userHandle ->
                try {
                    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                    val userInfo = launcherApps.getLauncherUserInfo(userHandle)
                    userInfo?.userType == UserManager.USER_TYPE_PROFILE_PRIVATE
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking user profile type", e)
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error determining if Private Space is set up", e)
            return false
        }
    }

    /**
     * Check if the private space is locked.
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun isPrivateSpaceLocked(): Boolean {
        if (!isPrivateSpaceSupported()) {
            return false
        }

        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val privateSpaceUser = getPrivateSpaceUser() ?: return false

        return userManager.isQuietModeEnabled(privateSpaceUser)
    }

    /**
     * Lock or unlock the private space.
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun setPrivateSpaceLock(lock: Boolean) {
        if (!isPrivateSpaceSupported()) {
            return
        }

        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val privateSpaceUser = getPrivateSpaceUser() ?: return

        try {
            userManager.requestQuietModeEnabled(lock, privateSpaceUser)
            Log.d(TAG, "Private space ${if (lock) "locked" else "unlocked"}")
        } catch (e: Exception) {
            Log.e(TAG, "Error changing private space lock state", e)
        }
    }

    /**
     * Toggle the lock state of the private space.
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun togglePrivateSpaceLock(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        if (!isPrivateSpaceSupported()) {
            onFailure("Private Space requires Android 15 or higher")
            return
        }

        if (!isPrivateSpaceSetUp()) {
            onFailure("Private Space is not set up on this device")
            return
        }

        val currentlyLocked = isPrivateSpaceLocked()
        setPrivateSpaceLock(!currentlyLocked)

        // Verify the change after a short delay
        Handler(Looper.getMainLooper()).postDelayed({
            if (isPrivateSpaceLocked() == !currentlyLocked) {
                onSuccess()
            } else {
                onFailure("Failed to ${if (currentlyLocked) "unlock" else "lock"} Private Space")
            }
        }, 500) // Short delay to allow the system to process the change
    }

    /**
     * Get a list of apps in the private space
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun getPrivateSpaceApps(): List<String> {
        if (!isPrivateSpaceSupported()) {
            return emptyList()
        }

        val privateSpaceUser = getPrivateSpaceUser() ?: return emptyList()
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

        try {
            return launcherApps.getActivityList(null, privateSpaceUser)
                .map { it.applicationInfo.packageName }
                .distinct()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting private space apps", e)
            return emptyList()
        }
    }

    /**
     * Get the intent to open Private Space settings
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun getPrivateSpaceSettingsIntentSender(): android.content.IntentSender? {
        if (!isPrivateSpaceSupported()) {
            return null
        }

        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            launcherApps.privateSpaceSettingsIntent
        } else {
            null
        }
    }
}