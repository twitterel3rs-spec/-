package com.example.util

/**
 * Utility functions for parsing numbers entered in Arabic or English locales.
 * Handles Eastern Arabic numerals (٠-٩), Arabic decimal marks (٫ ،), and standard commas/periods.
 */
fun String.parseArabicDoubleOrNull(): Double? {
    if (this.isBlank()) return null
    val normalized = this.trim()
        .replace('٠', '0')
        .replace('١', '1')
        .replace('٢', '2')
        .replace('٣', '3')
        .replace('٤', '4')
        .replace('٥', '5')
        .replace('٦', '6')
        .replace('٧', '7')
        .replace('٨', '8')
        .replace('٩', '9')
        .replace('٫', '.')
        .replace('،', '.')
        .replace(',', '.')
    return normalized.toDoubleOrNull()
}

fun String.parseArabicDouble(default: Double = 0.0): Double {
    return this.parseArabicDoubleOrNull() ?: default
}

fun formatFee(fee: Double): String {
    return String.format(java.util.Locale.US, "%.0f", fee)
}

fun Double.toFeeString(): String {
    return String.format(java.util.Locale.US, "%.0f", this)
}

fun String.parseArabicIntOrNull(): Int? {
    if (this.isBlank()) return null
    val normalized = this.trim()
        .replace('٠', '0')
        .replace('١', '1')
        .replace('٢', '2')
        .replace('٣', '3')
        .replace('٤', '4')
        .replace('٥', '5')
        .replace('٦', '6')
        .replace('٧', '7')
        .replace('٨', '8')
        .replace('٩', '9')
        .replace(".", "")
        .replace(",", "")
        .filter { it.isDigit() }
    return normalized.toIntOrNull()
}

/**
 * Returns the count of phone digits, accounting for both standard and Eastern Arabic digits (0-9, ٠-٩).
 */
fun String.phoneDigitCount(): Int {
    return this.count { it.isDigit() || it in '٠'..'٩' }
}

/**
 * Checks if a customer phone number is incomplete (less than 11 digits).
 * Returns true only if digits have actually been entered (1 to 10 digits).
 */
fun String.isCustomerPhoneIncomplete(): Boolean {
    val count = this.phoneDigitCount()
    return count in 1..10
}

/**
 * Checks if a customer phone number has more than 11 digits (too long / excess digits).
 */
fun String.isCustomerPhoneTooLong(): Boolean {
    val count = this.phoneDigitCount()
    return count > 11
}

/**
 * Checks if a customer phone number is invalid (either incomplete < 11 or too long > 11),
 * provided at least one digit was entered.
 */
fun String.isCustomerPhoneInvalid(): Boolean {
    val count = this.phoneDigitCount()
    return count > 0 && count != 11
}

/**
 * Returns a short warning label for supportingText or null if valid or empty.
 */
fun String.getCustomerPhoneWarning(): String? {
    val count = this.phoneDigitCount()
    return when {
        count == 0 -> null
        count in 1..10 -> "⚠️ الرقم ناقص ($count من 11 رقم)"
        count > 11 -> "⚠️ الرقم زائد ($count رقم - زائد ${count - 11} عن 11)"
        else -> null
    }
}

/**
 * Returns a detailed warning message for Toast notifications or dialog alerts.
 */
fun String.getCustomerPhoneToastWarning(): String? {
    val count = this.phoneDigitCount()
    return when {
        count in 1..10 -> "⚠️ تنبيه: رقم هاتف العميل ناقص ($count أرقام فقط من 11)"
        count > 11 -> "⚠️ تنبيه: رقم هاتف العميل زائد ($count رقماً - زائد ${count - 11} عن 11)"
        else -> null
    }
}

/**
 * Normalizes an Arabic phone number string to standard Western digits (0-9).
 */
fun String.normalizePhoneNumber(): String {
    return this.trim()
        .replace('٠', '0')
        .replace('١', '1')
        .replace('٢', '2')
        .replace('٣', '3')
        .replace('٤', '4')
        .replace('٥', '5')
        .replace('٦', '6')
        .replace('٧', '7')
        .replace('٨', '8')
        .replace('٩', '9')
}

/**
 * Normalizes Eastern Arabic digits (٠-٩) to standard ASCII digits (0-9).
 */
fun String.normalizeArabicDigits(): String {
    return this
        .replace('٠', '0')
        .replace('١', '1')
        .replace('٢', '2')
        .replace('٣', '3')
        .replace('٤', '4')
        .replace('٥', '5')
        .replace('٦', '6')
        .replace('٧', '7')
        .replace('٨', '8')
        .replace('٩', '9')
}

/**
 * Formats stock quantities including halves (½), quarters (¼, ¾), and clean decimals.
 * Examples:
 * - 0.5 -> "½ كيلو"
 * - 1.5 -> "1 ½ كيلو"
 * - 0.25 -> "¼ كيلو"
 * - 2.75 -> "2 ¾ كيلو"
 * - 5.0 -> "5 كيلو"
 */
