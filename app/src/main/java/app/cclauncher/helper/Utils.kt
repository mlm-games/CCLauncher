@file:Suppress("unused")

package app.cclauncher.helper

import android.annotation.SuppressLint
import android.app.SearchManager
import android.app.StatusBarManager
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.Point
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import app.cclauncher.R
import app.cclauncher.data.AnimationConstants
import app.cclauncher.data.AppKey
import app.cclauncher.data.AppModel
import app.cclauncher.data.Constants
import app.cclauncher.settings.AppSettingsRepository
import app.cclauncher.ui.theme.AnimationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

private const val TAG = "LauncherUtils"

fun getLauncherVisibleProfiles(
    userManager: UserManager,
    launcherApps: LauncherApps
): List<UserHandle> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        launcherApps.profiles
    } else {
        userManager.userProfiles
    }
}

suspend fun getAppsList(
    context: Context,
    settingsRepository: AppSettingsRepository,
    iconCache: IconCache,
    includeRegularApps: Boolean = true,
    includeHiddenApps: Boolean = false,
): MutableList<AppModel> = withContext(Dispatchers.IO) {

    val appList: MutableList<AppModel> = mutableListOf()

    try {
        val appContext = context.applicationContext
        val settings = settingsRepository.settings.first()
        val hiddenApps = settings.hiddenApps
        val includeIcons = settings.showAppIcons
        val renamedApps = settings.renamedApps
        val selectedIconPack = settings.selectedIconPack

        val userManager = appContext.getSystemService(Context.USER_SERVICE) as UserManager
        val launcherApps = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val collator = Collator.getInstance()
        val myUser = android.os.Process.myUserHandle()

        val profiles = getLauncherVisibleProfiles(userManager, launcherApps)

        for (profile in profiles) {
            val launcherUserInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                runCatching { launcherApps.getLauncherUserInfo(profile) }.getOrNull()
            } else null
            val isPrivateProfile =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM &&
                    launcherUserInfo?.userType == UserManager.USER_TYPE_PROFILE_PRIVATE

            if (isPrivateProfile && userManager.isQuietModeEnabled(profile)) {
                continue
            }

            val activities = runCatching { launcherApps.getActivityList(null, profile) }.getOrNull()
                .orEmpty()
            for (activity in activities) {
                val pkg = activity.applicationInfo.packageName

                // Skip the launcher itself
                if (pkg == appContext.packageName) continue

                val userString = profile.toString()
                val appKey = AppKey.appKey(pkg, activity.componentName.className, userString)
                val legacyKeys = listOf(
                    AppKey.legacyActivityUserHashKey(pkg, activity.componentName.className, profile.hashCode()),
                    AppKey.legacyPackageUserKey(pkg, userString)
                ).distinct()
                val keyCandidates = listOf(appKey) + legacyKeys

                val shouldMarkClone =
                    profile != myUser && !isPrivateProfile

                val defaultLabel = activity.label.toString() +
                    if (shouldMarkClone) " (Clone)" else ""

                val shownLabel = keyCandidates.firstNotNullOfOrNull { renamedApps[it] }
                    ?: defaultLabel
                val isHidden = keyCandidates.any { hiddenApps.contains(it) }
                val lastLaunchTime = keyCandidates.mapNotNull { settings.recentAppHistory[it] }
                    .maxOrNull() ?: 0L

                val appIcon = if (includeIcons) {
                    iconCache.getIcon(
                        packageName = pkg,
                        className = activity.componentName.className,
                        user = profile,
                        iconPackName = selectedIconPack
                    )
                } else null

                val model = AppModel(
                    appLabel = shownLabel,
                    key = collator.getCollationKey(activity.label.toString()),
                    appPackage = pkg,
                    activityClassName = activity.componentName.className,
                    isNew = (System.currentTimeMillis() - activity.firstInstallTime) < AnimationConstants.ONE_HOUR_IN_MILLIS,
                    user = profile,
                    appIcon = appIcon,
                    isHidden = isHidden,
                    userString = userString,
                    lastLaunchTime = lastLaunchTime
                )

                when {
                    isHidden && includeHiddenApps -> appList.add(model.copy(isHidden = true))
                    !isHidden && includeRegularApps -> appList.add(model)
                }
            }
        }

        when (settings.searchSortOrder) {
            Constants.SortOrder.ALPHABETICAL ->
                appList.sortBy { it.appLabel.lowercase(Locale.ROOT) }

            Constants.SortOrder.REVERSE_ALPHABETICAL ->
                appList.sortByDescending { it.appLabel.lowercase(Locale.ROOT) }

            Constants.SortOrder.RECENT_FIRST ->
                appList.sortWith(
                    compareByDescending<AppModel> { it.lastLaunchTime }
                        .thenBy { it.appLabel.lowercase(Locale.ROOT) }
                )

            else ->
                appList.sortBy { it.appLabel.lowercase(Locale.ROOT) }
        }
    } catch (e: Exception) {
        Log.e(TAG, "getAppsList failed", e)
    }

    appList
}

