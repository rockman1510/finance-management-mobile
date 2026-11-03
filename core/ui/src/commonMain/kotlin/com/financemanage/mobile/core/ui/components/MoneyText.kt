package com.financemanage.mobile.core.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.format.MoneyFormatter
import com.financemanage.mobile.core.ui.resources.Res
import com.financemanage.mobile.core.ui.resources.money_hidden_description
import com.financemanage.mobile.core.ui.theme.FinanceTheme
import com.financemanage.mobile.core.ui.theme.LocalHideBalances
import org.jetbrains.compose.resources.stringResource

/**
 * Displays an amount with the `vi-VN` format (`1.250.000 ₫`). Respects the "hide balances" setting.
 *
 * @param colorBySign negative amounts in the expense colour, positive ones in the income colour.
 * @param color explicit colour, used when [colorBySign] is false.
 */
@Composable
fun MoneyText(
    money: Money,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    colorBySign: Boolean = false,
    showPlusSign: Boolean = false,
    hidden: Boolean = LocalHideBalances.current,
    color: Color = Color.Unspecified,
) {
    val colors = FinanceTheme.colors
    val textColor =
        when {
            !colorBySign -> color
            money.isNegative -> colors.expense
            money.isPositive -> colors.income
            else -> Color.Unspecified
        }
    val hiddenDescription = stringResource(Res.string.money_hidden_description)
    Text(
        text = MoneyFormatter.format(money, hidden = hidden, showPlusSign = showPlusSign),
        modifier = if (hidden) modifier.semantics { contentDescription = hiddenDescription } else modifier,
        style = style,
        color = textColor,
        maxLines = 1,
    )
}
