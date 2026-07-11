package com.mkmemories.copilot.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Palette « aurore et forge » — assortie à l'emblème bouclier-fjord et au hero
val BrandNight = Color(0xFF0D1420)        // charbon nuit
val BrandNightHigh = Color(0xFF141B23)    // charbon relevé (fond icône)
val BrandSurface = Color(0xFF16202E)      // surface des cartes
val BrandSurfaceHigh = Color(0xFF1C2938)  // surface au survol / relief
val BrandIce = Color(0xFF38D6FF)          // cyan glacier
val BrandAuroraTeal = Color(0xFF2EE6A8)   // vert aurore
val BrandAuroraViolet = Color(0xFF8B5CF6) // violet aurore
val BrandEmber = Color(0xFFFF6B2C)        // orange braise
val BrandGold = Color(0xFFE8B84B)         // or bruni
val BrandGoldLight = Color(0xFFF6DE9A)    // or clair (reflets)
val BrandMist = Color(0xFFAFC3D6)         // brume — textes secondaires

private val NordicColorScheme = darkColorScheme(
    primary = BrandIce,
    onPrimary = BrandNight,
    secondary = BrandAuroraTeal,
    onSecondary = BrandNight,
    tertiary = BrandGold,
    onTertiary = BrandNight,
    background = BrandNight,
    onBackground = Color.White,
    surface = BrandSurface,
    onSurface = Color.White,
    surfaceVariant = BrandSurfaceHigh,
    onSurfaceVariant = BrandMist,
    error = BrandEmber,
    outline = BrandGold.copy(alpha = 0.35f),
)

// Typo système travaillée : graisses marquées, interlettrage serré sur les titres
private val NordicTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.25).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.15.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.2.sp,
    ),
)

private val NordicShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun MKCopilotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NordicColorScheme,
        typography = NordicTypography,
        shapes = NordicShapes,
        content = content,
    )
}
