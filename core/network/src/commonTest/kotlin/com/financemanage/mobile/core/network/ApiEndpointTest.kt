package com.financemanage.mobile.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.Url
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiEndpointTest {
    @Test
    fun anOverrideAppliesToTheNextRequestAndCanBeUndone() =
        runTest {
            val endpoint = ApiEndpoint(ApiConfig(baseUrl = "http://10.0.2.2:8080/", allowBaseUrlOverride = true))
            val urls = mutableListOf<Url>()
            val client =
                createHttpClient(
                    engine =
                        MockEngine { request ->
                            urls += request.url
                            respond("{}")
                        },
                    config = ApiConfig(baseUrl = "http://10.0.2.2:8080"),
                    tokenStore = InMemoryTokenStore(),
                    sessionEvents =
                        com.financemanage.mobile.core.network.auth
                            .SessionEvents(),
                    endpoint = endpoint,
                )

            client.get(api("auth/ping"))
            endpoint.override("http://192.168.1.20:8080/")
            client.get(api("auth/ping"))
            endpoint.override(null)
            client.get(api("auth/ping"))

            assertEquals(
                listOf(
                    "http://10.0.2.2:8080/api/v1/auth/ping",
                    "http://192.168.1.20:8080/api/v1/auth/ping",
                    "http://10.0.2.2:8080/api/v1/auth/ping",
                ),
                urls.map { it.toString() },
            )
        }

    @Test
    fun releaseBuildsIgnoreOverrides() {
        val endpoint = ApiEndpoint(ApiConfig(baseUrl = "https://api.example.com"))
        endpoint.override("http://192.168.1.20:8080")
        assertEquals("https://api.example.com", endpoint.baseUrl.value)
    }
}
