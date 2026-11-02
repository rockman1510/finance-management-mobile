package com.financemanage.mobile.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.material.icons.outlined.ArrowCircleUp
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.financemanage.mobile.core.navigation.QuickAddKind
import com.financemanage.mobile.core.ui.resources.Res
import com.financemanage.mobile.core.ui.resources.add_expense
import com.financemanage.mobile.core.ui.resources.add_income
import com.financemanage.mobile.core.ui.resources.add_sheet_title
import com.financemanage.mobile.core.ui.resources.add_transfer
import com.financemanage.mobile.core.ui.theme.FinanceTheme
import com.financemanage.mobile.core.ui.theme.Spacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The ➕ bottom sheet: Expense, Income, Transfer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSheet(
    onDismiss: () -> Unit,
    /** `EXPENSE` / `INCOME`, or `null` for a transfer. */
    onSelect: (String?) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = Spacing.lg)) {
            Text(
                text = stringResource(Res.string.add_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            )
            AddOption.entries.forEach { option ->
                ListItem(
                    headlineContent = { Text(stringResource(option.label)) },
                    leadingContent = { Icon(option.icon, contentDescription = null, tint = option.tint()) },
                    modifier = Modifier.clickable { onSelect(option.kind) },
                )
            }
        }
    }
}

private enum class AddOption(
    val kind: String?,
) {
    EXPENSE(QuickAddKind.EXPENSE),
    INCOME(QuickAddKind.INCOME),
    TRANSFER(null),
}

private val AddOption.label: StringResource
    get() =
        when (this) {
            AddOption.EXPENSE -> Res.string.add_expense
            AddOption.INCOME -> Res.string.add_income
            AddOption.TRANSFER -> Res.string.add_transfer
        }

private val AddOption.icon: ImageVector
    get() =
        when (this) {
            AddOption.EXPENSE -> Icons.Outlined.ArrowCircleUp
            AddOption.INCOME -> Icons.Outlined.ArrowCircleDown
            AddOption.TRANSFER -> Icons.Outlined.SwapHoriz
        }

@Composable
private fun AddOption.tint() =
    when (this) {
        AddOption.EXPENSE -> FinanceTheme.colors.expense
        AddOption.INCOME -> FinanceTheme.colors.income
        AddOption.TRANSFER -> MaterialTheme.colorScheme.primary
    }
