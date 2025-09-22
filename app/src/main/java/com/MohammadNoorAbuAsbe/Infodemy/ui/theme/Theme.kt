package com.MohammadNoorAbuAsbe.Infodemy.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = BgDark,
    primaryContainer = Primary.copy(alpha = 0.3f),
    onPrimaryContainer = TextColor,
    secondary = Secondary,
    onSecondary = BgDark,
    secondaryContainer = Secondary.copy(alpha = 0.3f),
    onSecondaryContainer = TextColor,
    tertiary = Info,
    onTertiary = BgDark,
    background = Bg,
    onBackground = TextColor,
    surface = BgLight,
    onSurface = TextColor,
    surfaceVariant = BgLight,
    onSurfaceVariant = TextMuted,
    surfaceTint = Primary,
    inverseSurface = TextColor,
    inverseOnSurface = Bg,
    error = Danger,
    onError = TextColor,
    errorContainer = Danger.copy(alpha = 0.3f),
    onErrorContainer = TextColor,
    outline = Border,
    outlineVariant = BorderMuted,
    scrim = BgDark.copy(alpha = 0.8f)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = BgLightTheme,
    primaryContainer = PrimaryLight.copy(alpha = 0.3f),
    onPrimaryContainer = TextDarkTheme,
    secondary = SecondaryLight,
    onSecondary = BgLightTheme,
    secondaryContainer = SecondaryLight.copy(alpha = 0.3f),
    onSecondaryContainer = TextDarkTheme,
    tertiary = Info,
    onTertiary = BgLightTheme,
    background = BgMainLight,
    onBackground = TextDarkTheme,
    surface = BgLightTheme,
    onSurface = TextDarkTheme,
    surfaceVariant = BgDarkLight,
    onSurfaceVariant = TextMutedLight,
    surfaceTint = PrimaryLight,
    inverseSurface = TextDarkTheme,
    inverseOnSurface = BgMainLight,
    error = Danger,
    onError = BgLightTheme,
    errorContainer = Danger.copy(alpha = 0.3f),
    onErrorContainer = TextDarkTheme,
    outline = BorderLight,
    outlineVariant = BorderMutedLight,
    scrim = TextDarkTheme.copy(alpha = 0.8f)
)

@Composable
fun MyRuppinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

