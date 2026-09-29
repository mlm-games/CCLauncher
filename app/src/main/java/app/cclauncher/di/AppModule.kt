package app.cclauncher.di

import android.appwidget.AppWidgetHost
import app.cclauncher.MainViewModel
import app.cclauncher.data.WidgetConstants
import app.cclauncher.data.repository.AppRepository
import app.cclauncher.helper.IconCache
import app.cclauncher.helper.PermissionManager
import app.cclauncher.helper.PrivateSpaceHelper
import app.cclauncher.helper.iconpack.IconPackManager
import app.cclauncher.settings.AppSettingsRepository
import app.cclauncher.settings.ccLauncherStringResourceProvider
import app.cclauncher.ui.components.snackbar.SnackbarManager
import app.cclauncher.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import io.github.mlmgames.settings.core.resources.StringResourceProvider

val appModule = module {

    // Application-scoped IO supervisor. Cancelled only on process death; tied to Application lifecycle.
    single { CoroutineScope(SupervisorJob() + Dispatchers.IO) }

    single<StringResourceProvider> { ccLauncherStringResourceProvider(androidApplication()) }

    single { AppSettingsRepository(androidApplication()) }

    single { AppWidgetHost(androidContext(), WidgetConstants.APPWIDGET_HOST_ID) }

    // Singletons so icon caches actually hit instead of being rebuilt per loadApps() call.
    single { IconCache(androidApplication()) }
    single { IconPackManager(androidApplication()) }
    single { PrivateSpaceHelper(androidApplication()) }
    single { PermissionManager(androidApplication()) }

    single {
        AppRepository(
            context = androidApplication(),
            settingsRepository = get(),
            iconCache = get(),
            coroutineScope = get<CoroutineScope>()
        )
    }

    single { SnackbarManager() }

    viewModel {
        MainViewModel(
            application = androidApplication(),
            appWidgetHost = get(),
            settingsRepository = get(),
            appRepository = get(),
            snackbarManager = get(),
            iconCache = get(),
            privateSpaceHelper = get(),
            permissionManager = get()
        )
    }

    viewModel { SettingsViewModel(application = androidApplication(), settingsRepository = get()) }
}