fun isPackageInstalled(context: Context, packageName: String, userString: String): Boolean {
    return try {
        val launcher = context.applicationContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val activityInfo = launcher.getActivityList(packageName, getUserHandleFromString(context, userString))
        activityInfo.isNotEmpty()
    } catch (e: Exception) {
        Log.w(TAG, "isPackageInstalled failed for $packageName", e)
        false
    }
}

fun getUserHandleFromString(context: Context, userHandleString: String): UserHandle {
    val userManager = context.applicationContext.getSystemService(Context.USER_SERVICE) as UserManager
    val launcherApps = context.applicationContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val profiles = getLauncherVisibleProfiles(userManager, launcherApps)

    for (userHandle in profiles) {
        if (userHandle.toString() == userHandleString) {
            return userHandle
        }
    }
    return android.os.Process.myUserHandle()
}

fun setPlainWallpaper(context: Context, color: Int) {
    try {
        val bitmap = createBitmap(1000, 2000)
        bitmap.eraseColor(context.getColor(color))
        val manager = WallpaperManager.getInstance(context.applicationContext)
        manager.setBitmap(bitmap, null, false, WallpaperManager.FLAG_SYSTEM)
        manager.setBitmap(bitmap, null, false, WallpaperManager.FLAG_LOCK)
        bitmap.recycle()
    } catch (e: Exception) {
        Log.e(TAG, "setPlainWallpaper failed", e)
    }
}

fun setPlainWallpaperByTheme(context: Context, appTheme: Int) {
    when (appTheme) {
        AppCompatDelegate.MODE_NIGHT_YES -> setPlainWallpaper(context, android.R.color.black)
        AppCompatDelegate.MODE_NIGHT_NO -> setPlainWallpaperLightGrey(context)
        else -> {
            if (context.isDarkThemeOn())
                setPlainWallpaper(context, android.R.color.black)
            else setPlainWallpaperLightGrey(context)
        }
    }
}

fun setPlainWallpaperLightGrey(context: Context) {
    try {
        val bitmap = createBitmap(1000, 2000)
        bitmap.eraseColor(0xFFEEEEEE.toInt()) // for light mode
        val manager = WallpaperManager.getInstance(context.applicationContext)
        manager.setBitmap(bitmap, null, false, WallpaperManager.FLAG_SYSTEM)
        manager.setBitmap(bitmap, null, false, WallpaperManager.FLAG_LOCK)
        bitmap.recycle()
    } catch (e: Exception) {
        Log.e(TAG, "setPlainWallpaperLightGrey failed", e)
    }
}

fun getChangedAppTheme(context: Context, currentAppTheme: Int): Int {
    return when (currentAppTheme) {
        AppCompatDelegate.MODE_NIGHT_YES -> AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.MODE_NIGHT_NO -> AppCompatDelegate.MODE_NIGHT_YES
        else -> {
            if (context.isDarkThemeOn())
                AppCompatDelegate.MODE_NIGHT_NO
            else AppCompatDelegate.MODE_NIGHT_YES
        }
    }
}

fun openAppInfo(context: Context, app: AppModel) {
    val launcher = context.applicationContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    val directComponent = app.activityClassName
        ?.takeIf { it.isNotBlank() }
        ?.let { ComponentName(app.appPackage, it) }

    val activities = runCatching { launcher.getActivityList(null, app.user) }.getOrNull().orEmpty()
    val fallbackComponent = activities
        .firstOrNull {
            it.applicationInfo.packageName == app.appPackage &&
                (app.activityClassName == null || it.componentName.className == app.activityClassName)
        }
        ?.componentName
        ?: activities
            .firstOrNull { it.applicationInfo.packageName == app.appPackage }
            ?.componentName

    val component = directComponent ?: fallbackComponent

    if (component != null) {
        try {
            launcher.startAppDetailsActivity(component, app.user, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "openAppInfo failed", e)
            context.showToast(context.getString(R.string.unable_to_open_app))
        }
    } else {
        context.showToast(context.getString(R.string.unable_to_open_app))
    }
}

