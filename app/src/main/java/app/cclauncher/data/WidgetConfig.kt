package app.cclauncher.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Configuration options for widgets
 */
@Parcelize
data class WidgetConfig(
    // Appearance settings
    val backgroundColor: Long = 0x33000000,  // Default: semi-transparent black
    val cornerRadius: Float = 16f,           // Default: 16dp rounded corners
    val padding: Int = 8,                    // Default: 8dp padding
    val elevation: Float = 4f,               // Default: 4dp elevation

    // Widget-specific settings
    val showTitle: Boolean = false,          // Whether to show widget title
    val customTitle: String = "",            // Custom title text
    val refreshInterval: Long = 0,           // Auto-refresh interval in ms (0 = never)

    // Advanced options
    val touchEnabled: Boolean = true,        // Whether widget responds to touch
    val allowResize: Boolean = true          // Whether widget can be resized
) : Parcelable