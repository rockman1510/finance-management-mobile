package com.financemanage.mobile.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.financemanage.mobile.core.ui.error.userMessage
import com.financemanage.mobile.core.ui.resources.Res
import com.financemanage.mobile.core.ui.resources.updated_at
import com.financemanage.mobile.core.ui.state.Resource
import com.financemanage.mobile.core.ui.theme.Spacing
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

/** Calls [onResume] every time the screen comes back to the foreground (and on first display). */
@Composable
fun RefreshOnResume(onResume: () -> Unit) {
    LifecycleResumeEffect(Unit) {
        onResume()
        onPauseOrDispose { }
    }
}

/**
 * Standard rendering of a [Resource]: skeleton while loading with no data, error state with retry, empty state,
 * content otherwise – all inside pull to refresh. A refresh error over cached data shows as a banner.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ResourceContent(
    resource: Resource<T>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    isEmpty: (T) -> Boolean = { false },
    empty: @Composable () -> Unit = {},
    loading: @Composable () -> Unit = { ListSkeleton() },
    content: @Composable (T) -> Unit,
) {
    val data = resource.data
    val error = resource.error
    PullToRefreshBox(
        isRefreshing = resource.loading && data != null,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            data == null && error != null -> ErrorState(error.userMessage(), onRetry = onRefresh)
            data == null -> loading()
            isEmpty(data) -> Box(Modifier.fillMaxSize()) { empty() }
            else ->
                Column(Modifier.fillMaxSize()) {
                    if (error != null) {
                        Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = error.userMessage(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                            )
                        }
                    }
                    Box(Modifier.weight(1f)) { content(data) }
                }
        }
    }
}

/** "Updated at HH:mm" line for cached data. */
@Composable
fun UpdatedAtText(
    updatedAt: Instant?,
    modifier: Modifier = Modifier,
) {
    if (updatedAt == null) return
    val time = updatedAt.toLocalDateTime(TimeZone.currentSystemDefault())
    val text = "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
    Text(
        text = stringResource(Res.string.updated_at, text),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
