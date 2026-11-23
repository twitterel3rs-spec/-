package com.example.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

data class CategoryTheme(
    val id: String,
    val nameAr: String,
    val primaryColor: Color,
    val bgColor: Color,
    val borderColor: Color,
    val tagBgColor: Color,
    val tagTextColor: Color,
    val priceBgColor: Color,
    val priceTextColor: Color = Color.White
)

object CategoryColorUtil {

    val PRESET_THEMES = listOf(
        CategoryTheme(
            id = "TEAL",
            nameAr = "فيروزي صنارة",
            primaryColor = Color(0xFF0D9488),
            bgColor = Color(0xFFF0FDFA),
            borderColor = Color(0xFF5EEAD4),
            tagBgColor = Color(0xFFCCFBF1),
            tagTextColor = Color(0xFF0F766E),
            priceBgColor = Color(0xFF0D9488)
        ),
        CategoryTheme(
            id = "BLUE",
            nameAr = "أزرق بحري",
            primaryColor = Color(0xFF0284C7),
            bgColor = Color(0xFFF0F9FF),
            borderColor = Color(0xFF7DD3FC),
            tagBgColor = Color(0xFFE0F2FE),
            tagTextColor = Color(0xFF0369A1),
            priceBgColor = Color(0xFF0284C7)
        ),
        CategoryTheme(
            id = "ORANGE",
            nameAr = "برتقالي مرجاني",
            primaryColor = Color(0xFFEA580C),
            bgColor = Color(0xFFFFF7ED),
            borderColor = Color(0xFFFDBA74),
            tagBgColor = Color(0xFFFFEDD5),
            tagTextColor = Color(0xFFC2410C),
            priceBgColor = Color(0xFFEA580C)
        ),
        CategoryTheme(
            id = "AMBER",
            nameAr = "ذهبي مقلي",
            primaryColor = Color(0xFFD97706),
            bgColor = Color(0xFFFFFBEB),
            borderColor = Color(0xFFFCD34D),
            tagBgColor = Color(0xFFFEF3C7),
            tagTextColor = Color(0xFFB45309),
            priceBgColor = Color(0xFFD97706)
        ),
        CategoryTheme(
            id = "RED",
            nameAr = "أحمر طواجن",
            primaryColor = Color(0xFFDC2626),
            bgColor = Color(0xFFFEF2F2),
            borderColor = Color(0xFFFCA5A5),
            tagBgColor = Color(0xFFFEE2E2),
            tagTextColor = Color(0xFFB91C1C),
            priceBgColor = Color(0xFFDC2626)
        ),
        CategoryTheme(
            id = "PURPLE",
            nameAr = "بنفسجي وجبات",
            primaryColor = Color(0xFF9333EA),
            bgColor = Color(0xFFFAF5FF),
            borderColor = Color(0xFFD8B4FE),
            tagBgColor = Color(0xFFF3E8FF),
            tagTextColor = Color(0xFF7E22CE),
            priceBgColor = Color(0xFF9333EA)
        ),
        CategoryTheme(
            id = "GREEN",
            nameAr = "أخضر فريش",
            primaryColor = Color(0xFF16A34A),
            bgColor = Color(0xFFF0FDF4),
            borderColor = Color(0xFF86EFAC),
            tagBgColor = Color(0xFFDCFCE7),
            tagTextColor = Color(0xFF15803D),
            priceBgColor = Color(0xFF16A34A)
        ),
        CategoryTheme(
            id = "ROSE",
            nameAr = "وردي مميز",
            primaryColor = Color(0xFFE11D48),
            bgColor = Color(0xFFFFF1F2),
            borderColor = Color(0xFFFDA4AF),
            tagBgColor = Color(0xFFFFE4E6),
            tagTextColor = Color(0xFFBE123C),
            priceBgColor = Color(0xFFE11D48)
        ),
        CategoryTheme(
            id = "BROWN",
            nameAr = "بني فحم",
            primaryColor = Color(0xFF78350F),
            bgColor = Color(0xFFFEF3C7),
            borderColor = Color(0xFFF59E0B),
            tagBgColor = Color(0xFFFDE68A),
            tagTextColor = Color(0xFF78350F),
            priceBgColor = Color(0xFF92400E)
        ),
        CategoryTheme(
            id = "INDIGO",
            nameAr = "نيلي ملكي",
            primaryColor = Color(0xFF4F46E5),
            bgColor = Color(0xFFEEF2FF),
            borderColor = Color(0xFFA5B4FC),
            tagBgColor = Color(0xFFE0E7FF),
            tagTextColor = Color(0xFF3730A3),
            priceBgColor = Color(0xFF4F46E5)
        ),
        CategoryTheme(
            id = "SLATE",
            nameAr = "رمادي كلاسيك",
            primaryColor = Color(0xFF475569),
            bgColor = Color(0xFFF8FAFC),
            borderColor = Color(0xFFCBD5E1),
            tagBgColor = Color(0xFFE2E8F0),
            tagTextColor = Color(0xFF334155),
            priceBgColor = Color(0xFF475569)
        )
    )

    fun getThemeById(id: String?): CategoryTheme? {
        if (id.isNullOrBlank()) return null
        return PRESET_THEMES.firstOrNull { it.id.equals(id, ignoreCase = true) }
    }

    /**
     * Resolves the CategoryStyle for a given category name, checking custom saved theme ID first,
     * then falling back to keyword heuristics.
     */
    fun resolveCategoryStyle(
        categoryName: String,
        customThemeId: String? = null
    ): CategoryTheme {
        // 1. Try explicit user configured theme
        val explicitTheme = getThemeById(customThemeId)
        if (explicitTheme != null) return explicitTheme

        // 2. Keyword fallback heuristics
        val cat = categoryName.trim()
        return when {
            cat.contains("طازج") || cat.contains("سمك") || cat.contains("فريش") || cat.contains("نيء") || cat.contains("بلطي") || cat.contains("بوري") ->
                getThemeById("BLUE") ?: PRESET_THEMES[1]
            cat.contains("مقلي") || cat.contains("كرسبي") || cat.contains("بانيه") || cat.contains("جمبري") ->
                getThemeById("AMBER") ?: PRESET_THEMES[3]
            cat.contains("مشوي") || cat.contains("سنجاري") || cat.contains("ردة") || cat.contains("فحم") || cat.contains("زيت وليمون") ->
                getThemeById("ORANGE") ?: PRESET_THEMES[2]
            cat.contains("طواجن") || cat.contains("طاجن") || cat.contains("شوربة") || cat.contains("سي فود") ->
                getThemeById("RED") ?: PRESET_THEMES[4]
            cat.contains("أرز") || cat.contains("مكرونة") || cat.contains("وجبات") ->
                getThemeById("PURPLE") ?: PRESET_THEMES[5]
            cat.contains("سلطة") || cat.contains("خضار") || cat.contains("مقبلات") ->
                getThemeById("GREEN") ?: PRESET_THEMES[6]
            cat.contains("ساندوتش") ->
                getThemeById("BROWN") ?: PRESET_THEMES[8]
            else ->
                PRESET_THEMES[0] // Default Teal
        }
    }
}
