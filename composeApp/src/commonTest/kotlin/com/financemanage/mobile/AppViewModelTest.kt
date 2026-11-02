package com.financemanage.mobile

import com.financemanage.mobile.core.data.settings.AppSettingsRepository
import com.financemanage.mobile.core.data.settings.ThemeMode
import com.financemanage.mobile.core.testing.FakeNetworkMonitor
import com.financemanage.mobile.core.testing.InMemoryPreferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
            val settings = AppSettingsRepository(InMemoryPreferencesDataStore())
            val network = FakeNetworkMonitor(online = true)
            val viewModel = AppViewModel(settings, network)
            val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

            settings.setThemeMode(ThemeMode.DARK)
            settings.setHideBalances(true)
            network.online.value = false

            val state = viewModel.uiState.first { !it.isOnline }
            assertEquals(ThemeMode.DARK, state.themeMode)
            assertEquals(true, state.hideBalances)
            assertFalse(state.isOnline)
            collector.cancel()
        }
}
