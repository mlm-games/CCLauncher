package app.cclauncher.data

import android.os.UserHandle
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.runtime.Immutable
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
    val userString: String = user.toString()
) : Comparable<AppModel> {
    override fun compareTo(other: AppModel): Int = when {
        key != null && other.key != null -> key.compareTo(other.key)
        else -> appLabel.compareTo(other.appLabel, true)
    }

    fun getKey(): String = "$appPackage/${userString}"
}