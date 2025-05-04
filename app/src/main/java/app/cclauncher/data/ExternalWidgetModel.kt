package app.cclauncher.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Model for external widgets from other apps
 */
@Parcelize
@Serializable
data class ExternalWidgetModel(
    val id: String = "",
    val appWidgetId: Int = -1,
    val providerClassName: String = "",
    val packageName: String = "",
    val label: String = "",
    val previewImage: ByteArray? = null,
    val position: Int = 0,
    val width: Int = 1,  // Width in grid cells
    val height: Int = 1, // in grid cells
    val config: WidgetConfig = WidgetConfig(),
    val specialType: String = ""
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExternalWidgetModel

        if (id != other.id) return false
        if (appWidgetId != other.appWidgetId) return false
        if (providerClassName != other.providerClassName) return false
        if (packageName != other.packageName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + appWidgetId
        result = 31 * result + providerClassName.hashCode()
        result = 31 * result + packageName.hashCode()
        return result
    }
}