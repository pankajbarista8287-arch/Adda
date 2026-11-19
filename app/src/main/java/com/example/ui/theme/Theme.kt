package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AddaDarkColorScheme = darkColorScheme(
    primary = RoyalPurpleAccent,
    onPrimary = TextPrimary,
    primaryContainer = RoyalPurpleSurface,
    onPrimaryContainer = TextPrimary,
    secondary = NeonCyan,
    onSecondary = ObsidianVoid,
    secondaryContainer = RoyalPurpleDeep,
    onSecondaryContainer = NeonCyan,
    tertiary = NeonPink,
    onTertiary = TextPrimary,
    background = ObsidianVoid,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = RoyalPurpleDeep,
    onSurfaceVariant = TextSecondary,
    outline = DividerSubtle,
    error = CrimsonRed,
    onError = TextPrimary
)

val AddaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun AddaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AddaDarkColorScheme,
        typography = AddaTypography,
        shapes = AddaShapes,
        content = content
    )
}
