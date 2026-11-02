package com.financemanage.mobile.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.format.MoneyFormatter
import com.financemanage.mobile.core.model.hasAtMostFractionDigits
import com.financemanage.mobile.core.model.toBigDecimalOrNull
import com.financemanage.mobile.core.ui.theme.Spacing
import com.ionspin.kotlin.bignum.decimal.BigDecimal

/** Text field with an error message under it (`fieldErrors` from the backend or local validation). */
@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    enabled: Boolean = true,
    supportingText: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    maxLength: Int? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onValueChange(if (maxLength != null) text.take(maxLength) else text) },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        isError = error != null,
        singleLine = singleLine,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions.Default,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        supportingText = (error ?: supportingText)?.let { text -> { Text(text) } },
    )
}

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    supportingText: String? = null,
    imeAction: ImeAction = ImeAction.Done,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    FormTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        error = error,
        supportingText = supportingText,
        keyboardType = KeyboardType.Password,
        imeAction = imeAction,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = null,
                )
            }
        },
    )
}

/**
 * Amount entry with the currency's decimals. The raw text uses `.` or `,` as decimal separator and is parsed with
 * [parseAmountInput]; the field shows the currency symbol.
 */
@Composable
fun AmountTextField(
    value: String,
    onValueChange: (String) -> Unit,
    currency: CurrencyCode,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    enabled: Boolean = true,
    allowNegative: Boolean = false,
) {
    FormTextField(
        value = value,
        onValueChange = { text ->
            val allowed = text.filter { it.isDigit() || it == '.' || it == ',' || (allowNegative && it == '-') }
            onValueChange(allowed)
        },
        label = label,
        modifier = modifier,
        error = error,
        enabled = enabled,
        keyboardType = KeyboardType.Decimal,
        trailingIcon = {
            Text(
                MoneyFormatter.symbol(currency),
                modifier = Modifier.size(width = 48.dp, height = 24.dp),
            )
        },
    )
}

/**
 * Parses what the user typed in an amount field: `1.250.000` (vi grouping), `1250000`, `12,5`, `12.50`.
 * Returns `null` when it is not a number.
 */
fun parseAmountInput(
    text: String,
    fractionDigits: Int,
): BigDecimal? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null
    val normalized =
        if (fractionDigits == 0) {
            trimmed.replace(".", "").replace(",", "")
        } else {
            // Last separator is the decimal one; others are grouping.
            val lastSeparator = maxOf(trimmed.lastIndexOf('.'), trimmed.lastIndexOf(','))
            if (lastSeparator < 0) {
                trimmed
            } else {
                trimmed.substring(0, lastSeparator).replace(".", "").replace(",", "") + "." +
                    trimmed.substring(lastSeparator + 1)
            }
        }
    return normalized.toBigDecimalOrNull()?.takeIf { it.hasAtMostFractionDigits(fractionDigits) }
}

/** Row with a label and a switch. */
@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (supportingText != null) {
                Text(
                    supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Primary button that shows a spinner and ignores taps while [loading]. */
@Composable
fun LoadingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(text)
            }
        }
    }
}

/** Space between form rows. */
val FormSpacing = Spacing.md
