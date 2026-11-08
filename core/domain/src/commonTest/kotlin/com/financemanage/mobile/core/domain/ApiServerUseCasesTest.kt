package com.financemanage.mobile.core.domain

import com.financemanage.mobile.core.domain.model.ApiServer
import com.financemanage.mobile.core.domain.repository.ApiServerRepository
import com.financemanage.mobile.core.domain.usecase.ResetApiBaseUrlUseCase
import com.financemanage.mobile.core.domain.usecase.SetApiBaseUrlUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class RecordingServerRepository : ApiServerRepository {
    val calls = mutableListOf<String?>()
    override val server: Flow<ApiServer> = emptyFlow()

    override suspend fun setBaseUrl(baseUrl: String?) {
        calls += baseUrl
    }
}

class ApiServerUseCasesTest {
    @Test
    fun normalizeKeepsSchemeHostAndPortOnly() {
        assertEquals("http://192.168.1.20:8080", SetApiBaseUrlUseCase.normalize(" http://192.168.1.20:8080/ "))
        assertEquals("https://abc.trycloudflare.com", SetApiBaseUrlUseCase.normalize("HTTPS://ABC.trycloudflare.com"))
        assertEquals("http://localhost", SetApiBaseUrlUseCase.normalize("http://localhost"))
    }

    @Test
    fun normalizeRejectsWhatIsNotABaseUrl() {
        listOf(
            "",
            "192.168.1.20:8080",
            "ftp://host",
            "http://host:8080/api/v1",
            "http://host:0",
            "http://host:70000",
            "http://user@host",
            "http://host?x=1",
            "http://host name",
        ).forEach { assertNull(SetApiBaseUrlUseCase.normalize(it), it) }
    }

    @Test
    fun setSavesTheNormalizedAddressAndRejectsInvalidInput() =
        runTest {
            val repository = RecordingServerRepository()
            val set = SetApiBaseUrlUseCase(repository)

            assertFalse(set("http://host:8080/api"))
            assertTrue(repository.calls.isEmpty())

            assertTrue(set("http://192.168.1.20:8080/"))
            assertEquals(listOf<String?>("http://192.168.1.20:8080"), repository.calls)
        }

    @Test
    fun resetGoesBackToTheDefault() =
        runTest {
            val repository = RecordingServerRepository()
            ResetApiBaseUrlUseCase(repository)()
            assertEquals(listOf<String?>(null), repository.calls)
        }
}
