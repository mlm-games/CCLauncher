package app.cclauncher.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Configuration options for widgets
 */
@Parcelize
@Serializable
data class WidgetConfig(
    val backgroundColor: Long = 0x33000000,  // Default: semi-transparent black
    val cornerRadius: Float = 16f,
    val padding: Int = 8,
    val elevation: Float = 4f,

    // Widget-specific settings
    val showTitle: Boolean = false,          // Whether to show widget title
    val customTitle: String = "",            // Custom title text
    val refreshInterval: Long = 0,           // Auto-refresh interval in ms (0 = never)

    val touchEnabled: Boolean = true,        // Whether widget responds to touch
    val allowResize: Boolean = true          // Whether widget can be resized
) : Parcelable