fun getScreenDimensions(context: Context): Pair<Int, Int> {
    val windowManager = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bounds = windowManager.currentWindowMetrics.bounds
        Pair(bounds.width(), bounds.height())
    } else {
        // Fallback for pre-R (minSdk 24 still supports these devices)
        @Suppress("DEPRECATION")
        val display = windowManager.defaultDisplay
        val point = Point()
        @Suppress("DEPRECATION")
        display.getRealSize(point)
        Pair(point.x, point.y)
    }
}


fun openSearch(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, "")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "No web search handler", e)
        context.showToast(R.string.unable_to_open_app)
    }
}

@SuppressLint("WrongConstant", "InlinedApi")
fun expandNotificationDrawer(context: Context) {
    try {
        val statusBarManager = context.applicationContext
            .getSystemService(Context.STATUS_BAR_SERVICE) as StatusBarManager
        statusBarManager.javaClass.getMethod("expandNotificationsPanel").invoke(statusBarManager)
    } catch (e: Exception) {
        Log.w(TAG, "expandNotificationsPanel failed", e)
    }
}

fun openAlarmApp(context: Context) {
    try {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "No alarm app found", e)
        context.showToast(R.string.unable_to_open_app)
    }
}

fun openCalendar(context: Context) {
    try {
        val calendarUri = CalendarContract.CONTENT_URI
            .buildUpon()
            .appendPath("time")
            .build()
        context.startActivity(Intent(Intent.ACTION_VIEW, calendarUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "No calendar app found", e)
        context.showToast(R.string.unable_to_open_app)
    }
}

fun isTablet(context: Context): Boolean {
    return try {
        val windowManager = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = context.resources.displayMetrics

        val (widthPixels, heightPixels) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            @Suppress("DEPRECATION")
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(dm)
            dm.widthPixels to dm.heightPixels
        }

        val widthInches = widthPixels / metrics.xdpi
        val heightInches = heightPixels / metrics.ydpi
        val diagonalInches = sqrt(widthInches.toDouble().pow(2.0) + heightInches.toDouble().pow(2.0))

        diagonalInches >= 7.0
    } catch (e: Exception) {
        Log.w(TAG, "isTablet check failed", e)
        false
    }
}


fun Context.isDarkThemeOn(): Boolean {
    return resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES
}

fun Context.copyToClipboard(text: String) {
    if (text.isBlank()) return
    val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = ClipData.newPlainText(getString(R.string.app_name), text)
    clipboardManager.setPrimaryClip(clipData)
}

fun Context.openUrl(url: String) {
    if (url.isBlank()) return
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "No browser for $url", e)
        showToast(R.string.unable_to_open_app)
    }
}

fun Context.isSystemApp(packageName: String): Boolean {
    if (packageName.isBlank()) return true
    return try {
        val applicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(packageName, 0)
        }
        ((applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0)
                || (applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0))
    } catch (e: PackageManager.NameNotFoundException) {
        false
    } catch (e: Exception) {
        Log.w(TAG, "isSystemApp failed for $packageName", e)
        false
    }
}

fun Context.uninstall(packageName: String) {
    if (packageName.isBlank()) return
    try {
        val intent = Intent(Intent.ACTION_DELETE, "package:$packageName".toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "No uninstall handler", e)
        showToast(R.string.unable_to_open_app)
    }
}

@ColorInt
fun Context.getColorFromAttr(
    @AttrRes attrColor: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true,
): Int {
    theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.data
}

fun View.animateAlpha(alpha: Float = 1.0f) {
    this.animate().apply {
        interpolator = LinearInterpolator()
        duration = AnimationConfig.SUB_QUICK.toLong()
        alpha(alpha)
        start()
    }
}

fun Context.shareApp() {
    try {
        val message = getString(R.string.are_you_using_your_phone_or_is_your_phone_using_you) +
                "\n" + Constants.URL_CCLAUNCHER_GITHUB
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, message)
            type = "text/plain"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val shareIntent = Intent.createChooser(sendIntent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(shareIntent)
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "No share handler", e)
    }
}

fun Context.starApp() {
    try {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Constants.URL_CCLAUNCHER_GITHUB.toUri()
        ).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_NEW_TASK
            )
        }
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "No browser for star link", e)
        showToast(R.string.unable_to_open_app)
    }
}

fun AppModel.resolveUser(context: Context): UserHandle =
    getUserHandleFromString(context, userString)

fun AppModel.withResolvedUser(context: Context): AppModel =
    copy(user = resolveUser(context))
