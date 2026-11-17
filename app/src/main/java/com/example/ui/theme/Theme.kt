package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val TableTrailLightColorScheme = lightColorScheme(
    primary = DeepCrimson,
    onPrimary = Color.White,
    primaryContainer = CrimsonContainer,
    onPrimaryContainer = OnCrimsonContainer,
    secondary = RoastedOrange,
    onSecondary = Color.White,
    secondaryContainer = OrangeContainer,
    onSecondaryContainer = OnOrangeContainer,
    tertiary = HerbGreen,
    onTertiary = Color.White,
    tertiaryContainer = HerbGreenContainer,
    background = WarmCreamBg,
    onBackground = EspressoInk,
    surface = WarmCreamSurface,
    onSurface = EspressoInk,
    surfaceVariant = WarmCreamSurfaceVariant,
    onSurfaceVariant = MutedCocoa,
    outline = WarmBorder,
    outlineVariant = WarmBorder,
    error = DeepCrimsonDark,
    onError = Color.White
)

private val TableTrailDarkColorScheme = darkColorScheme(
    primary = CoralCrimsonDark,
    onPrimary = Color(0xFF2B0705),
    primaryContainer = CrimsonContainerDark,
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = WarmAmberDark,
    onSecondary = Color(0xFF2C1302),
    secondaryContainer = OrangeContainerDark,
    onSecondaryContainer = Color(0xFFFFDCC4),
    tertiary = Color(0xFF81C784),
    onTertiary = Color(0xFF0A2E0D),
    background = EspressoBgDark,
    onBackground = CreamInkDark,
    surface = EspressoSurfaceDark,
    onSurface = CreamInkDark,
    surfaceVariant = EspressoSurfaceVariantDark,
    onSurfaceVariant = MutedCreamDark,
    outline = EspressoBorderDark,
    outlineVariant = EspressoBorderDark,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

val TableTrailShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun TableTrailTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TableTrailDarkColorScheme else TableTrailLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = TableTrailTypography,
        shapes = TableTrailShapes,
        content = content
    )
}
