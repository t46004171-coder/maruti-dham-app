package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.model.ThemeConfig

@Immutable
data class MarutiDynamicPalette(
    val background: Color = NavyDeep,
    val surface: Color = NavySurface,
    val card: Color = NavyCard,
    val elevated: Color = NavyElevated,
    val primary: Color = RoyalGold,
    val accent: Color = LuminousGold,
    val textPrimary: Color = CreamIvory,
    val textSecondary: Color = MutedSlate,
    val isLightBackground: Boolean = false
)

val LocalMarutiPalette = staticCompositionLocalOf { MarutiDynamicPalette() }

fun parseHexColorSafe(hex: String, fallback: Color): Color {
    val cleaned = hex.trim().removePrefix("#")
    if (cleaned.length != 6 && cleaned.length != 8) return fallback
    return try {
        val value = cleaned.toLong(16)
        val argb = if (cleaned.length == 6) (0xFF000000L or value) else value
        Color(argb.toInt())
    } catch (_: Exception) {
        fallback
    }
}

private val MarutiShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun MarutiDhamTheme(
    isHindi: Boolean = false,
    themeConfig: ThemeConfig = ThemeConfig(),
    content: @Composable () -> Unit
) {
    val bgColor = parseHexColorSafe(themeConfig.backgroundColorHex, NavyDeep)
    val surfaceColor = parseHexColorSafe(themeConfig.surfaceColorHex, NavyCard)
    val primaryColor = parseHexColorSafe(themeConfig.primaryColorHex, RoyalGold)
    val accentColor = parseHexColorSafe(themeConfig.accentColorHex, LuminousGold)
    val textColor = parseHexColorSafe(themeConfig.textColorHex, CreamIvory)
    val isLightBg = bgColor.luminance() > 0.45f
    val secondaryText = if (isLightBg) Color(0xFF4B5563) else MutedSlate
    val elevatedColor = if (isLightBg) Color(0xFFEDE6D6) else surfaceColor

    val colorScheme = if (isLightBg) {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = elevatedColor,
            onPrimaryContainer = textColor,
            secondary = accentColor,
            onSecondary = textColor,
            secondaryContainer = surfaceColor,
            onSecondaryContainer = textColor,
            tertiary = AntiqueGold,
            onTertiary = Color.White,
            background = bgColor,
            onBackground = textColor,
            surface = surfaceColor,
            onSurface = textColor,
            surfaceVariant = elevatedColor,
            onSurfaceVariant = secondaryText,
            outline = primaryColor.copy(alpha = 0.5f),
            outlineVariant = AntiqueGold.copy(alpha = 0.45f),
            error = StatusClosedRed,
            onError = Color.White
        )
    } else {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = NavyDeep,
            primaryContainer = elevatedColor,
            onPrimaryContainer = accentColor,
            secondary = WarmSaffron,
            onSecondary = NavyDeep,
            secondaryContainer = surfaceColor,
            onSecondaryContainer = textColor,
            tertiary = AntiqueGold,
            onTertiary = textColor,
            background = bgColor,
            onBackground = textColor,
            surface = surfaceColor,
            onSurface = textColor,
            surfaceVariant = surfaceColor,
            onSurfaceVariant = secondaryText,
            outline = NavyOutline,
            outlineVariant = primaryColor.copy(alpha = 0.45f),
            error = StatusClosedRed,
            onError = Color.White
        )
    }

    val palette = MarutiDynamicPalette(
        background = bgColor,
        surface = surfaceColor,
        card = surfaceColor,
        elevated = elevatedColor,
        primary = primaryColor,
        accent = accentColor,
        textPrimary = textColor,
        textSecondary = secondaryText,
        isLightBackground = isLightBg
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLightBg
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = isLightBg
            }
        }
    }

    CompositionLocalProvider(LocalMarutiPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildMarutiTypography(isHindi = isHindi),
            shapes = MarutiShapes,
            content = content
        )
    }
}
