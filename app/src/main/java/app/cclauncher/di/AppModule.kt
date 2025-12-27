package app.cclauncher.di

import android.appwidget.AppWidgetHost
import app.cclauncher.MainViewModel
import app.cclauncher.data.WidgetConstants
import app.cclauncher.data.repository.AppRepository
import app.cclauncher.settings.AppSettingsRepository
import app.cclauncher.ui.components.snackbar.SnackbarManager
import app.cclauncher.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single { AppSettingsRepository(androidContext()) }

    single { AppWidgetHost(androidContext(), WidgetConstants.APPWIDGET_HOST_ID) }

    single {
        AppRepository(
            context = androidContext(),
            settingsRepository = get(),
            coroutineScope = get<CoroutineScope>()
        )
    }

    single { SnackbarManager() }

    viewModel { MainViewModel(get(), get()) }

    viewModel { SettingsViewModel(get()) }
}