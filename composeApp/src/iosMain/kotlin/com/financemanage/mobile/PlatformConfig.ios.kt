package com.financemanage.mobile

import com.financemanage.mobile.core.network.ApiConfig
import kotlin.experimental.ExperimentalNativeApi

/** Debug builds talk to the backend on the Mac running the simulator (API §1). */
@OptIn(ExperimentalNativeApi::class)
actual fun platformApiConfig(): ApiConfig =
    if (Platform.isDebugBinary) {
        ApiConfig(baseUrl = "http://localhost:8080", enableLogging = true, allowBaseUrlOverride = true)
    } else {
        ApiConfig(baseUrl = PRODUCTION_API_BASE_URL, enableLogging = false)
    }

actual fun platformGoogleWebClientId(): String? = null
