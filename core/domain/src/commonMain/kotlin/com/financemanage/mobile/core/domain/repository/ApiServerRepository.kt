package com.financemanage.mobile.core.domain.repository

import com.financemanage.mobile.core.domain.model.ApiServer
import kotlinx.coroutines.flow.Flow

/** Which backend the app talks to; debug builds can change it at runtime (kept across restarts). */
interface ApiServerRepository {
    val server: Flow<ApiServer>

    /** Uses [baseUrl] (scheme + host + port) from now on, or the build's default when `null`. */
    suspend fun setBaseUrl(baseUrl: String?)
}