fun formatStockFraction(stock: Double, unit: String): String {
    if (stock <= 0.0) return "نفد"

    val whole = stock.toInt()
    val frac = Math.round((stock - whole) * 1000.0) / 1000.0

    if (Math.abs(frac) < 0.001) {
        return "$whole $unit"
    }

    val fractionLabel = when {
        Math.abs(frac - 0.25) < 0.01 -> if (whole > 0) "$whole.25 $unit ($whole¼)" else "0.25 $unit (ربع)"
        Math.abs(frac - 0.5) < 0.01 -> if (whole > 0) "$whole.5 $unit ($whole½)" else "0.5 $unit (نصف)"
        Math.abs(frac - 0.75) < 0.01 -> if (whole > 0) "$whole.75 $unit ($whole¾)" else "0.75 $unit (¾)"
        Math.abs(frac - 0.125) < 0.01 -> if (whole > 0) "$whole.125 $unit ($whole⅛)" else "0.125 $unit (⅛)"
        Math.abs(frac - 0.375) < 0.01 -> if (whole > 0) "$whole.375 $unit ($whole⅜)" else "0.375 $unit (⅜)"
        Math.abs(frac - 0.625) < 0.01 -> if (whole > 0) "$whole.625 $unit ($whole⅝)" else "0.625 $unit (⅝)"
        Math.abs(frac - 0.875) < 0.01 -> if (whole > 0) "$whole.875 $unit ($whole⅞)" else "0.875 $unit (⅞)"
        Math.abs(frac - 0.333) < 0.02 -> if (whole > 0) "$whole.33 $unit ($whole⅓)" else "0.33 $unit (⅓)"
        Math.abs(frac - 0.666) < 0.02 -> if (whole > 0) "$whole.67 $unit ($whole⅔)" else "0.67 $unit (⅔)"
        else -> null
    }

    return fractionLabel ?: run {
        val formatted = String.format(java.util.Locale.US, "%.2f", stock)
            .trimEnd('0')
            .trimEnd('.')
        "$formatted $unit"
    }
}

/**
 * Checks if the string contains any Latin/English letters (a-z, A-Z)
 */
fun String.containsLatinLetters(): Boolean {
    return this.any { it in 'a'..'z' || it in 'A'..'Z' }
}

/**
 * Maps English QWERTY keyboard keystrokes to standard Arabic (101 keyboard) characters.
 * Useful when typing on a PC emulator (like LDPlayer) when the keyboard layout is stuck in English
 * or when the user types Arabic with the English layout active.
 */
fun convertEnglishKeyboardToArabic(input: String): String {
    if (input.isBlank()) return input
    val sb = java.lang.StringBuilder()
    for (ch in input) {
        when (ch) {
            'q' -> sb.append('ض')
            'w' -> sb.append('ص')
            'e' -> sb.append('ث')
            'r' -> sb.append('ق')
            't' -> sb.append('ف')
            'y' -> sb.append('غ')
            'u' -> sb.append('ع')
            'i' -> sb.append('ه')
            'o' -> sb.append('خ')
            'p' -> sb.append('ح')
            '[' -> sb.append('ج')
            ']' -> sb.append('د')
            'a' -> sb.append('ش')
            's' -> sb.append('س')
            'd' -> sb.append('ي')
            'f' -> sb.append('ب')
            'g' -> sb.append('ل')
            'h' -> sb.append('ا')
            'j' -> sb.append('ت')
            'k' -> sb.append('ن')
            'l' -> sb.append('م')
            ';' -> sb.append('ك')
            '\'' -> sb.append('ط')
            'z' -> sb.append('ئ')
            'x' -> sb.append('ء')
            'c' -> sb.append('ؤ')
            'v' -> sb.append('ر')
            'b' -> sb.append("لا")
            'n' -> sb.append('ى')
            'm' -> sb.append('ة')
            ',' -> sb.append('و')
            '.' -> sb.append('ز')
            '/' -> sb.append('ظ')
            '`' -> sb.append('ذ')
            '~' -> sb.append('ّ')
            'Q' -> sb.append('َ')
            'W' -> sb.append('ً')
            'E' -> sb.append('ُ')
            'R' -> sb.append('ٌ')
            'T' -> sb.append("لإ")
            'Y' -> sb.append('إ')
            'U' -> sb.append('‘')
            'I' -> sb.append('÷')
            'O' -> sb.append('×')
            'P' -> sb.append('؛')
            '{' -> sb.append('<')
            '}' -> sb.append('>')
            'A' -> sb.append('ِ')
            'S' -> sb.append('ٍ')
            'D' -> sb.append(']')
            'F' -> sb.append('[')
            'G' -> sb.append("لأ")
            'H' -> sb.append('أ')
            'J' -> sb.append('ـ')
            'K' -> sb.append('،')
            'L' -> sb.append('/')
            ':' -> sb.append(':')
            '"' -> sb.append('"')
            'Z' -> sb.append('~')
            'X' -> sb.append('ْ')
            'C' -> sb.append('}')
            'V' -> sb.append('{')
            'B' -> sb.append("لآ")
            'N' -> sb.append('آ')
            'M' -> sb.append('\'')
            '?' -> sb.append('؟')
            else -> sb.append(ch)
        }
    }
    return sb.toString()
}



