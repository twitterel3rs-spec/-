package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.example.R

// Bundled Google Font (Cairo) designed specifically for Arabic typography
val CairoFontFamily = FontFamily(
    Font(R.font.cairo, FontWeight.Normal),
    Font(R.font.cairo, FontWeight.Medium),
    Font(R.font.cairo, FontWeight.SemiBold),
    Font(R.font.cairo, FontWeight.Bold)
)

// Crucial for Arabic typography: includeFontPadding = true prevents clipping of
// deep descenders (e.g., 'ح' in 'بحث', 'ي', 'ع', 'ج', 'ر') and diacritics
@Suppress("DEPRECATION")
val ArabicPlatformTextStyle = PlatformTextStyle(
    includeFontPadding = true
)

// Centered line height with no trimming to guarantee neither ascenders nor descenders are cut off
val ArabicLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

private fun createArabicStyle(
    fontWeight: FontWeight,
    fontSize: Int,
    lineHeight: Int,
    letterSpacing: Double = 0.0
): TextStyle = TextStyle(
    fontFamily = CairoFontFamily,
    fontWeight = fontWeight,
    fontSize = fontSize.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
    platformStyle = ArabicPlatformTextStyle,
    lineHeightStyle = ArabicLineHeightStyle
)

// Comprehensive Material 3 Typography tuned for Arabic script
val Typography = Typography(
    displayLarge = createArabicStyle(FontWeight.Normal, 57, 68),
    displayMedium = createArabicStyle(FontWeight.Normal, 45, 56),
    displaySmall = createArabicStyle(FontWeight.Normal, 36, 46),

    headlineLarge = createArabicStyle(FontWeight.SemiBold, 32, 42),
    headlineMedium = createArabicStyle(FontWeight.SemiBold, 28, 38),
    headlineSmall = createArabicStyle(FontWeight.SemiBold, 24, 34),

    titleLarge = createArabicStyle(FontWeight.Bold, 22, 32),
    titleMedium = createArabicStyle(FontWeight.Bold, 16, 26),
    titleSmall = createArabicStyle(FontWeight.Bold, 14, 22),

    bodyLarge = createArabicStyle(FontWeight.Normal, 16, 26, 0.2),
    bodyMedium = createArabicStyle(FontWeight.Normal, 14, 22, 0.2),
    bodySmall = createArabicStyle(FontWeight.Normal, 12, 19, 0.2),

    labelLarge = createArabicStyle(FontWeight.Medium, 14, 22, 0.1),
    labelMedium = createArabicStyle(FontWeight.Medium, 12, 19, 0.1),
    labelSmall = createArabicStyle(FontWeight.Medium, 11, 17, 0.1)
)
