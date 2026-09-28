package app.cclauncher.helper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap

/**
 * Utilities for bitmap related operations
 */
object BitmapUtils {
    const val MAX_ICON_SIZE_PX = 192

    /**
     * Convert a drawable to a bitmap, downscaling to [MAX_ICON_SIZE_PX] to bound memory.
     * @param drawable The drawable to convert
     * @param defaultSize The default size to use if intrinsic dimensions are invalid
     * @return The converted bitmap, or null if conversion fails
     */
    fun drawableToBitmap(
        drawable: Drawable?,
        defaultSize: Int = 48
    ): Bitmap? {
        if (drawable == null) return null

        return try {
            var width = drawable.intrinsicWidth.takeIf { it > 0 } ?: defaultSize
            var height = drawable.intrinsicHeight.takeIf { it > 0 } ?: defaultSize

            // Downscale huge adaptive icons to bound memory (prevents OOM on low-RAM devices).
            val scale = minOf(1f, MAX_ICON_SIZE_PX / maxOf(width, height).toFloat())
            if (scale < 1f) {
                width = (width * scale).toInt().coerceAtLeast(1)
                height = (height * scale).toInt().coerceAtLeast(1)
            }

            val bitmap = createBitmap(width, height)
            val canvas = Canvas(bitmap)

            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            bitmap
        } catch (e: Exception) {
            android.util.Log.e("BitmapUtils", "drawableToBitmap failed", e)
            null
        }
    }
}