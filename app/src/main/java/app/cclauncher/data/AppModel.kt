package app.cclauncher.data

import android.os.UserHandle
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.runtime.Immutable
import java.text.CollationKey

@Immutable
data class AppModel(
    val appLabel: String,
    val key: CollationKey?,
    val appPackage: String,
    val activityClassName: String?,
    val isNew: Boolean = false,
    val user: UserHandle,
    val appIcon: ImageBitmap? = null,
    val isHidden: Boolean = false
) : Comparable<AppModel> {
    override fun compareTo(other: AppModel): Int = when {
        key != null && other.key != null -> key.compareTo(other.key)
        else -> appLabel.compareTo(other.appLabel, true)
    }

    fun getKey(): String = "$appPackage/${user.hashCode()}"
}