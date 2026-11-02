package com.financemanage.mobile.core.network.di

import com.financemanage.mobile.core.network.ApiConfig
import com.financemanage.mobile.core.network.NetworkJson
import com.financemanage.mobile.core.network.auth.SessionEvents
import com.financemanage.mobile.core.network.createHttpClient
import com.financemanage.mobile.core.network.createPlatformHttpEngine
import com.financemanage.mobile.core.network.pending.PendingPoller
import org.koin.core.module.Module
import org.koin.dsl.module

/** Requires a `TokenStore` binding (provided by `core:data`). */
fun networkModule(config: ApiConfig): Module =
    module {
        single { config }
        single { NetworkJson }
        single { SessionEvents() }
        single { PendingPoller() }
        single {
            createHttpClient(
                engine = createPlatformHttpEngine(),
                config = get(),
                tokenStore = get(),
                sessionEvents = get(),
                json = get(),
            )
        }
    }
