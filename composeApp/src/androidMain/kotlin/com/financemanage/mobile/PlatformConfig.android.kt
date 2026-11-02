package com.financemanage.mobile

import com.financemanage.mobile.core.network.ApiConfig

actual fun platformApiConfig(): ApiConfig =
    ApiConfig(baseUrl = BuildConfig.API_BASE_URL, enableLogging = BuildConfig.DEBUG)
