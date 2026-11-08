package com.financemanage.mobile.core.data.server

import com.financemanage.mobile.core.domain.model.ApiServer
import com.financemanage.mobile.core.domain.repository.ApiServerRepository
import com.financemanage.mobile.core.domain.repository.AppSettingsRepository
import com.financemanage.mobile.core.network.ApiEndpoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Backend address: the build's default, or – debug builds only – an override kept in the app settings. Release builds
 * ([ApiEndpoint.canOverride] false) ignore any stored override.
 */
class ApiServerRepositoryImpl(
    private val endpoint: ApiEndpoint,
    private val settings: AppSettingsRepository,
    private val scope: CoroutineScope,
) : ApiServerRepository {
    override val server: Flow<ApiServer> =
        endpoint.baseUrl.map { ApiServer(it, endpoint.defaultBaseUrl, endpoint.canOverride) }

    /** Applies the stored override at start-up. */
    fun start() {
        if (!endpoint.canOverride) return
        scope.launch {
            settings.preferences
                .first()
                .apiBaseUrlOverride
                ?.let(endpoint::override)
        }
    }

    override suspend fun setBaseUrl(baseUrl: String?) {
        if (!endpoint.canOverride) return
        val override = baseUrl?.trimEnd('/')?.takeIf { it != endpoint.defaultBaseUrl }
        settings.setApiBaseUrlOverride(override)
        endpoint.override(override)
    }
}
