package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val OpenJarvisDarkColorScheme = darkColorScheme(
    primary = NeuralCyan,
    onPrimary = NeuralOnCyan,
    primaryContainer = NeuralCyanContainer,
    onPrimaryContainer = NeuralCyan,
    secondary = JarvisGold,
    onSecondary = Color(0xFF1F1400),
    secondaryContainer = JarvisGoldContainer,
    onSecondaryContainer = JarvisGold,
    tertiary = JarvisEmerald,
    onTertiary = Color(0xFF002116),
    tertiaryContainer = JarvisEmeraldContainer,
    onTertiaryContainer = JarvisEmerald,
    error = JarvisRose,
    onError = Color.White,
    errorContainer = JarvisRoseContainer,
    onErrorContainer = Color(0xFFFFB4C0),
    background = ObsidianVoid,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerHigh = ObsidianCardHigh,
    outline = ObsidianOutline
)

private val OpenJarvisLightColorScheme = lightColorScheme(
    primary = DeepCyanLight,
    onPrimary = Color.White,
    primaryContainer = DeepCyanContainerLight,
    onPrimaryContainer = Color(0xFF002028),
    secondary = DeepAmberLight,
    onSecondary = Color.White,
    secondaryContainer = DeepAmberContainerLight,
    onSecondaryContainer = Color(0xFF331800),
    tertiary = JarvisEmerald,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF064E3B),
    error = JarvisRose,
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF881337),
    background = AlabasterCanvas,
    onBackground = TextPrimaryLight,
    surface = AlabasterSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = AlabasterVariant,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerHigh = Color(0xFFEEF3FA),
    outline = AlabasterOutline
)

val OpenJarvisShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun OpenJarvisTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) OpenJarvisDarkColorScheme else OpenJarvisLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OpenJarvisTypography,
        shapes = OpenJarvisShapes,
        content = content
    )
}
