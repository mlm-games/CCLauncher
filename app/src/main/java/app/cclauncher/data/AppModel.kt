package app.cclauncher.data

import android.os.UserHandle
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.text.CollationKey

@Serializable
@Immutable
data class AppModel(
    val appLabel: String,
    @Transient
    val key: CollationKey? = null,
    val appPackage: String,
    val activityClassName: String?,
    val isNew: Boolean = false,
    @Transient
    val user: UserHandle = android.os.Process.myUserHandle(),
    @Transient
    val appIcon: ImageBitmap? = null,
    val isHidden: Boolean = false,
    val userString: String = user.toString(),
    @Transient
    val lastLaunchTime: Long = 0,

    val isSystemShortcut: Boolean = false,
    val systemShortcutId: String? = null,
    val systemShortcutPackage: String? = null
) : Comparable<AppModel> {
    override fun compareTo(other: AppModel): Int = when {
        key != null && other.key != null -> key.compareTo(other.key)
        else -> appLabel.compareTo(other.appLabel, ignoreCase = true)
    }

    fun getKey(): String = if (isSystemShortcut) {
        AppKey.shortcutKey(systemShortcutPackage, systemShortcutId, userString)
    } else {
        AppKey.appKey(appPackage, activityClassName, userString)
    }
}

object AppKey {
    fun appKey(packageName: String, activityClassName: String?, userString: String): String =
        "${packageName.trim()}/${activityClassName.orEmpty()}/${userString.trim()}"

    fun shortcutKey(packageName: String?, shortcutId: String?, userString: String): String =
        "shortcut:${packageName.orEmpty().trim()}/${shortcutId.orEmpty().trim()}/${userString.trim()}"

    fun legacyPackageUserKey(packageName: String, userString: String): String =
        "${packageName.trim()}/${userString.trim()}"

    fun legacyActivityUserHashKey(packageName: String, activityClassName: String?, userHash: Int): String =
        "${packageName.trim()}/${activityClassName.orEmpty()}/${userHash}"

    fun legacyShortcutKey(packageName: String?, shortcutId: String?, userHash: Int): String =
        "shortcut_sys:${packageName.orEmpty().trim()}_${shortcutId.orEmpty().trim()}_${userHash}"

    fun legacyMoveKeysForApp(app: AppModel): Set<String> =
        if (app.isSystemShortcut) {
            setOf(legacyShortcutKey(app.systemShortcutPackage, app.systemShortcutId, app.user.hashCode()))
        } else {
            setOf(legacyActivityUserHashKey(app.appPackage, app.activityClassName, app.user.hashCode()))
        }

    fun legacyCopyKeysForApp(app: AppModel): Set<String> =
        if (app.isSystemShortcut) {
            emptySet()
        } else {
            setOf(legacyPackageUserKey(app.appPackage, app.userString))
        }

    fun legacyKeyCandidatesForApp(app: AppModel): Set<String> =
        (legacyMoveKeysForApp(app) + legacyCopyKeysForApp(app))
            .filter { it.isNotBlank() }
            .toSet()
}
