package com.financemanage.mobile.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.financemanage.mobile.core.ui.components.EmptyState
import com.financemanage.mobile.core.ui.resources.Res
import com.financemanage.mobile.core.ui.resources.coming_soon_message
import com.financemanage.mobile.core.ui.resources.coming_soon_title
import org.jetbrains.compose.resources.stringResource

/** Placeholder content until the corresponding feature module (F02–F22) is implemented. */
@Composable
fun ComingSoon(modifier: Modifier = Modifier) {
    EmptyState(
        title = stringResource(Res.string.coming_soon_title),
        message = stringResource(Res.string.coming_soon_message),
        icon = Icons.Outlined.Construction,
        modifier = modifier,
    )
}
