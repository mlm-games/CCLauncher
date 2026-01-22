package app.cclauncher

import android.app.Activity
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle
import android.util.Log
import androidx.core.content.getSystemService
import app.cclauncher.ui.components.snackbar.SnackbarManager
import org.koin.android.ext.android.inject

class ConfirmPinShortcutActivity : Activity() {

    companion object {
        private const val TAG = "ConfirmPinShortcut"
    }

    private val snackbarManager: SnackbarManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val launcherApps = getSystemService<LauncherApps>()
        if (launcherApps == null) {
            Log.e(TAG, "LauncherApps service not available")
            finish()
            return
        }

        val request = launcherApps.getPinItemRequest(intent)

        if (request == null || !request.isValid ||
            request.requestType != LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT
        ) {
            Log.e(TAG, "Invalid pin item request")
            finish()
            return
        }

        val shortcutInfo = request.shortcutInfo
        if (shortcutInfo == null) {
            Log.e(TAG, "No shortcut info in request")
            finish()
            return
        }

        Log.d(TAG, "Pin request received for ${shortcutInfo.`package`}/${shortcutInfo.id}")

        val accepted = request.accept()

        if (accepted) {
            snackbarManager.show("Shortcut '${shortcutInfo.shortLabel}' added")

            val refreshIntent = Intent("app.cclauncher.ACTION_REFRESH_APPS")
            refreshIntent.`package` = applicationContext.packageName
            sendBroadcast(refreshIntent)
            Log.d(TAG, "Shortcut pinned successfully, refresh broadcast sent")
        } else {
            Log.w(TAG, "Shortcut pin was rejected")
        }

        finish()
    }
}
