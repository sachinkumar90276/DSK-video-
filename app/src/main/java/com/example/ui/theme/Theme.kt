package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AmoledColorScheme = darkColorScheme(
    primary = DskCrimson,
    onPrimary = DskWhite,
    primaryContainer = Color(0xFF42060B),
    onPrimaryContainer = Color(0xFFFFDADA),
    secondary = DskRoyalPurple,
    onSecondary = DskWhite,
    secondaryContainer = Color(0xFF2B1354),
    onSecondaryContainer = Color(0xFFE9DDFF),
    tertiary = DskNeonViolet,
    onTertiary = DskWhite,
    background = DskPitchBlack,
    onBackground = DskWhite,
    surface = DskObsidian,
    onSurface = DskWhite,
    surfaceVariant = DskSurfaceElevated,
    onSurfaceVariant = DskSilver,
    outline = DskGlassBorder,
    outlineVariant = Color(0x1FFFFFFF),
    error = DskError,
    onError = DskWhite
)

private val CinemaSlateColorScheme = AmoledColorScheme.copy(
    background = Color(0xFF0C0D16),
    surface = Color(0xFF141624),
    surfaceVariant = Color(0xFF1C1F32)
)

val DskShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

object DskGradients {
    val BrandAccent = Brush.horizontalGradient(
        colors = listOf(DskCrimson, DskRubyGlow, DskRoyalPurple)
    )
    val HeroOverlay = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            Color(0x6607070B),
            Color(0xE607070B),
            DskPitchBlack
        )
    )
    val GlassBackdrop = Brush.linearGradient(
        colors = listOf(
            Color(0xCC1A1A28),
            Color(0x99101018)
        )
    )
    val KidsHeader = Brush.horizontalGradient(
        colors = listOf(Color(0xFF7C3AED), Color(0xFFEC4899), Color(0xFFF59E0B))
    )
    val GlassBorderStroke = BorderStroke(1.dp, DskGlassBorder)
}

@Composable
fun DskVideoTheme(
    amoledBlack: Boolean = true,
    content: @Composable () -> Unit
) {
    val scheme = if (amoledBlack) AmoledColorScheme else CinemaSlateColorScheme
    MaterialTheme(
        colorScheme = scheme,
        typography = DskTypography,
        shapes = DskShapes,
        content = content
    )
}
