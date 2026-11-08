package com.financemanage.mobile.feature.auth

import com.financemanage.mobile.core.data.server.ApiServerRepositoryImpl
import com.financemanage.mobile.core.data.settings.AppSettingsRepositoryImpl
import com.financemanage.mobile.core.domain.usecase.ObserveApiServerUseCase
import com.financemanage.mobile.core.domain.usecase.ResetApiBaseUrlUseCase
import com.financemanage.mobile.core.domain.usecase.SetApiBaseUrlUseCase
import com.financemanage.mobile.core.network.ApiConfig
import com.financemanage.mobile.core.network.ApiEndpoint
import com.financemanage.mobile.core.testing.InMemoryPreferencesDataStore
import com.financemanage.mobile.core.testing.awaitValue
import com.financemanage.mobile.core.testing.runViewModelTest
import com.financemanage.mobile.feature.auth.presentation.ServerSettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServerSettingsViewModelTest {
    private fun viewModel(
        config: ApiConfig,
        scope: CoroutineScope,
    ): Pair<ServerSettingsViewModel, ApiEndpoint> {
        val endpoint = ApiEndpoint(config)
        val repository =
            ApiServerRepositoryImpl(endpoint, AppSettingsRepositoryImpl(InMemoryPreferencesDataStore()), scope)
        val viewModel =
            ServerSettingsViewModel(
                ObserveApiServerUseCase(repository),
                SetApiBaseUrlUseCase(repository),
                ResetApiBaseUrlUseCase(repository),
            )
        return viewModel to endpoint
    }

    @Test
    fun aLongPressEditsTheAddressValidatesItAndCanGoBackToTheDefault() =
        runViewModelTest {
            val (viewModel, endpoint) =
                viewModel(ApiConfig("http://10.0.2.2:8080", allowBaseUrlOverride = true), backgroundScope)
            viewModel.state.awaitValue { it.available }

            viewModel.open()
            assertEquals("http://10.0.2.2:8080", viewModel.state.value.input)

            viewModel.onInput("192.168.1.20:8080")
            viewModel.save()
            val invalid = viewModel.state.awaitValue { it.invalid }
            assertTrue(invalid.editing)
            assertEquals("http://10.0.2.2:8080", endpoint.baseUrl.value)

            viewModel.onInput("http://192.168.1.20:8080/")
            assertFalse(viewModel.state.value.invalid)
            viewModel.save()
            viewModel.state.awaitValue { !it.editing && it.server?.baseUrl == "http://192.168.1.20:8080" }
            assertEquals("http://192.168.1.20:8080", endpoint.baseUrl.value)

            viewModel.open()
            viewModel.resetToDefault()
            viewModel.state.awaitValue { !it.editing && it.server?.isDefault == true }
            assertEquals("http://10.0.2.2:8080", endpoint.baseUrl.value)
        }

    @Test
    fun nothingIsAvailableInReleaseBuilds() =
        runViewModelTest {
            val (viewModel, _) = viewModel(ApiConfig("https://api.example.com"), backgroundScope)
            viewModel.state.awaitValue { it.server != null }
            assertFalse(viewModel.state.value.available)
            viewModel.open()
            assertFalse(viewModel.state.value.editing)
        }
}
