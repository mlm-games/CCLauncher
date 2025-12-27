package app.cclauncher.helper

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import app.cclauncher.settings.AppSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@SuppressLint("AccessibilityPolicy")
class MyAccessibilityService : AccessibilityService(), KoinComponent {

    private val settingsRepository: AppSettingsRepository by inject()

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
            settingsRepository.updateSetting { it.copy(lockMode = true) }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {}

    override fun onInterrupt() {}
}