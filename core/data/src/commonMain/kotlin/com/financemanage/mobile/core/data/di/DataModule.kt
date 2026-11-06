package com.financemanage.mobile.core.data.di

import com.financemanage.mobile.core.data.cache.JsonCache
import com.financemanage.mobile.core.data.database.FinanceDatabase
import com.financemanage.mobile.core.data.outbox.OutboxRepositoryImpl
import com.financemanage.mobile.core.data.outbox.OutboxSender
import com.financemanage.mobile.core.data.outbox.OutboxSync
import com.financemanage.mobile.core.data.push.PushBridge
import com.financemanage.mobile.core.data.push.PushRegistrar
import com.financemanage.mobile.core.data.push.PushRepositoryImpl
import com.financemanage.mobile.core.data.security.SecureTokenStore
import com.financemanage.mobile.core.data.session.SessionManager
import com.financemanage.mobile.core.data.session.SignOutHook
import com.financemanage.mobile.core.data.settings.AppSettingsRepositoryImpl
import com.financemanage.mobile.core.data.shared.CategoryRepositoryImpl
import com.financemanage.mobile.core.data.shared.ExchangeRateRepositoryImpl
import com.financemanage.mobile.core.data.shared.ProfileRepositoryImpl
import com.financemanage.mobile.core.data.shared.WalletRepositoryImpl
import com.financemanage.mobile.core.domain.repository.AppSettingsRepository
import com.financemanage.mobile.core.domain.repository.CategoryRepository
import com.financemanage.mobile.core.domain.repository.ExchangeRateRepository
import com.financemanage.mobile.core.domain.repository.OutboxRepository
import com.financemanage.mobile.core.domain.repository.ProfileRepository
import com.financemanage.mobile.core.domain.repository.PushRepository
import com.financemanage.mobile.core.domain.repository.SessionRepository
import com.financemanage.mobile.core.domain.repository.WalletRepository
import com.financemanage.mobile.core.network.auth.TokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

/** Platform bindings: `KVault`, `DataStore<Preferences>`, `FinanceDatabase`, `NetworkMonitor`. */
expect val dataPlatformModule: Module

/** Qualifier of the application-wide [CoroutineScope] (outlives screens). */
val AppScope = named("AppScope")

/** Data layer: implementations of the shared repositories of `core:domain` and app-wide services. */
val dataModule: Module =
    module {
        single(AppScope) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
        single<TokenStore> { SecureTokenStore(vault = get(), json = get()) }
        single<AppSettingsRepository> { AppSettingsRepositoryImpl(get()) }
        single { get<FinanceDatabase>().syncMetadataDao() }
        single { get<FinanceDatabase>().jsonCacheDao() }
        single { get<FinanceDatabase>().outboxDao() }
        single { JsonCache(get(), get()) }
        single {
            SessionManager(
                tokenStore = get(),
                httpClient = get(),
                cache = get(),
                outboxDao = get(),
                settings = get(),
                hooks = { getKoin().getAll<SignOutHook>() },
                sessionEvents = get(),
                scope = get(AppScope),
            )
        } bind SessionRepository::class
        single {
            OutboxSync(
                dao = get(),
                senders = { getKoin().getAll<OutboxSender>() },
                networkMonitor = get(),
                scope = get(AppScope),
            )
        }
        single<OutboxRepository> { OutboxRepositoryImpl(get(), get()) }
        single<PushRepository> { PushRepositoryImpl() }
        single {
            PushRegistrar(
                client = get(),
                sessionState = get<SessionManager>().state,
                tokens = PushBridge.token,
                scope = get(AppScope),
            )
        } bind SignOutHook::class
        single<ProfileRepository> { ProfileRepositoryImpl(get(), get()) }
        single<WalletRepository> { WalletRepositoryImpl(get(), get()) }
        single<CategoryRepository> { CategoryRepositoryImpl(get(), get()) }
        single<ExchangeRateRepository> { ExchangeRateRepositoryImpl(get(), get()) }
    }
