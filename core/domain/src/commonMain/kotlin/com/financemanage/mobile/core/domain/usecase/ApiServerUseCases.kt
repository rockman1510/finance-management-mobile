package com.financemanage.mobile.core.domain.usecase

import com.financemanage.mobile.core.domain.model.ApiServer
import com.financemanage.mobile.core.domain.repository.ApiServerRepository
import kotlinx.coroutines.flow.Flow

/** The backend the app talks to, and whether it can be changed (debug builds). */
class ObserveApiServerUseCase(
    private val repository: ApiServerRepository,
) {
    operator fun invoke(): Flow<ApiServer> = repository.server
}

/** Points the app at another backend (debug builds). */
class SetApiBaseUrlUseCase(
    private val repository: ApiServerRepository,
) {
    /** Returns `false` (and changes nothing) when [input] is not `http(s)://host[:port]`. */
    suspend operator fun invoke(input: String): Boolean {
        val baseUrl = normalize(input) ?: return false
        repository.setBaseUrl(baseUrl)
        return true
    }

    companion object {
        private val BASE_URL = Regex("""^(https?)://([A-Za-z0-9.-]+)(?::(\d{1,5}))?/?$""", RegexOption.IGNORE_CASE)
        private const val MAX_PORT = 65_535

        /**
         * `" HTTP://192.168.1.20:8080/ "` → `"http://192.168.1.20:8080"`; `null` when the input is not a scheme +
         * host (+ port) – no path, query or credentials, since the app adds `/api/v1/…` itself.
         */
        fun normalize(input: String): String? {
            val match = BASE_URL.matchEntire(input.trim()) ?: return null
            val (scheme, host, port) = match.destructured
            if (port.isNotEmpty() && port.toInt() !in 1..MAX_PORT) return null
            return buildString {
                append(scheme.lowercase()).append("://").append(host.lowercase())
                if (port.isNotEmpty()) append(':').append(port)
            }
        }
    }
}

/** Goes back to the build's default backend. */
class ResetApiBaseUrlUseCase(
    private val repository: ApiServerRepository,
) {
    suspend operator fun invoke() = repository.setBaseUrl(null)
}
