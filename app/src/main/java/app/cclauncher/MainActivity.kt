package app.cclauncher

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.cclauncher.data.Constants
import app.cclauncher.data.Navigation
import app.cclauncher.data.PrefsDataStore
import app.cclauncher.helper.isEinkDisplay
import app.cclauncher.helper.isDarkThemeOn
import app.cclauncher.helper.isTablet
import app.cclauncher.helper.setPlainWallpaper
import app.cclauncher.helper.showLauncherSelector
import app.cclauncher.ui.CLauncherNavigation
import app.cclauncher.ui.util.updateStatusBarVisibility
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefsDataStore = PrefsDataStore(this)
        lifecycleScope.launch {
            val appTheme = prefsDataStore.appTheme.first()
            if (isEinkDisplay()) {
                prefsDataStore.setAppTheme(AppCompatDelegate.MODE_NIGHT_NO)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                AppCompatDelegate.setDefaultNightMode(appTheme)
            }
        }

        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        lifecycleScope.launch {
            val firstOpen = prefsDataStore.firstOpen.first()
            if (firstOpen) {
                viewModel.firstOpen(true)
                prefsDataStore.setFirstOpen(false)
                prefsDataStore.setFirstOpenTime(System.currentTimeMillis())
            }
        }

        lifecycleScope.launch {
            //ensure window is ready
            delay(500)
            prefsDataStore.preferences.first().let { prefs ->
                try {
                    updateStatusBarVisibility(this@MainActivity, prefs.statusBar)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }


        setupOrientation()
        window.addFlags(FLAG_LAYOUT_NO_LIMITS)

        setContent {
            val preferences by prefsDataStore.preferences.collectAsState(initial = null)

            CLauncherTheme {
                var currentScreen by remember { mutableStateOf(Navigation.HOME) }

                CLauncherNavigation(
                    viewModel = viewModel,
                    currentScreen = currentScreen,
                    onScreenChange = { screen ->
                        currentScreen = screen
                    }
                )
            }
        }

        initObservers()
        viewModel.loadApps()
    }

    private fun initObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.launcherResetFailed.collect { resetFailed ->
                    openLauncherChooser(resetFailed)
                }
            }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun setupOrientation() {
        if (isTablet(this) || Build.VERSION.SDK_INT == Build.VERSION_CODES.O)
            return
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    private fun openLauncherChooser(resetFailed: Boolean) {
        if (resetFailed) {
            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            startActivity(intent)
        }
    }

    private fun setPlainWallpaper() {
        if (this.isDarkThemeOn())
            setPlainWallpaper(this, android.R.color.black)
        else setPlainWallpaper(this, android.R.color.white)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        lifecycleScope.launch {
            val appTheme = viewModel.prefsDataStore.appTheme.first()
            AppCompatDelegate.setDefaultNightMode(appTheme)

            val plainWallpaper = viewModel.prefsDataStore.plainWallpaper.first()
            if (plainWallpaper && AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
                setPlainWallpaper()
                recreate()
            }
        }
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        when (result.resultCode) {
            RESULT_OK -> {
                when (result.data?.getIntExtra("requestCode", 0)) {
                    Constants.REQUEST_CODE_ENABLE_ADMIN -> {
                        lifecycleScope.launch {
                            viewModel.prefsDataStore.updatePreference { it.copy(lockMode = true) }
                        }
                    }
                    Constants.REQUEST_CODE_LAUNCHER_SELECTOR -> {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                        } else {
                            showLauncherSelector(Constants.REQUEST_CODE_LAUNCHER_SELECTOR)
                        }
                    }
                }
            }
        }
    }
}