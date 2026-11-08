package com.financemanage.mobile.core.network

/**
 * @param baseUrl scheme + host + port, e.g. `http://10.0.2.2:8080` (Android emulator) or `https://<DOMAIN>`.
 * @param enableLogging log requests (headers only, `Authorization` redacted) – debug builds only.
 * @param allowBaseUrlOverride let a developer point the app at another backend at runtime (hidden button on the
 *   welcome screen) – debug builds only; release builds always use [baseUrl].
 */
data class ApiConfig(
    val baseUrl: String,
    val enableLogging: Boolean = false,
    val allowBaseUrlOverride: Boolean = false,
)

/** Prefix of every business endpoint, relative to [ApiConfig.baseUrl]. Use relative paths: `client.get(api("me"))`. */
const val API_PREFIX = "api/v1/"

/** `api("wallets")` -> `"api/v1/wallets"`. */
fun api(path: String): String = API_PREFIX + path.removePrefix("/")
