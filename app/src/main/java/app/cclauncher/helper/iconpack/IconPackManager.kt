package app.cclauncher.helper.iconpack

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.cclauncher.helper.BitmapUtils
import app.cclauncher.helper.BitmapUtils.drawableToBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.util.concurrent.ConcurrentHashMap

class IconPackManager(private val context: Context) {
    private val packageManager = context.packageManager
    private val iconPackCache = LruCache<String, Bitmap>(100)
    private val iconPackMappings = ConcurrentHashMap<String, IconPackInfo>()

    data class IconPackInfo(
        val packageName: String,
        val name: String,
        val componentMap: Map<String, String> = emptyMap(),
        val isLoaded: Boolean = false
    )

    /**
     * Get available icon packs
     */
    suspend fun getAvailableIconPacks(): List<IconPackInfo> = withContext(Dispatchers.IO) {
        val iconPacks = mutableListOf<IconPackInfo>()

        // Add default option
        iconPacks.add(IconPackInfo("default", "Default Icons"))

        try {
            // Find installed icon packs
            val intent = packageManager.queryIntentActivities(
                android.content.Intent("org.adw.launcher.THEMES"), 0
            ) + packageManager.queryIntentActivities(
                android.content.Intent("com.gau.go.launcherex.theme"), 0
            ) + packageManager.queryIntentActivities(
                android.content.Intent("com.anddoes.launcher.THEME"), 0
            )

            intent.distinctBy { it.activityInfo.packageName }.forEach { resolveInfo ->
                try {
                    val packageName = resolveInfo.activityInfo.packageName
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    val name = packageManager.getApplicationLabel(appInfo).toString()

                    iconPacks.add(IconPackInfo(packageName, name))
                } catch (e: Exception) {
                    // Skip invalid icon packs
                }
            }
        } catch (e: Exception) {
            // Continue with default only
        }

        iconPacks
    }

    /**
     * Load icon pack mappings in background
     */
    suspend fun loadIconPack(packageName: String): IconPackInfo? = withContext(Dispatchers.IO) {
        if (packageName == "default") {
            return@withContext IconPackInfo("default", "Default Icons", isLoaded = true)
        }

        // Check if already loaded
        iconPackMappings[packageName]?.let {
            if (it.isLoaded) return@withContext it
        }

        try {
            val resources = packageManager.getResourcesForApplication(packageName)
            val componentMap = parseAppFilter(resources, packageName)

            val iconPack = IconPackInfo(
                packageName = packageName,
                name = packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(packageName, 0)
                ).toString(),
                componentMap = componentMap,
                isLoaded = true
            )

            iconPackMappings[packageName] = iconPack
            iconPack
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get icon from icon pack
     */
    suspend fun getIconFromPack(
        iconPackName: String,
        componentName: String,
        fallbackIcon: Drawable?
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        if (iconPackName == "default") {
            return@withContext fallbackIcon?.let { drawableToBitmap(it)?.asImageBitmap() }
        }

        val cacheKey = "$iconPackName|$componentName"

        // Check cache first
        iconPackCache[cacheKey]?.let { return@withContext it.asImageBitmap() }

        try {
            val iconPack = iconPackMappings[iconPackName] ?: loadIconPack(iconPackName)
            iconPack?.let { pack ->
                val iconName = pack.componentMap[componentName]
                if (iconName != null) {
                    val resources = packageManager.getResourcesForApplication(iconPackName)
                    val iconId = resources.getIdentifier(iconName, "drawable", iconPackName)

                    if (iconId != 0) {
                        val drawable = resources.getDrawable(iconId, null)
                        val bitmap = drawableToBitmap(drawable)
                        bitmap?.let {
                            iconPackCache.put(cacheKey, it)
                            return@withContext it.asImageBitmap()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Fall back to default icon
        }

        // Fallback to original icon
        fallbackIcon?.let { drawableToBitmap(it)?.asImageBitmap() }
    }

    /**
     * Parse app-filter.xml from icon pack
     */
    private fun parseAppFilter(resources: Resources, packageName: String): Map<String, String> {
        val componentMap = mutableMapOf<String, String>()

        try {
            val appFilterId = resources.getIdentifier("appfilter", "xml", packageName)
            if (appFilterId == 0) return componentMap

            val parser = resources.getXml(appFilterId)
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")

                    if (component != null && drawable != null) {
                        // Parse component name (format: ComponentInfo{package/class})
                        val componentName = component.removePrefix("ComponentInfo{").removeSuffix("}")
                        componentMap[componentName] = drawable
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            // Return empty map on error
        }

        return componentMap
    }

    /**
     * Clear icon pack cache
     */
    fun clearCache() {
        iconPackCache.evictAll()
        iconPackMappings.clear()
    }
}
