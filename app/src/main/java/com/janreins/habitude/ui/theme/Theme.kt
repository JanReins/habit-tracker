package com.janreins.habitude.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

internal val LightColors = lightColorScheme(
    primary = SageDeep,
    onPrimary = Cream,
    primaryContainer = SageLight,
    onPrimaryContainer = SageDeep,
    secondary = ClayDeep,
    onSecondary = Cream,
    secondaryContainer = ClayLight,
    onSecondaryContainer = ClayDeep,
    tertiary = Honey,
    background = Cream,
    onBackground = Ink,
    surface = Cream,
    onSurface = Ink,
    surfaceVariant = Oat,
    onSurfaceVariant = Pebble,
    surfaceContainer = Oat,
    outline = Mist,
)

internal val DarkColors = darkColorScheme(
    primary = SageNight,
    onPrimary = Ink,
    primaryContainer = SageDeep,
    onPrimaryContainer = SageLight,
    secondary = ClayNight,
    onSecondary = Ink,
    secondaryContainer = ClayDeep,
    onSecondaryContainer = ClayLight,
    tertiary = Honey,
    background = Ink,
    onBackground = Cream,
    surface = Ink,
    onSurface = Cream,
    surfaceVariant = Charcoal,
    onSurfaceVariant = Mist,
    surfaceContainer = Charcoal,
    outline = Pebble,
)

private val HabitudeShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun HabitudeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = HabitudeTypography,
        shapes = HabitudeShapes,
        content = content,
    )
}
