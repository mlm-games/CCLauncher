package app.cclauncher.helper

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.UserHandle
import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.graphics.createBitmap
import app.cclauncher.helper.iconpack.IconPackManager

/**
 * Enhanced cache for app icons with icon pack support
 */
class IconCache(private val context: Context) {
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val iconCache = LruCache<String, Bitmap>(200)
    private val iconPackManager = IconPackManager(context)

    /**
     * Get available icon packs
     */
    suspend fun getAvailableIconPacks() = iconPackManager.getAvailableIconPacks()

    /**
     * Get an app icon with icon pack support
     */
    suspend fun getIcon(
        packageName: String,
        className: String?,
        user: UserHandle,
        iconPackName: String = "default"
    ): ImageBitmap? {
        val componentName = "$packageName/${className ?: ""}"
        val cacheKey = "$iconPackName|$packageName|$className|${user.hashCode()}"

        // Check cache first
        synchronized(iconCache) {
            iconCache[cacheKey]?.let { return it.asImageBitmap() }
        }

        // Load icon if not in cache
        return withContext(Dispatchers.IO) {
            try {
                // Get original icon first
                val appInfo = launcherApps.getActivityList(packageName, user)
                    .find { it.componentName.className == className }

                val originalIcon = appInfo?.getIcon(0)

                // Get icon from icon pack (with fallback to original)
                val finalIcon = if (iconPackName != "default") {
                    iconPackManager.getIconFromPack(iconPackName, componentName, originalIcon)
                } else {
                    originalIcon?.let { drawableToBitmap(it)?.asImageBitmap() }
                }

                // Cache the final bitmap
                finalIcon?.let { imageBitmap ->
                    val bitmap = drawableToBitmap(originalIcon ?: return@let)
                    bitmap?.let {
                        synchronized(iconCache) {
                            iconCache.put(cacheKey, it)
                        }
                    }
                }

                finalIcon
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Convert a drawable to a bitmap
     */
    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        try {
            val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 48
            val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 48

            val bitmap = createBitmap(width, height)
            val canvas = Canvas(bitmap)

            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Clear the icon cache
     */
    fun clearCache() {
        synchronized(iconCache) {
            iconCache.evictAll()
        }
        iconPackManager.clearCache()
    }
}
