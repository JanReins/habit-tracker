package com.janreins.habitude.ui.charts

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Chart colours. The build/break pair is a little more saturated than the UI theme so marks
 * stay distinct for colour-blind readers (checked with the dataviz palette validator: light
 * ΔE 8.3, dark ΔE 7.6 under protanopia). Slips also carry a dot so they never rely on colour.
 * The ramp is sequential, one hue, light to dark, and flips its anchor in dark mode.
 */
@Immutable
data class ChartPalette(
    val build: Color,
    val breakHabit: Color,
    /** A scheduled day that was missed: a quiet fill, not an alarm. */
    val missed: Color,
    /** Rest days and "nothing to show" cells. */
    val faint: Color,
    val grid: Color,
    val ramp: List<Color>,
)

private val Light = ChartPalette(
    build = Color(0xFF1F6E45),
    breakHabit = Color(0xFFCC6E3C),
    missed = Color(0xFFE2D7C7),
    faint = Color(0xFFEDE5D8),
    grid = Color(0x33000000),
    ramp = listOf(
        Color(0xFFE3EDE5),
        Color(0xFFB9D4C0),
        Color(0xFF84B595),
        Color(0xFF4E9068),
        Color(0xFF1F6E45),
    ),
)

private val Dark = ChartPalette(
    build = Color(0xFF21794C),
    breakHabit = Color(0xFFD97646),
    missed = Color(0xFF3D3833),
    faint = Color(0xFF332F2B),
    grid = Color(0x33FFFFFF),
    ramp = listOf(
        Color(0xFF33423A),
        Color(0xFF2F5E44),
        Color(0xFF2E7A52),
        Color(0xFF4E9C6E),
        Color(0xFF7DBF95),
    ),
)

@Composable
fun chartPalette(): ChartPalette =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Dark else Light

/** Buckets a 0..1 share into the sequential ramp. */
fun ChartPalette.rampColor(share: Float): Color = when {
    share <= 0f -> ramp[0]
    share < 0.34f -> ramp[1]
    share < 0.67f -> ramp[2]
    share < 1f -> ramp[3]
    else -> ramp[4]
}
