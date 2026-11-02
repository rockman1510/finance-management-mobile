package com.financemanage.mobile.core.data.di

import com.financemanage.mobile.core.data.database.FinanceDatabase
import com.financemanage.mobile.core.data.security.SecureTokenStore
import com.financemanage.mobile.core.data.settings.AppSettingsRepository
import com.financemanage.mobile.core.network.auth.TokenStore
import org.koin.core.module.Module
import org.koin.dsl.module

/** Platform bindings: `KVault`, `DataStore<Preferences>`, `FinanceDatabase`, `NetworkMonitor`. */
expect val dataPlatformModule: Module

val dataModule: Module =
    module {
        single<TokenStore> { SecureTokenStore(vault = get(), json = get()) }
        single { AppSettingsRepository(get()) }
        single { get<FinanceDatabase>().syncMetadataDao() }
    }
