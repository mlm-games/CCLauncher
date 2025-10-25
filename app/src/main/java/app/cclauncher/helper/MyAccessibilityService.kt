package app.cclauncher.helper

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import app.cclauncher.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyAccessibilityService : AccessibilityService() {
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "LOCK_SCREEN") {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
        return START_NOT_STICKY
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        CoroutineScope(Dispatchers.Main).launch {
            SettingsRepository(applicationContext).updateSetting { it.copy(lockMode = true) }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // no-op
    }

    override fun onInterrupt() { /* no-op */ }
}