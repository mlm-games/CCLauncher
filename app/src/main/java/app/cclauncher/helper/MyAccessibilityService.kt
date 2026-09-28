package app.cclauncher.helper

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import app.cclauncher.settings.AppSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MyAccessibilityService : AccessibilityService(), KoinComponent {

    companion object {
        @Volatile
        private var connected: MyAccessibilityService? = null

        @Suppress("InlinedApi")
        fun lockScreenIfConnected(): Boolean =
            connected?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) ?: false
    }

    private val settingsRepository: AppSettingsRepository by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "LOCK_SCREEN") {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
        return START_NOT_STICKY
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        connected = this
        serviceScope.launch {
            runCatching {
                settingsRepository.updateSetting {
                    it.copy(lockMode = it.doubleTapToLock && it.accessibilityConsent)
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        if (connected === this) connected = null
        serviceScope.cancel()
        super.onDestroy()
    }
}
