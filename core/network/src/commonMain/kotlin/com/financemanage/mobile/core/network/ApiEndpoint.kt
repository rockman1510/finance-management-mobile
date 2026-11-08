package com.financemanage.mobile.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The backend address every request goes to: [ApiConfig.baseUrl], or – in debug builds only – an address a developer
 * set at runtime (e.g. the LAN IP of the machine running the backend, for a real device). The [HttpClient][io.ktor.client.HttpClient]
 * reads [baseUrl] for each request, so a change applies at once.
 */
class ApiEndpoint(
    private val config: ApiConfig,
) {
    private val current = MutableStateFlow(config.baseUrl.trimEnd('/'))

    /** Scheme + host + port, without a trailing slash. */
    val baseUrl: StateFlow<String> = current.asStateFlow()

    val defaultBaseUrl: String get() = config.baseUrl.trimEnd('/')

    /** Whether [override] has any effect (debug builds). */
    val canOverride: Boolean get() = config.allowBaseUrlOverride

    /** Uses [url] from now on, or the default when `null`. Ignored when [canOverride] is false. */
    fun override(url: String?) {
        if (!canOverride) return
        current.value = url?.trimEnd('/') ?: defaultBaseUrl
    }
}
