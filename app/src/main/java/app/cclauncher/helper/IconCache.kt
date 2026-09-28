package app.cclauncher.helper

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.os.UserHandle
import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.cclauncher.helper.iconpack.IconPackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IconCache(context: Context) {
    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    // Byte-sized LRU (~12MB) instead of fixed 250 full-size bitmaps.
    private val iconCache = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount.coerceAtLeast(1)
    }

    private val iconPackManager = IconPackManager(appContext)

    suspend fun getAvailableIconPacks() = iconPackManager.getAvailableIconPacks()

    suspend fun getIcon(
        packageName: String,
        className: String?,
        user: UserHandle,
        iconPackName: String = "default"
    ): ImageBitmap? = withContext(Dispatchers.IO) {

        // Single binder call — reuse the activity list instead of querying 2-3x per icon.
        val activities = runCatching { launcherApps.getActivityList(packageName, user) }.getOrNull()
            .orEmpty()
        val resolvedClassName = activities
            .firstOrNull { className == null || it.componentName.className == className }
            ?.componentName?.className
            ?: activities.firstOrNull()?.componentName?.className
            ?: className

        val cacheKey = "$iconPackName|$packageName|$resolvedClassName|${user.hashCode()}"
        synchronized(iconCache) {
            iconCache[cacheKey]?.let { return@withContext it.asImageBitmap() }
        }

        val activityInfo = activities
            .firstOrNull { resolvedClassName != null && it.componentName.className == resolvedClassName }
            ?: activities.firstOrNull()

        val originalDrawable = runCatching { activityInfo?.getIcon(0) }.getOrNull()
        val componentName = if (resolvedClassName.isNullOrBlank()) {
            "$packageName/"
        } else {
            "$packageName/$resolvedClassName"
        }

        val finalBitmap: Bitmap? = when {
            iconPackName != "default" -> {
                iconPackManager.getBitmapFromPack(iconPackName, componentName)
                    ?: originalDrawable?.let { BitmapUtils.drawableToBitmap(it) }
            }
            else -> originalDrawable?.let { BitmapUtils.drawableToBitmap(it) }
        }

        finalBitmap?.let { bmp ->
            synchronized(iconCache) { iconCache.put(cacheKey, bmp) }
            return@withContext bmp.asImageBitmap()
        }

        null
    }

    fun clearCache() {
        synchronized(iconCache) {
            iconCache.evictAll()
        }
        iconPackManager.clearCache()
    }
}