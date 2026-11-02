package com.financemanage.mobile.core.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * 6-digit code input drawn as separate boxes; typing advances automatically (one hidden text field).
 * [onComplete] fires when all digits are entered.
 */
@Composable
fun OtpField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    isError: Boolean = false,
    onComplete: (String) -> Unit = {},
) {
    BasicTextField(
        value = value,
        onValueChange = { text ->
            val digits = text.filter { it.isDigit() }.take(length)
            onValueChange(digits)
            if (digits.length == length) onComplete(digits)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = modifier,
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(length) { index ->
                    val char = value.getOrNull(index)?.toString().orEmpty()
                    val focused = index == value.length
                    val color =
                        when {
                            isError -> MaterialTheme.colorScheme.error
                            focused -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outline
                        }
                    Box(
                        modifier =
                            Modifier
                                .size(
                                    44.dp,
                                    52.dp,
                                ).border(if (focused) 2.dp else 1.dp, color, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(char, style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        },
    )
}
