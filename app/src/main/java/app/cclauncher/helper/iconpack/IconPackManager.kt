package app.cclauncher.helper.iconpack

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.cclauncher.helper.BitmapUtils.drawableToBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.util.concurrent.ConcurrentHashMap

class IconPackManager(context: Context) {
    private val packageManager = context.packageManager

    // cacheKey = "pack|package/class"
    private val iconPackCache = LruCache<String, Bitmap>(150)
    private val iconPackMappings = ConcurrentHashMap<String, IconPackInfo>()

    data class IconPackInfo(
        val packageName: String,
        val name: String,
        val componentMap: Map<String, String> = emptyMap(),
        val isLoaded: Boolean = false
    )

    suspend fun getAvailableIconPacks(): List<IconPackInfo> = withContext(Dispatchers.IO) {
        val iconPacks = mutableListOf<IconPackInfo>()
        iconPacks.add(IconPackInfo("default", "Default Icons"))

        runCatching {
            val intentResults =
                packageManager.queryIntentActivities(android.content.Intent("org.adw.launcher.THEMES"), 0) +
                        packageManager.queryIntentActivities(android.content.Intent("com.gau.go.launcherex.theme"), 0) +
                        packageManager.queryIntentActivities(android.content.Intent("com.anddoes.launcher.THEME"), 0)

            intentResults
                .distinctBy { it.activityInfo.packageName }
                .forEach { resolveInfo ->
                    val packageName = resolveInfo.activityInfo.packageName
                    runCatching {
                        val appInfo = packageManager.getApplicationInfo(packageName, 0)
                        val name = packageManager.getApplicationLabel(appInfo).toString()
                        iconPacks.add(IconPackInfo(packageName, name))
                    }
                }
        }

        iconPacks
    }

    suspend fun loadIconPack(packageName: String): IconPackInfo? = withContext(Dispatchers.IO) {
        if (packageName == "default") {
            return@withContext IconPackInfo("default", "Default Icons", isLoaded = true)
        }

        iconPackMappings[packageName]?.let { if (it.isLoaded) return@withContext it }

        return@withContext runCatching {
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
        }.getOrNull()
    }

    /**
     * Returns a Bitmap if found in the icon pack. Does NOT apply fallback.
     */
    suspend fun getBitmapFromPack(
        iconPackName: String,
        componentName: String
    ): Bitmap? = withContext(Dispatchers.IO) {

        if (iconPackName == "default") return@withContext null

        val cacheKey = "$iconPackName|$componentName"
        iconPackCache[cacheKey]?.let { return@withContext it }

        val iconPack = iconPackMappings[iconPackName] ?: loadIconPack(iconPackName) ?: return@withContext null
        val iconName = iconPack.componentMap[componentName] ?: return@withContext null

        val resources = runCatching { packageManager.getResourcesForApplication(iconPackName) }.getOrNull()
            ?: return@withContext null

        val iconId = resources.getIdentifier(iconName, "drawable", iconPackName)
        if (iconId == 0) return@withContext null

        val drawable = runCatching { resources.getDrawable(iconId, null) }.getOrNull() ?: return@withContext null
        val bmp = drawableToBitmap(drawable) ?: return@withContext null

        iconPackCache.put(cacheKey, bmp)
        bmp
    }

    /**
     * Icon pack icon (if exists) else fallback drawable.
     */
    suspend fun getIconFromPack(
        iconPackName: String,
        componentName: String,
        fallbackIcon: Drawable?
    ): ImageBitmap? = withContext(Dispatchers.IO) {

        if (iconPackName == "default") {
            return@withContext fallbackIcon?.let { drawableToBitmap(it)?.asImageBitmap() }
        }

        val bmp = getBitmapFromPack(iconPackName, componentName)
            ?: fallbackIcon?.let { drawableToBitmap(it) }

        bmp?.asImageBitmap()
    }

    private fun parseAppFilter(resources: Resources, packageName: String): Map<String, String> {
        val out = mutableMapOf<String, String>()

        val appFilterId = resources.getIdentifier("appfilter", "xml", packageName)
        if (appFilterId == 0) return out

        return runCatching {
            val parser = resources.getXml(appFilterId)
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")

                    if (!component.isNullOrBlank() && !drawable.isNullOrBlank()) {
                        val raw = component.removePrefix("ComponentInfo{").removeSuffix("}")
                        val parts = raw.split('/')
                        if (parts.size == 2) {
                            val pkg = parts[0]
                            val clsRaw = parts[1]
                            val cls = if (clsRaw.startsWith(".")) pkg + clsRaw else clsRaw
                            out["$pkg/$cls"] = drawable
                        } else {
                            // Fallback: keep raw as-is
                            out[raw] = drawable
                        }
                    }
                }
                eventType = parser.next()
            }

            out
        }.getOrDefault(out)
    }

    fun clearCache() {
        iconPackCache.evictAll()
        iconPackMappings.clear()
    }
}