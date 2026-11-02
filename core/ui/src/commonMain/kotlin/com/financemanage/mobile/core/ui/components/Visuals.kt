package com.financemanage.mobile.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.financemanage.mobile.core.ui.theme.Spacing

/** Progress bar for budgets/goals. [percent] is 0–100 (more is clamped visually). */
@Composable
fun FinanceProgressBar(
    percent: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    LinearProgressIndicator(
        progress = { (percent.coerceIn(0, 100)) / 100f },
        modifier = modifier.fillMaxWidth().height(8.dp).semantics { contentDescription = "$percent%" },
        color = color,
        trackColor = color.copy(alpha = 0.2f),
        strokeCap = StrokeCap.Round,
        drawStopIndicator = {},
    )
}

/** Ring showing a percentage (saving goals). */
@Composable
fun ProgressRing(
    percent: Int,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    androidx.compose.foundation.layout.Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = 6.dp.toPx()
            drawArc(color.copy(alpha = 0.2f), 0f, 360f, false, style = Stroke(stroke))
            drawArc(
                color,
                -90f,
                360f * percent.coerceIn(0, 100) / 100f,
                false,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Text("$percent%", style = MaterialTheme.typography.labelMedium)
    }
}

/** One slice of a [DonutChart]. `value` is a share (any positive scale). */
data class ChartSlice(
    val label: String,
    val value: Float,
    val color: Color,
)

val ChartPalette =
    listOf(
        Color(0xFF26A69A),
        Color(0xFFEF5350),
        Color(0xFF5C6BC0),
        Color(0xFFFFA726),
        Color(0xFFAB47BC),
        Color(0xFF29B6F6),
        Color(0xFF9CCC65),
        Color(0xFF8D6E63),
        Color(0xFFEC407A),
        Color(0xFF78909C),
    )

@Composable
fun DonutChart(
    slices: List<ChartSlice>,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
) {
    val total = slices.sumOf { it.value.toDouble() }.toFloat()
    Canvas(modifier.size(size)) {
        if (total <= 0f) return@Canvas
        val stroke = 28.dp.toPx()
        val inset = stroke / 2
        var start = -90f
        slices.forEach { slice ->
            val sweep = 360f * slice.value / total
            drawArc(
                color = slice.color,
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(this.size.width - stroke, this.size.height - stroke),
                style = Stroke(stroke),
            )
            start += sweep
        }
    }
}

/** A group of bars for one period (e.g. income / expense of a month). */
data class BarGroup(
    val label: String,
    val values: List<Float>,
)

/** Grouped bar chart; negative values are drawn below the baseline. */
@Composable
fun BarChart(
    groups: List<BarGroup>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
) {
    val maxValue = groups.flatMap { it.values }.maxOfOrNull { kotlin.math.abs(it) }?.takeIf { it > 0f } ?: 1f
    val hasNegative = groups.any { group -> group.values.any { it < 0f } }
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            if (groups.isEmpty()) return@Canvas
            val groupWidth = size.width / groups.size
            val barCount = groups.maxOf { it.values.size }.coerceAtLeast(1)
            val barWidth = (groupWidth * 0.7f) / barCount
            val baseline = if (hasNegative) size.height / 2 else size.height
            val scale = (if (hasNegative) size.height / 2 else size.height) / maxValue
            groups.forEachIndexed { groupIndex, group ->
                group.values.forEachIndexed { barIndex, value ->
                    val barHeight = kotlin.math.abs(value) * scale
                    val left = groupIndex * groupWidth + groupWidth * 0.15f + barIndex * barWidth
                    val top = if (value >= 0f) baseline - barHeight else baseline
                    drawRoundRect(
                        color = colors[barIndex % colors.size],
                        topLeft = Offset(left, top),
                        size = Size(barWidth * 0.9f, barHeight),
                        cornerRadius = CornerRadius(4f, 4f),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            groups.forEach { group ->
                Text(
                    text = group.label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Legend row: colour dot + label + trailing text. */
@Composable
fun LegendRow(
    color: Color,
    label: String,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Canvas(Modifier.size(12.dp)) { drawCircle(color) }
        Text(label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        trailing()
    }
}

/** "Label ........ value" row used in detail screens. */
@Composable
fun InfoRow(
    label: String,
    modifier: Modifier = Modifier,
    value: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        value()
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    InfoRow(label, modifier) { Text(value, style = MaterialTheme.typography.bodyMedium) }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}
