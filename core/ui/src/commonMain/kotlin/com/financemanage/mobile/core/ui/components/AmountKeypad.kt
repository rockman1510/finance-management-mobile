package com.financemanage.mobile.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.financemanage.mobile.core.ui.resources.Res
import com.financemanage.mobile.core.ui.resources.keypad_backspace
import com.financemanage.mobile.core.ui.resources.keypad_decimal
import com.financemanage.mobile.core.ui.resources.keypad_triple_zero
import org.jetbrains.compose.resources.stringResource

private val KeyHeight = 56.dp
private val DigitRows = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))

/**
 * Numeric keypad for amounts: 0–9, `000` (or `,` for currencies with decimals) and backspace.
 * Feed the keys to [AmountInput.press].
 */
@Composable
fun AmountKeypad(
    onKey: (KeypadKey) -> Unit,
    modifier: Modifier = Modifier,
    allowDecimal: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DigitRows.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { digit ->
                    KeypadButton(label = digit.toString(), onClick = { onKey(KeypadKey.Digit(digit)) })
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            if (allowDecimal) {
                KeypadButton(
                    label = ",",
                    description = stringResource(Res.string.keypad_decimal),
                    onClick = { onKey(KeypadKey.Decimal) },
                )
            } else {
                KeypadButton(
                    label = "000",
                    description = stringResource(Res.string.keypad_triple_zero),
                    onClick = { onKey(KeypadKey.TripleZero) },
                )
            }
            KeypadButton(label = "0", onClick = { onKey(KeypadKey.Digit(0)) })
            TextButton(
                onClick = { onKey(KeypadKey.Backspace) },
                modifier = Modifier.weight(1f).height(KeyHeight),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = stringResource(Res.string.keypad_backspace),
                )
            }
        }
    }
}

@Composable
private fun RowScope.KeypadButton(
    label: String,
    onClick: () -> Unit,
    description: String? = null,
) {
    val semanticsModifier =
        if (description != null) {
            Modifier.semantics { contentDescription = description }
        } else {
            Modifier
        }
    TextButton(
        onClick = onClick,
        modifier = Modifier.weight(1f).height(KeyHeight).then(semanticsModifier),
    ) {
        Text(text = label, style = MaterialTheme.typography.headlineSmall)
    }
}
