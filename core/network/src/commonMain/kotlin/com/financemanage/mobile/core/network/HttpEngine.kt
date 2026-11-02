package com.financemanage.mobile.core.network

import io.ktor.client.engine.HttpClientEngine

/** OkHttp on Android, Darwin (NSURLSession) on iOS. */
expect fun createPlatformHttpEngine(): HttpClientEngine
