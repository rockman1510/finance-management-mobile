package com.financemanage.mobile

import com.financemanage.mobile.core.data.settings.AppSettingsRepositoryImpl
import com.financemanage.mobile.core.domain.model.SessionState
import com.financemanage.mobile.core.domain.model.ThemeMode
import com.financemanage.mobile.core.domain.repository.SessionRepository
import com.financemanage.mobile.core.domain.usecase.ObserveConnectivityUseCase
import com.financemanage.mobile.core.domain.usecase.ObservePreferencesUseCase
import com.financemanage.mobile.core.domain.usecase.ObserveSessionStateUseCase
import com.financemanage.mobile.core.testing.FakeNetworkMonitor
import com.financemanage.mobile.core.testing.InMemoryPreferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun reflectsSettingsAndConnectivity() =
        runTest {
            val settings = AppSettingsRepositoryImpl(InMemoryPreferencesDataStore())
            val network = FakeNetworkMonitor(online = true)
            val session =
                object : SessionRepository {
                    override val state = MutableStateFlow(SessionState.SIGNED_IN)

                    override suspend fun signOut() = Unit

                    override suspend fun clearLocalSession(wipeSettings: Boolean) = Unit
                }
            val viewModel =
                AppViewModel(
                    ObservePreferencesUseCase(settings),
                    ObserveConnectivityUseCase(network),
                    ObserveSessionStateUseCase(session),
                )
            val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

            settings.setThemeMode(ThemeMode.DARK)
            settings.setHideBalances(true)
            network.online.value = false

            val state = viewModel.uiState.first { !it.isOnline }
            assertEquals(ThemeMode.DARK, state.themeMode)
            assertEquals(true, state.hideBalances)
            assertFalse(state.isOnline)
            assertEquals(SessionState.SIGNED_IN, state.session)
            collector.cancel()
        }
}
