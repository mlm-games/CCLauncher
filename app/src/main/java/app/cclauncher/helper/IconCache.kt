package app.cclauncher.helper

import android.content.ComponentName
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

/**
 * Cache for app icons to improve performance
 */
class IconCache(context: Context) {
    private val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val iconCache = LruCache<String, Bitmap>(200)

    /**
     * Get an app icon, either from cache or by loading it
     */
    suspend fun getIcon(packageName: String, className: String?, user: UserHandle): ImageBitmap? {
        val cacheKey = "$packageName|$className|${user.hashCode()}"

        // Check cache first
        synchronized(iconCache) {
            iconCache[cacheKey]?.let { return it.asImageBitmap() }
        }

        // Load icon if not in cache
        return withContext(Dispatchers.IO) {
            try {
                val component = ComponentName(packageName, className ?: "")
                val appInfo = launcherApps.getActivityList(packageName, user)
                    .find { it.componentName.className == className }

                appInfo?.let {
                    val icon = it.getIcon(0)
                    val bitmap = drawableToBitmap(icon)

                    // Cache the bitmap
                    bitmap?.let {
                        synchronized(iconCache) {
                            iconCache.put(cacheKey, it)
                        }
                    }
                    bitmap?.asImageBitmap()
                }
            } catch (e: Exception) {
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
    }
}