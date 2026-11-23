package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme =
  darkColorScheme(
    primary = BabyBlueLight,
    onPrimary = Color(0xFF082F49),
    primaryContainer = Color(0xFF075985),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = SoftPeachLight,
    onSecondary = Color(0xFF431407),
    tertiary = BabyBlueAccent,
    background = SeaNavyDarkBg,
    surface = SeaNavyDarkSurface,
    surfaceVariant = SeaNavyDarkSurfaceVariant,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BabyBluePrimary,
    onPrimary = Color.White,
    primaryContainer = BabyBlueSoft,
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = SoftPeachSecondary,
    onSecondary = Color.White,
    secondaryContainer = SoftPeachSoft,
    onSecondaryContainer = Color(0xFF9A3412),
    tertiary = BabyBlueAccent,
    onTertiary = Color.White,
    background = SkyBgLight,
    surface = SkySurfaceLight,
    surfaceVariant = SkySurfaceVariantLight,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFFBAE6FD)
  )

@Composable
fun SennaraTheme(
  darkTheme: Boolean = false, // Keep default to light mode to ensure bright, cheerful colors requested by user
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  CompositionLocalProvider(
    LocalLayoutDirection provides LayoutDirection.Rtl,
    androidx.compose.material3.LocalTextStyle provides Typography.bodyMedium
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content
    )
  }
}
