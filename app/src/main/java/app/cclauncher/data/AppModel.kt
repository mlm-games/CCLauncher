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
    val lastLaunchTime: Long = 0
) : Comparable<AppModel> {
    override fun compareTo(other: AppModel): Int = when {
        key != null && other.key != null -> key.compareTo(other.key)
        else -> appLabel.compareTo(other.appLabel, ignoreCase = true)
    }

    fun getKey(): String = AppKey.of(appPackage, userString)
}

object AppKey {
    fun of(packageName: String, userString: String): String =
        "${packageName.trim()}/${userString.trim()}"
}