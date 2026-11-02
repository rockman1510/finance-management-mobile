package com.financemanage.mobile.di

import com.financemanage.mobile.AppViewModel
import com.financemanage.mobile.core.data.di.dataModule
import com.financemanage.mobile.core.data.di.dataPlatformModule
import com.financemanage.mobile.core.network.ApiConfig
import com.financemanage.mobile.core.network.di.networkModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

private val appModule =
    module {
        viewModel { AppViewModel(settings = get(), networkMonitor = get()) }
    }

/** Every Koin module of the app. Feature modules add their own module here. */
fun appModules(config: ApiConfig): List<Module> =
    listOf(
        networkModule(config),
        dataPlatformModule,
        dataModule,
        appModule,
    )

fun initKoin(
    config: ApiConfig,
    platformDeclaration: KoinAppDeclaration = {},
) {
    startKoin {
        platformDeclaration()
        modules(appModules(config))
    }
}
