package com.financemanage.mobile.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.financemanage.mobile.core.model.AsyncStatus
import com.financemanage.mobile.core.model.PollResult
import com.financemanage.mobile.core.ui.resources.Res
import com.financemanage.mobile.core.ui.resources.pending_failed
import com.financemanage.mobile.core.ui.resources.processing
import com.financemanage.mobile.core.ui.resources.saved
import com.financemanage.mobile.core.ui.resources.will_update_shortly
import com.financemanage.mobile.core.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** UI state of an asynchronous action (API §2.1). */
sealed interface PendingAction {
    data object Processing : PendingAction

    data object Recorded : PendingAction

    data class Failed(
        val reason: String?,
    ) : PendingAction

    /** Still PENDING after polling: refresh when the screen is shown again. */
    data object WillUpdate : PendingAction

    companion object {
        /** Maps a poll result on an object's [AsyncStatus]. */
        fun <T> from(
            result: PollResult<T>,
            status: (T) -> AsyncStatus,
            reason: (T) -> String?,
        ): PendingAction =
            when (result) {
                is PollResult.TimedOut -> WillUpdate
                is PollResult.Settled ->
                    if (status(result.value) == AsyncStatus.FAILED) Failed(reason(result.value)) else Recorded
            }
    }
}

/** Banner for a [PendingAction]: "Đang xử lý…", "Đã lưu", the failure reason or "Sẽ cập nhật trong giây lát". */
@Composable
fun PendingBanner(
    action: PendingAction?,
    modifier: Modifier = Modifier,
) {
    if (action == null) return
    val colors = MaterialTheme.colorScheme
    val (container, text) =
        when (action) {
            PendingAction.Processing -> colors.secondaryContainer to stringResource(Res.string.processing)
            PendingAction.Recorded -> colors.primaryContainer to stringResource(Res.string.saved)
            is PendingAction.Failed ->
                colors.errorContainer to
                    stringResource(Res.string.pending_failed, action.reason.orEmpty())
            PendingAction.WillUpdate -> colors.surfaceVariant to stringResource(Res.string.will_update_shortly)
        }
    Surface(color = container, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (action) {
                PendingAction.Processing -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                PendingAction.Recorded -> Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                is PendingAction.Failed -> Icon(Icons.Outlined.ErrorOutline, contentDescription = null)
                PendingAction.WillUpdate -> Icon(Icons.Outlined.Schedule, contentDescription = null)
            }
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
