package com.financemanage.mobile.core.data

import com.financemanage.mobile.core.data.server.ApiServerRepositoryImpl
import com.financemanage.mobile.core.data.settings.AppSettingsRepositoryImpl
import com.financemanage.mobile.core.network.ApiConfig
import com.financemanage.mobile.core.network.ApiEndpoint
import com.financemanage.mobile.core.testing.InMemoryPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiServerRepositoryTest {
    private val settings = AppSettingsRepositoryImpl(InMemoryPreferencesDataStore())
    private val debug = ApiConfig(baseUrl = "http://10.0.2.2:8080", allowBaseUrlOverride = true)

    @Test
    fun anOverrideIsAppliedSavedAndRestoredAfterARestart() =
        runTest {
            val endpoint = ApiEndpoint(debug)
            val repository = ApiServerRepositoryImpl(endpoint, settings, backgroundScope)
            assertTrue(repository.server.first().isDefault)

            repository.setBaseUrl("http://192.168.1.20:8080")
            assertEquals("http://192.168.1.20:8080", endpoint.baseUrl.value)
            val server = repository.server.first()
            assertFalse(server.isDefault)
            assertTrue(server.editable)
            assertEquals("http://10.0.2.2:8080", server.defaultBaseUrl)

            // A new process: the stored override is applied at start-up.
            val restarted = ApiEndpoint(debug)
            ApiServerRepositoryImpl(restarted, settings, this).start()
            testScheduler.advanceUntilIdle()
            assertEquals("http://192.168.1.20:8080", restarted.baseUrl.value)

            repository.setBaseUrl(null)
            assertEquals("http://10.0.2.2:8080", endpoint.baseUrl.value)
            assertNull(settings.preferences.first().apiBaseUrlOverride)
        }

    @Test
    fun releaseBuildsNeitherSaveNorApplyAnOverride() =
        runTest {
            settings.setApiBaseUrlOverride("http://192.168.1.20:8080")
            val endpoint = ApiEndpoint(ApiConfig(baseUrl = "https://api.example.com"))
            val repository = ApiServerRepositoryImpl(endpoint, settings, this)
            repository.start()
            repository.setBaseUrl("http://evil.example")
            testScheduler.advanceUntilIdle()

            assertEquals("https://api.example.com", endpoint.baseUrl.value)
            assertFalse(repository.server.first().editable)
        }
}
