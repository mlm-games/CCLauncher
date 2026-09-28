package app.cclauncher

import android.app.Application
import app.cclauncher.di.appModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.logger.Level

class CLauncherApp : Application() {
    /** Process-wide IO scope for repository work. Never leaks an Activity context. */
    lateinit var applicationScope: CoroutineScope
        private set

    override fun onCreate() {
        super.onCreate()
        applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@CLauncherApp)
            modules(appModule)
        }
    }

    override fun onTerminate() {
        applicationScope.cancel()
        stopKoin()
        super.onTerminate()
    }
}
