package com.financemanage.mobile.core.data.cache

import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

/**
 * What a screen needs to render a resource: the (cached) data, whether a refresh is running and the last error.
 * - `data == null && loading` → skeleton
 * - `data == null && error != null` → error state with retry
 * - `data != null` → content (+ a snackbar/banner when `error != null`)
 */
data class Resource<out T>(
    val data: T? = null,
    val loading: Boolean = false,
    val error: AppError? = null,
    val updatedAt: Instant? = null,
) {
    fun <R> map(transform: (T) -> R): Resource<R> = Resource(data?.let(transform), loading, error, updatedAt)
}

/**
 * Glues a cached source (Room) and a refresh call for a ViewModel. Call [refresh] on start, on pull to refresh
 * and when the screen comes back to the foreground.
 */
class ResourceHolder<T>(
    private val scope: CoroutineScope,
    source: Flow<Cached<T>?>,
    private val refresher: suspend () -> AppResult<*>,
) {
    private val refreshing = MutableStateFlow(false)
    private val error = MutableStateFlow<AppError?>(null)
    private var everLoaded = false

    val state: StateFlow<Resource<T>> =
        combine(source, refreshing, error) { cached, isRefreshing, lastError ->
            Resource(cached?.value, isRefreshing || (!everLoaded && cached == null), lastError, cached?.updatedAt)
        }.stateIn(scope, SharingStarted.Eagerly, Resource(loading = true))

    fun refresh() {
        if (refreshing.value) return
        scope.launch {
            refreshing.value = true
            val result = refresher()
            error.value = (result as? AppResult.Failure)?.error
            everLoaded = true
            refreshing.value = false
        }
    }

    fun clearError() {
        error.value = null
    }
}

/** A [ResourceHolder] for data that is not cached (details fetched on demand). */
fun <T> CoroutineScope.memoryResource(fetch: suspend () -> AppResult<T>): ResourceHolder<T> {
    val memory = MutableStateFlow<Cached<T>?>(null)
    return ResourceHolder(this, memory.map { it }) {
        val result = fetch()
        if (result is AppResult.Success) memory.value = Cached(result.value, kotlinx.datetime.Clock.System.now())
        result
    }
}
