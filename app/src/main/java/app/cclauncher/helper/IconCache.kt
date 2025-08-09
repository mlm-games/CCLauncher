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
                    originalIcon?.let { BitmapUtils.drawableToBitmap(it)?.asImageBitmap() }
                }

                // Cache the final bitmap
                finalIcon?.let { imageBitmap ->
                    val bitmap = BitmapUtils.drawableToBitmap(originalIcon)
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
     * Clear the icon cache
     */
    fun clearCache() {
        synchronized(iconCache) {
            iconCache.evictAll()
        }
        iconPackManager.clearCache()
    }
}
