package com.financemanage.mobile

import com.financemanage.mobile.core.network.ApiConfig

/** HTTPS production backend used by iOS release builds (Android reads `financemanage.apiBaseUrl.release`). */
const val PRODUCTION_API_BASE_URL = "https://api.financemanage.example"

/** Base URL per platform and build type. */
expect fun platformApiConfig(): ApiConfig

/** OAuth web client id for Google sign-in, or `null` when Google sign-in is not offered (iOS). */
expect fun platformGoogleWebClientId(): String?
