package com.janreins.habitude.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.janreins.habitude.domain.WeekStat
import java.time.format.DateTimeFormatter

/**
 * One bar per week on a single zero-based axis. Gridlines are recessive; the bar you tap is
 * described in the caption underneath, and the rest fade back a little.
 */
@Composable
fun WeeklyBars(
    weeks: List<WeekStat>,
    color: Color,
    maxValue: Float,
    ticks: List<Float>,
    tickLabel: (Float) -> String,
    describe: (WeekStat) -> String,
    modifier: Modifier = Modifier,
) {
    var selected by remember(weeks) { mutableStateOf<Int?>(null) }
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val grid = chartPalette().grid
    val gutter = 34.dp
    val footer = 18.dp
    val dateFormat = remember { DateTimeFormatter.ofPattern("d MMM") }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .pointerInput(weeks) {
                    detectTapGestures { tap ->
                        val slot = (size.width - gutter.toPx()) / weeks.size
                        val index = ((tap.x - gutter.toPx()) / slot).toInt()
                        if (index in weeks.indices && weeks[index].value != null) {
                            selected = if (selected == index) null else index
                        }
                    }
                },
        ) {
            val plotTop = 6.dp.toPx()
            val plotBottom = size.height - footer.toPx()
            val plotHeight = plotBottom - plotTop
            val slot = (size.width - gutter.toPx()) / weeks.size
            val barWidth = (slot * 0.62f).coerceAtMost(18.dp.toPx())
            val radius = CornerRadius(4.dp.toPx())
            fun y(value: Float) = plotBottom - plotHeight * (value / maxValue).coerceIn(0f, 1f)

            ticks.forEach { tick ->
                val ty = y(tick)
                drawLine(grid, Offset(gutter.toPx(), ty), Offset(size.width, ty), strokeWidth = 1.dp.toPx())
                val layout = measurer.measure(tickLabel(tick), labelStyle)
                drawText(layout, topLeft = Offset(0f, ty - layout.size.height / 2))
            }

            weeks.forEachIndexed { index, week ->
                val centerX = gutter.toPx() + slot * index + slot / 2
                if (index % 3 == (weeks.size - 1) % 3) {
                    val layout = measurer.measure(week.weekStart.format(dateFormat), labelStyle)
                    drawText(
                        layout,
                        topLeft = Offset(
                            (centerX - layout.size.width / 2).coerceIn(gutter.toPx(), size.width - layout.size.width),
                            plotBottom + 4.dp.toPx(),
                        ),
                    )
                }
                val value = week.value ?: return@forEachIndexed
                val top = y(value).coerceAtMost(plotBottom - 2.dp.toPx())
                val faded = selected != null && selected != index
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = centerX - barWidth / 2,
                            top = top,
                            right = centerX + barWidth / 2,
                            bottom = plotBottom,
                            topLeftCornerRadius = radius,
                            topRightCornerRadius = radius,
                        ),
                    )
                }
                drawPath(path, if (faded) color.copy(alpha = 0.35f) else color)
            }
        }
        Text(
            selected?.let { describe(weeks[it]) } ?: "Tap a week to see it",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
