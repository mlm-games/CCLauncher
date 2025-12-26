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
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    // cacheKey = "pack|package|class|userHash"
    private val iconCache = LruCache<String, Bitmap>(250)

    private val iconPackManager = IconPackManager(context)

    suspend fun getAvailableIconPacks() = iconPackManager.getAvailableIconPacks()

    suspend fun getIcon(
        packageName: String,
        className: String?,
        user: UserHandle,
        iconPackName: String = "default"
    ): ImageBitmap? = withContext(Dispatchers.IO) {

        // Resolve class name if null.
        val resolvedClassName = runCatching {
            val list = launcherApps.getActivityList(packageName, user)
            val info = list.firstOrNull { className == null || it.componentName.className == className }
                ?: list.firstOrNull()
            info?.componentName?.className
        }.getOrNull() ?: className

        val cacheKey = "$iconPackName|$packageName|$resolvedClassName|${user.hashCode()}"
        synchronized(iconCache) {
            iconCache[cacheKey]?.let { return@withContext it.asImageBitmap() }
        }

        val activityInfo = runCatching {
            launcherApps.getActivityList(packageName, user)
                .firstOrNull { resolvedClassName != null && it.componentName.className == resolvedClassName }
                ?: launcherApps.getActivityList(packageName, user).firstOrNull()
        }.getOrNull()

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