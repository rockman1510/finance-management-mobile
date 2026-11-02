package com.financemanage.mobile.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.material.icons.outlined.ArrowCircleUp
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.financemanage.mobile.core.ui.resources.Res
import com.financemanage.mobile.core.ui.resources.action_back
import com.financemanage.mobile.core.ui.resources.add_expense
import com.financemanage.mobile.core.ui.resources.add_income
import com.financemanage.mobile.core.ui.resources.add_sheet_title
import com.financemanage.mobile.core.ui.resources.add_transfer
import com.financemanage.mobile.core.ui.theme.FinanceTheme
import com.financemanage.mobile.core.ui.theme.Spacing
import com.financemanage.mobile.navigation.ComingSoon
import com.financemanage.mobile.navigation.QuickAddKind
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The ➕ bottom sheet: Expense, Income, Transfer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSheet(
    onDismiss: () -> Unit,
    onSelect: (QuickAddKind) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = Spacing.lg)) {
            Text(
                text = stringResource(Res.string.add_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            )
            QuickAddKind.entries.forEach { kind ->
                ListItem(
                    headlineContent = { Text(stringResource(kind.label)) },
                    leadingContent = { Icon(kind.icon, contentDescription = null, tint = kind.tint()) },
                    modifier = Modifier.clickable { onSelect(kind) },
                )
            }
        }
    }
}

/** Temporary destination until Quick add (F07) and Transfer (F08) are implemented. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddPlaceholderScreen(
    kind: QuickAddKind,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(kind.label)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        ComingSoon(Modifier.fillMaxSize().padding(padding))
    }
}

private val QuickAddKind.label: StringResource
    get() =
        when (this) {
            QuickAddKind.EXPENSE -> Res.string.add_expense
            QuickAddKind.INCOME -> Res.string.add_income
            QuickAddKind.TRANSFER -> Res.string.add_transfer
        }

private val QuickAddKind.icon: ImageVector
    get() =
        when (this) {
            QuickAddKind.EXPENSE -> Icons.Outlined.ArrowCircleUp
            QuickAddKind.INCOME -> Icons.Outlined.ArrowCircleDown
            QuickAddKind.TRANSFER -> Icons.Outlined.SwapHoriz
        }

@Composable
private fun QuickAddKind.tint() =
    when (this) {
        QuickAddKind.EXPENSE -> FinanceTheme.colors.expense
        QuickAddKind.INCOME -> FinanceTheme.colors.income
        QuickAddKind.TRANSFER -> MaterialTheme.colorScheme.primary
    }
