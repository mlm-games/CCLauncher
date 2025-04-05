package app.cclauncher.helper

import android.app.Activity
import android.app.AppOpsManager
import android.app.role.RoleManager
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.WindowManager
import androidx.annotation.RequiresApi
import app.cclauncher.data.Constants

@RequiresApi(Build.VERSION_CODES.Q)
fun Activity.showLauncherSelector(requestCode: Int) {
    val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
    if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
        startActivityForResult(intent, requestCode)
    } else
        resetDefaultLauncher()
}

fun Context.resetDefaultLauncher() {
//    try {
//        val componentName = ComponentName(this, FakeHomeActivity::class.java)
//        packageManager.setComponentEnabledSetting(
//            componentName,
//            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
//            PackageManager.DONT_KILL_APP
//        )
//        val selector = Intent(Intent.ACTION_MAIN)
//        selector.addCategory(Intent.CATEGORY_HOME)
//        startActivity(selector)
//        packageManager.setComponentEnabledSetting(
//            componentName,
//            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
//            PackageManager.DONT_KILL_APP
//        )
//    } catch (e: Exception) {
//        e.printStackTrace()
//    }
}


fun Context.isEinkDisplay(): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Modern API (Android 11+)
            val refreshRate = (getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)
                .getDisplay(Display.DEFAULT_DISPLAY)
                .refreshRate
            refreshRate <= Constants.MIN_ANIM_REFRESH_RATE
        } else {
            // Legacy API (pre-Android 11)
            @Suppress("DEPRECATION")
            val display = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
            display.refreshRate <= Constants.MIN_ANIM_REFRESH_RATE
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
fun Context.appUsagePermissionGranted(): Boolean {
    val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    return appOpsManager.unsafeCheckOpNoThrow(
        "android:get_usage_stats",
        android.os.Process.myUid(),
        packageName
    ) == AppOpsManager.MODE_ALLOWED
}
