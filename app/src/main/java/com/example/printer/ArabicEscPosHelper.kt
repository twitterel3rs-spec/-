package com.example.printer

/**
 * High-performance, zero-dependency Arabic text shaper, BiDi reorderer,
 * and ESC/POS code page mapper specifically tailored for thermal receipt printers (Xprinter, Epson, etc.).
 *
 * It formats Arabic text into real connected text commands (WITHOUT rasterizing into images):
 * 1. Contextual Arabic Shaping (Isolated, Initial, Medial, Final).
 * 2. Proper Lam-Alef ligatures (لا, لأ, لإ, لآ).
 * 3. Bidirectional (BiDi) line reordering (Arabic runs RTL, numbers and Latin text stay LTR).
 * 4. Dual codepage encoding:
 *    - PC864 (IBM/DOS 864): Standard hardware font table with native positional glyphs.
 *    - PC1256 (Windows-1256): Standard Arabic Windows table.
 */
object ArabicEscPosHelper {

    enum class CodepageMode(val codeTableIndex: Byte, val label: String) {
        CP864_PAGE_22(0x16, "PC864 (صفحة 22 - افتراضي Xprinter)"),
        CP864_PAGE_37(0x25, "PC864 (صفحة 37)"),
        CP1256_PAGE_50(0x32, "PC1256 (صفحة 50 - Windows Arabic)"),
        CP1256_PAGE_32(0x20, "PC1256 (صفحة 32 - WPC1256)")
    }

    // ESC/POS Commands
    val CMD_CANCEL_KANJI = byteArrayOf(0x1C, 0x2E) // FS . (Cancel Kanji 2-byte mode to ensure 1-byte code table)

    fun getSelectCodepageCommand(mode: CodepageMode): ByteArray {
        return byteArrayOf(0x1B, 0x74, mode.codeTableIndex) // ESC t <n>
    }

    /**
     * Process an entire block of text (which may contain multiple lines, numbers, Latin words, and punctuation)
     * and produce the raw byte sequence ready to send directly to the thermal printer socket.
     */
    fun formatTextForPrinter(text: String, mode: CodepageMode = CodepageMode.CP864_PAGE_22): ByteArray {
        val lines = text.split("\n")
        val output = mutableListOf<Byte>()

        for (i in lines.indices) {
            val line = lines[i]
            if (line.isNotEmpty()) {
                val lineBytes = processSingleLine(line, mode)
                for (b in lineBytes) output.add(b)
            }
            if (i < lines.size - 1) {
                output.add(0x0A.toByte()) // LF
            }
        }

        return output.toByteArray()
    }

    /**
     * Reorders and shapes a single line of text for LTR thermal printing.
     */
    fun processSingleLine(line: String, mode: CodepageMode): ByteArray {
        // If line contains no Arabic characters, encode as standard ASCII/CP
        if (!containsArabic(line)) {
            return encodeLatinAscii(line, mode)
        }

        // Tokenize line into runs: Arabic words, numbers, Latin words, spaces, symbols
        val tokens = tokenizeLine(line)

        // For thermal printers that print Left-to-Right:
        // A right-to-left line must have its tokens reversed overall,
        // with Arabic words reversed letter-by-letter (shaped),
        // while numbers and Latin words remain in their natural LTR direction.
        val reorderedTokens = mutableListOf<Token>()
        for (token in tokens.asReversed()) {
            reorderedTokens.add(token)
        }

        val resultBytes = mutableListOf<Byte>()
        for (token in reorderedTokens) {
            when (token.type) {
                TokenType.ARABIC -> {
                    // Shape and reverse Arabic characters
                    val shaped = shapeArabicWord(token.text)
                    val reversedShaped = shaped.reversed()
                    for (ch in reversedShaped) {
                        val b = encodeArabicChar(ch, mode)
                        resultBytes.add(b)
                    }
                }
                TokenType.NUMBER, TokenType.LATIN -> {
                    // Numbers & Latin words keep their natural left-to-right order
                    for (ch in token.text) {
                        resultBytes.add((ch.code and 0xFF).toByte())
                    }
                }
                TokenType.SYMBOL, TokenType.SPACE -> {
                    for (ch in token.text) {
                        val b = encodeSymbol(ch, mode)
                        resultBytes.add(b)
                    }
                }
            }
        }

        return resultBytes.toByteArray()
    }

    private enum class TokenType { ARABIC, NUMBER, LATIN, SYMBOL, SPACE }
    private data class Token(val text: String, val type: TokenType)

    private fun tokenizeLine(line: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == ' ' || c == '\t' -> {
                    val start = i
                    while (i < line.length && (line[i] == ' ' || line[i] == '\t')) i++
                    tokens.add(Token(line.substring(start, i), TokenType.SPACE))
                }
                isArabic(c) -> {
                    val start = i
                    while (i < line.length && isArabic(line[i])) i++
                    tokens.add(Token(line.substring(start, i), TokenType.ARABIC))
                }
                c.isDigit() -> {
                    val start = i
                    while (i < line.length && (line[i].isDigit() || (line[i] == '.' && i + 1 < line.length && line[i + 1].isDigit()))) {
                        i++
                    }
                    tokens.add(Token(line.substring(start, i), TokenType.NUMBER))
                }
                isLatin(c) -> {
                    val start = i
                    while (i < line.length && isLatin(line[i])) i++
                    tokens.add(Token(line.substring(start, i), TokenType.LATIN))
                }
                else -> {
                    tokens.add(Token(c.toString(), TokenType.SYMBOL))
                    i++
                }
            }
        }
        return tokens
    }

    private fun isArabic(c: Char): Boolean {
        val code = c.code
        return (code in 0x0600..0x06FF) || (code in 0x0750..0x077F) || (code in 0xFB50..0xFDFF) || (code in 0xFE70..0xFEFF)
    }

    private fun isLatin(c: Char): Boolean {
        return (c in 'a'..'z') || (c in 'A'..'Z')
    }

    private fun containsArabic(s: String): Boolean {
        for (c in s) {
            if (isArabic(c)) return true
        }
        return false
    }

    // --- Arabic Shaping Engine ---

    enum class Form { ISOLATED, INITIAL, MEDIAL, FINAL }

    private class LetterForms(
        val isolated: Char,
        val finalForm: Char,
        val initialForm: Char,
        val medialForm: Char,
        val connectsPreceding: Boolean,
        val connectsSucceeding: Boolean
    )

    private val formsMap = mapOf(
        // Hamza
        '\u0621' to LetterForms('\uFE80', '\uFE80', '\uFE80', '\uFE80', connectsPreceding = false, connectsSucceeding = false), // ء
        // Alef with Madda
        '\u0622' to LetterForms('\uFE81', '\uFE82', '\uFE81', '\uFE82', connectsPreceding = true, connectsSucceeding = false),  // آ
        // Alef with Hamza Above
        '\u0623' to LetterForms('\uFE83', '\uFE84', '\uFE83', '\uFE84', connectsPreceding = true, connectsSucceeding = false),  // أ
        // Waw with Hamza
        '\u0624' to LetterForms('\uFE85', '\uFE86', '\uFE85', '\uFE86', connectsPreceding = true, connectsSucceeding = false),  // ؤ
        // Alef with Hamza Below
        '\u0625' to LetterForms('\uFE87', '\uFE88', '\uFE87', '\uFE88', connectsPreceding = true, connectsSucceeding = false),  // إ
        // Yeh with Hamza
        '\u0626' to LetterForms('\uFE89', '\uFE8A', '\uFE8B', '\uFE8C', connectsPreceding = true, connectsSucceeding = true),   // ئ
        // Alef
        '\u0627' to LetterForms('\uFE8D', '\uFE8E', '\uFE8D', '\uFE8E', connectsPreceding = true, connectsSucceeding = false),  // ا
        // Beh
        '\u0628' to LetterForms('\uFE8F', '\uFE90', '\uFE91', '\uFE92', connectsPreceding = true, connectsSucceeding = true),   // ب
        // Teh Marbuta
        '\u0629' to LetterForms('\uFE93', '\uFE94', '\uFE93', '\uFE94', connectsPreceding = true, connectsSucceeding = false),  // ة
        // Teh
        '\u062A' to LetterForms('\uFE95', '\uFE96', '\uFE97', '\uFE98', connectsPreceding = true, connectsSucceeding = true),   // ت
        // Theh
        '\u062B' to LetterForms('\uFE99', '\uFE9A', '\uFE9B', '\uFE9C', connectsPreceding = true, connectsSucceeding = true),   // ث
        // Jeem
        '\u062C' to LetterForms('\uFE9D', '\uFE9E', '\uFE9F', '\uFEA0', connectsPreceding = true, connectsSucceeding = true),   // ج
        // Hah
        '\u062D' to LetterForms('\uFEA1', '\uFEA2', '\uFEA3', '\uFEA4', connectsPreceding = true, connectsSucceeding = true),   // ح
        // Khah
        '\u062E' to LetterForms('\uFEA5', '\uFEA6', '\uFEA7', '\uFEA8', connectsPreceding = true, connectsSucceeding = true),   // خ
        // Dal
        '\u062F' to LetterForms('\uFEA9', '\uFEAA', '\uFEA9', '\uFEAA', connectsPreceding = true, connectsSucceeding = false),  // د
        // Thal
        '\u0630' to LetterForms('\uFEAB', '\uFEAC', '\uFEAB', '\uFEAC', connectsPreceding = true, connectsSucceeding = false),  // ذ
        // Reh
        '\u0631' to LetterForms('\uFEAD', '\uFEAE', '\uFEAD', '\uFEAE', connectsPreceding = true, connectsSucceeding = false),  // ر
        // Zain
        '\u0632' to LetterForms('\uFEAF', '\uFEB0', '\uFEAF', '\uFEB0', connectsPreceding = true, connectsSucceeding = false),  // ز
        // Seen
        '\u0633' to LetterForms('\uFEB1', '\uFEB2', '\uFEB3', '\uFEB4', connectsPreceding = true, connectsSucceeding = true),   // س
        // Sheen
        '\u0634' to LetterForms('\uFEB5', '\uFEB6', '\uFEB7', '\uFEB8', connectsPreceding = true, connectsSucceeding = true),   // ش
        // Sad
        '\u0635' to LetterForms('\uFEB9', '\uFEBA', '\uFEBB', '\uFEBC', connectsPreceding = true, connectsSucceeding = true),   // ص
        // Dad
        '\u0636' to LetterForms('\uFEBD', '\uFEBE', '\uFEBF', '\uFEC0', connectsPreceding = true, connectsSucceeding = true),   // ض
        // Tah
        '\u0637' to LetterForms('\uFEC1', '\uFEC2', '\uFEC3', '\uFEC4', connectsPreceding = true, connectsSucceeding = true),   // ط
        // Zah
        '\u0638' to LetterForms('\uFEC5', '\uFEC6', '\uFEC7', '\uFEC8', connectsPreceding = true, connectsSucceeding = true),   // ظ
        // Ain
        '\u0639' to LetterForms('\uFEC9', '\uFECA', '\uFECB', '\uFECC', connectsPreceding = true, connectsSucceeding = true),   // ع
        // Ghain
        '\u063A' to LetterForms('\uFECD', '\uFECE', '\uFECF', '\uFED0', connectsPreceding = true, connectsSucceeding = true),   // غ
        // Tatweel
        '\u0640' to LetterForms('\u0640', '\u0640', '\u0640', '\u0640', connectsPreceding = true, connectsSucceeding = true),   // ـ
        // Feh
        '\u0641' to LetterForms('\uFED1', '\uFED2', '\uFED3', '\uFED4', connectsPreceding = true, connectsSucceeding = true),   // ف
        // Qaf
        '\u0642' to LetterForms('\uFED5', '\uFED6', '\uFED7', '\uFED8', connectsPreceding = true, connectsSucceeding = true),   // ق
        // Kaf
        '\u0643' to LetterForms('\uFED9', '\uFEDA', '\uFEDB', '\uFEDC', connectsPreceding = true, connectsSucceeding = true),   // ك
        // Lam
        '\u0644' to LetterForms('\uFEDD', '\uFEDE', '\uFEDF', '\uFEE0', connectsPreceding = true, connectsSucceeding = true),   // ل
        // Meem
        '\u0645' to LetterForms('\uFEE1', '\uFEE2', '\uFEE3', '\uFEE4', connectsPreceding = true, connectsSucceeding = true),   // م
        // Noon
        '\u0646' to LetterForms('\uFEE5', '\uFEE6', '\uFEE7', '\uFEE8', connectsPreceding = true, connectsSucceeding = true),   // ن
        // Heh
        '\u0647' to LetterForms('\uFEE9', '\uFEEA', '\uFEEB', '\uFEEC', connectsPreceding = true, connectsSucceeding = true),   // ه
        // Waw
        '\u0648' to LetterForms('\uFEED', '\uFEEE', '\uFEED', '\uFEEE', connectsPreceding = true, connectsSucceeding = false),  // و
        // Alef Maksura
        '\u0649' to LetterForms('\uFEEF', '\uFEF0', '\uFEEF', '\uFEF0', connectsPreceding = true, connectsSucceeding = false),  // ى
        // Yeh
        '\u064A' to LetterForms('\uFEF1', '\uFEF2', '\uFEF3', '\uFEF4', connectsPreceding = true, connectsSucceeding = true)    // ي
    )

    // Shapes an Arabic word into Presentation Forms-B characters
    fun shapeArabicWord(word: String): List<Char> {
        val chars = word.toCharArray()
        val result = mutableListOf<Char>()
        var i = 0

        while (i < chars.size) {
            val curr = chars[i]

            // Check Lam-Alef ligatures
            if (curr == '\u0644' && i + 1 < chars.size) {
                val next = chars[i + 1]
                val lamAlef = when (next) {
                    '\u0627' -> Pair('\uFEFB', '\uFEFC') // لا (Isolated, Final)
                    '\u0623' -> Pair('\uFEF7', '\uFEF8') // لأ
                    '\u0625' -> Pair('\uFEF9', '\uFEFA') // لإ
                    '\u0622' -> Pair('\uFEF5', '\uFEF6') // لآ
                    else -> null
                }

                if (lamAlef != null) {
                    val prevConnects = if (i > 0) {
                        formsMap[chars[i - 1]]?.connectsSucceeding == true
                    } else false

                    val chosen = if (prevConnects) lamAlef.second else lamAlef.first
                    result.add(chosen)
                    i += 2
                    continue
                }
            }

            val formInfo = formsMap[curr]
            if (formInfo == null) {
                result.add(curr)
                i++
                continue
            }

            val prevConnects = if (i > 0) {
                formsMap[chars[i - 1]]?.connectsSucceeding == true
            } else false

            val nextConnects = if (i + 1 < chars.size) {
                formsMap[chars[i + 1]]?.connectsPreceding == true
            } else false

            val shapedChar = when {
                prevConnects && nextConnects && formInfo.connectsPreceding && formInfo.connectsSucceeding -> formInfo.medialForm
                prevConnects && formInfo.connectsPreceding -> formInfo.finalForm
                nextConnects && formInfo.connectsSucceeding -> formInfo.initialForm
                else -> formInfo.isolated
            }

            result.add(shapedChar)
            i++
        }

        return result
    }

    // --- CP864 & CP1256 Byte Encoders ---

    // Verified CP864 character-to-byte mapping
    private val CP864_BYTE_MAP = mapOf(
        '\u00B0' to 0x80, // °
        '\u00B7' to 0x81, // ·
        '\u2219' to 0x82, // ∙
        '\u221A' to 0x83, // √
        '\u2592' to 0x84, // ▒
        '\u2500' to 0x85, // ─
        '\u2502' to 0x86, // │
        '\u253C' to 0x87, // ┼
        '\u2524' to 0x88, // ┤
        '\u252C' to 0x89, // ┬
        '\u251C' to 0x8A, // ├
        '\u2534' to 0x8B, // ┴
        '\u2510' to 0x8C, // ┐
        '\u250C' to 0x8D, // ┌
        '\u2514' to 0x8E, // └
        '\u2518' to 0x8F, // ┘
        '\u03B2' to 0x90, // β
        '\u221E' to 0x91, // ∞
        '\u03C6' to 0x92, // φ
        '\u00B1' to 0x93, // ±
        '\u00BD' to 0x94, // ½
        '\u00BC' to 0x95, // ¼
        '\u2248' to 0x96, // ≈
        '\u00AB' to 0x97, // «
        '\u00BB' to 0x98, // »
        '\uFEF7' to 0x99, // ﻷ (Lam-Alef with Hamza Above Isolated)
        '\uFEF8' to 0x9A, // ﻸ (Lam-Alef with Hamza Above Final)
        '\uFEF9' to 0x99, // ﻹ (Lam-Alef with Hamza Below Isolated fallback)
        '\uFEFA' to 0x9A, // ﻺ (Lam-Alef with Hamza Below Final fallback)
        '\uFEFB' to 0x9D, // ﻻ (Lam-Alef Isolated)
        '\uFEFC' to 0x9E, // ﻼ (Lam-Alef Final)
        '\u00A0' to 0xA0, // NBSP
        '\u00AD' to 0xA1, // Soft hyphen
        '\uFE82' to 0xA2, // ﺂ (Alef with Madda Final)
        '\u00A3' to 0xA3, // £
        '\u00A4' to 0xA4, // ¤
        '\uFE84' to 0xA5, // ﺄ (Alef with Hamza Above Final)
        '\uFE88' to 0xA5, // ﺈ (Alef with Hamza Below Final fallback)
        '\uFE8E' to 0xA8, // ﺎ (Alef Final)
        '\uFE8F' to 0xA9, // ﺏ (Beh Isolated)
        '\uFE90' to 0xA9, // ﺐ (Beh Final)
        '\uFE95' to 0xAA, // ﺕ (Teh Isolated)
        '\uFE96' to 0xAA, // ﺖ (Teh Final)
        '\uFE99' to 0xAB, // ﺙ (Theh Isolated)
        '\uFE9A' to 0xAB, // ﺚ (Theh Final)
        '\u060C' to 0xAC, // ، (Arabic Comma)
        '\uFE9D' to 0xAD, // ﺝ (Jeem Isolated)
        '\uFE9E' to 0xAD, // ﺞ (Jeem Final)
        '\uFEA1' to 0xAE, // ﺡ (Hah Isolated)
        '\uFEA2' to 0xAE, // ﺢ (Hah Final)
        '\uFEA5' to 0xAF, // ﺥ (Khah Isolated)
        '\uFEA6' to 0xAF, // ﺦ (Khah Final)
        '\u0660' to 0xB0, // ٠
        '\u0661' to 0xB1, // ١
        '\u0662' to 0xB2, // ٢
        '\u0663' to 0xB3, // ٣
        '\u0664' to 0xB4, // ٤
        '\u0665' to 0xB5, // ٥
        '\u0666' to 0xB6, // ٦
        '\u0667' to 0xB7, // ٧
        '\u0668' to 0xB8, // ٨
        '\u0669' to 0xB9, // ٩
        '\uFED1' to 0xBA, // ﻑ (Feh Isolated)
        '\uFED2' to 0xBA, // ﻒ (Feh Final)
        '\u061B' to 0xBB, // ؛ (Arabic Semicolon)
        '\uFEB1' to 0xBC, // ﺱ (Seen Isolated)
        '\uFEB2' to 0xBC, // ﺲ (Seen Final)
        '\uFEB5' to 0xBD, // ﺵ (Sheen Isolated)
        '\uFEB6' to 0xBD, // ﺶ (Sheen Final)
        '\uFEB9' to 0xBE, // ﺹ (Sad Isolated)
        '\uFEBA' to 0xBE, // ﺺ (Sad Final)
        '\u061F' to 0xBF, // ؟ (Arabic Question Mark)
        '\u00A2' to 0xC0, // ¢
        '\uFE80' to 0xC1, // ﺀ (Hamza Isolated)
        '\uFE81' to 0xC2, // ﺁ (Alef with Madda Isolated)
        '\uFE83' to 0xC3, // ﺃ (Alef with Hamza Above Isolated)
        '\uFE85' to 0xC4, // ﺅ (Waw with Hamza Isolated)
        '\uFE86' to 0xC4, // ﺆ (Waw with Hamza Final)
        '\uFECA' to 0xC5, // ﻊ (Ain Final)
        '\uFE8B' to 0xC6, // ﺋ (Yeh with Hamza Initial)
        '\uFE8C' to 0xC6, // ﺌ (Yeh with Hamza Medial)
        '\uFE89' to 0xC6, // ﺉ (Yeh with Hamza Isolated)
        '\uFE8A' to 0xC6, // ﺊ (Yeh with Hamza Final)
        '\uFE8D' to 0xC7, // ﺍ (Alef Isolated)
        '\uFE87' to 0xC7, // ﺇ (Alef with Hamza Below Isolated fallback)
        '\uFE91' to 0xC8, // ﺑ (Beh Initial)
        '\uFE92' to 0xC8, // ﺒ (Beh Medial)
        '\uFE93' to 0xC9, // ﺓ (Teh Marbuta Isolated)
        '\uFE94' to 0xC9, // ﺔ (Teh Marbuta Final)
        '\uFE97' to 0xCA, // ﺗ (Teh Initial)
        '\uFE98' to 0xCA, // ﺘ (Teh Medial)
        '\uFE9B' to 0xCB, // ﺛ (Theh Initial)
        '\uFE9C' to 0xCB, // ﺜ (Theh Medial)
        '\uFE9F' to 0xCC, // ﺟ (Jeem Initial)
        '\uFEA0' to 0xCC, // ﺠ (Jeem Medial)
        '\uFEA3' to 0xCD, // ﺣ (Hah Initial)
        '\uFEA4' to 0xCD, // ﺤ (Hah Medial)
        '\uFEA7' to 0xCE, // ﺧ (Khah Initial)
        '\uFEA8' to 0xCE, // ﺨ (Khah Medial)
        '\uFEA9' to 0xCF, // ﺩ (Dal Isolated)
        '\uFEAA' to 0xCF, // ﺪ (Dal Final)
        '\uFEAB' to 0xD0, // ﺫ (Thal Isolated)
        '\uFEAC' to 0xD0, // ﺬ (Thal Final)
        '\uFEAD' to 0xD1, // ﺭ (Reh Isolated)
        '\uFEAE' to 0xD1, // ﺮ (Reh Final)
        '\uFEAF' to 0xD2, // ﺯ (Zain Isolated)
        '\uFEB0' to 0xD2, // ﺰ (Zain Final)
        '\uFEB3' to 0xD3, // ﺳ (Seen Initial)
        '\uFEB4' to 0xD3, // ﺴ (Seen Medial)
        '\uFEB7' to 0xD4, // ﺷ (Sheen Initial)
        '\uFEB8' to 0xD4, // ﺸ (Sheen Medial)
        '\uFEBB' to 0xD5, // ﺻ (Sad Initial)
        '\uFEBC' to 0xD5, // ﺼ (Sad Medial)
        '\uFEBF' to 0xD6, // ﺿ (Dad Initial)
        '\uFEC0' to 0xD6, // ﻀ (Dad Medial)
        '\uFEC1' to 0xD7, // ﻁ (Tah Isolated)
        '\uFEC2' to 0xD7, // ﻂ (Tah Final)
        '\uFEC3' to 0xD7, // ﻃ (Tah Initial)
        '\uFEC4' to 0xD7, // ﻄ (Tah Medial)
        '\uFEC5' to 0xD8, // ﻅ (Zah Isolated)
        '\uFEC6' to 0xD8, // ﻆ (Zah Final)
        '\uFEC7' to 0xD8, // ﻇ (Zah Initial)
        '\uFEC8' to 0xD8, // ﻈ (Zah Medial)
        '\uFECB' to 0xD9, // ﻋ (Ain Initial)
        '\uFECF' to 0xDA, // ﻏ (Ghain Initial)
        '\u00F7' to 0xDD, // ÷
        '\u00D7' to 0xDE, // ×
        '\uFEC9' to 0xDF, // ﻉ (Ain Isolated)
        '\u0640' to 0xE0, // ـ (Tatweel)
        '\uFED3' to 0xE1, // ﻓ (Feh Initial)
        '\uFED4' to 0xE1, // ﻔ (Feh Medial)
        '\uFED7' to 0xE2, // ﻗ (Qaf Initial)
        '\uFED8' to 0xE2, // ﻘ (Qaf Medial)
        '\uFEDB' to 0xE3, // ﻛ (Kaf Initial)
        '\uFEDC' to 0xE3, // ﻜ (Kaf Medial)
        '\uFEDF' to 0xE4, // ﻟ (Lam Initial)
        '\uFEE0' to 0xE4, // ﻠ (Lam Medial)
        '\uFEE3' to 0xE5, // ﻣ (Meem Initial)
        '\uFEE4' to 0xE5, // ﻤ (Meem Medial)
        '\uFEE7' to 0xE6, // ﻧ (Noon Initial)
        '\uFEE8' to 0xE6, // ﻨ (Noon Medial)
        '\uFEEB' to 0xE7, // ﻫ (Heh Initial)
        '\uFEED' to 0xE8, // ﻭ (Waw Isolated)
        '\uFEEE' to 0xE8, // ﻮ (Waw Final)
        '\uFEEF' to 0xE9, // ﻯ (Alef Maksura Isolated)
        '\uFEF3' to 0xEA, // ﻳ (Yeh Initial)
        '\uFEF4' to 0xEA, // ﻴ (Yeh Medial)
        '\uFEBD' to 0xEB, // ﺽ (Dad Isolated)
        '\uFEBE' to 0xEB, // ﺾ (Dad Final)
        '\uFECC' to 0xEC, // ﻌ (Ain Medial)
        '\uFECE' to 0xED, // ﻎ (Ghain Final)
        '\uFECD' to 0xEE, // ﻍ (Ghain Isolated)
        '\uFEE1' to 0xEF, // ﻡ (Meem Isolated)
        '\uFEE2' to 0xEF, // ﻢ (Meem Final)
        '\uFEE5' to 0xF2, // ﻥ (Noon Isolated)
        '\uFEE6' to 0xF2, // ﻦ (Noon Final)
        '\uFEE9' to 0xF3, // ﻩ (Heh Isolated)
        '\uFEEA' to 0xF3, // ﻪ (Heh Final)
        '\uFEEC' to 0xF4, // ﻬ (Heh Medial)
        '\uFEF0' to 0xF5, // ﻰ (Alef Maksura Final)
        '\uFEF2' to 0xF6, // ﻲ (Yeh Final)
        '\uFED0' to 0xF7, // ﻐ (Ghain Medial)
        '\uFED5' to 0xF8, // ﻕ (Qaf Isolated)
        '\uFED6' to 0xF8, // ﻖ (Qaf Final)
        '\uFEF5' to 0xF9, // ﻵ (Lam-Alef with Madda Isolated)
        '\uFEF6' to 0xFA, // ﻶ (Lam-Alef with Madda Final)
        '\uFEDD' to 0xFB, // ﻝ (Lam Isolated)
        '\uFEDE' to 0xFB, // ﻞ (Lam Final)
        '\uFED9' to 0xFC, // ﻙ (Kaf Isolated)
        '\uFEDA' to 0xFC, // ﻚ (Kaf Final)
        '\uFEF1' to 0xFD, // ﻱ (Yeh Isolated)
        '\u25A0' to 0xFE  // ■
    )

    private fun encodeArabicChar(ch: Char, mode: CodepageMode): Byte {
        when (mode) {
            CodepageMode.CP864_PAGE_22, CodepageMode.CP864_PAGE_37 -> {
                val byteVal = CP864_BYTE_MAP[ch]
                if (byteVal != null) return byteVal.toByte()

                // Fallbacks
                if (ch.code < 128) return ch.code.toByte()
                return '?'.code.toByte()
            }
            CodepageMode.CP1256_PAGE_50, CodepageMode.CP1256_PAGE_32 -> {
                // In CP1256 mode, convert presentation form back to base Arabic character for CP1256
                val baseChar = presentationFormToBaseArabic(ch)
                val encoded = baseChar.toString().toByteArray(charset("windows-1256"))
                return if (encoded.isNotEmpty()) encoded[0] else '?'.code.toByte()
            }
        }
    }

    private fun encodeSymbol(ch: Char, mode: CodepageMode): Byte {
        return when (ch) {
            '×' -> if (mode == CodepageMode.CP864_PAGE_22 || mode == CodepageMode.CP864_PAGE_37) 0xDE.toByte() else 'x'.code.toByte()
            '«' -> if (mode == CodepageMode.CP864_PAGE_22 || mode == CodepageMode.CP864_PAGE_37) 0x97.toByte() else '"'.code.toByte()
            '»' -> if (mode == CodepageMode.CP864_PAGE_22 || mode == CodepageMode.CP864_PAGE_37) 0x98.toByte() else '"'.code.toByte()
            '✓' -> if (mode == CodepageMode.CP864_PAGE_22 || mode == CodepageMode.CP864_PAGE_37) 0xFB.toByte() else 'V'.code.toByte()
            '•' -> '*'.code.toByte()
            else -> if (ch.code < 128) ch.code.toByte() else ' '.code.toByte()
        }
    }

    private fun encodeLatinAscii(s: String, mode: CodepageMode): ByteArray {
        val bytes = ByteArray(s.length)
        for (i in s.indices) {
            val c = s[i]
            bytes[i] = if (c.code < 128) c.code.toByte() else encodeSymbol(c, mode)
        }
        return bytes
    }

    private fun presentationFormToBaseArabic(ch: Char): Char {
        return when (ch) {
            in '\uFE80'..'\uFE80' -> '\u0621'
            in '\uFE81'..'\uFE82' -> '\u0622'
            in '\uFE83'..'\uFE84' -> '\u0623'
            in '\uFE85'..'\uFE86' -> '\u0624'
            in '\uFE87'..'\uFE88' -> '\u0625'
            in '\uFE89'..'\uFE8C' -> '\u0626'
            in '\uFE8D'..'\uFE8E' -> '\u0627'
            in '\uFE8F'..'\uFE92' -> '\u0628'
            in '\uFE93'..'\uFE94' -> '\u0629'
            in '\uFE95'..'\uFE98' -> '\u062A'
            in '\uFE99'..'\uFE9C' -> '\u062B'
            in '\uFE9D'..'\uFEA0' -> '\u062C'
            in '\uFEA1'..'\uFEA4' -> '\u062D'
            in '\uFEA5'..'\uFEA8' -> '\u062E'
            in '\uFEA9'..'\uFEAA' -> '\u062F'
            in '\uFEAB'..'\uFEAC' -> '\u0630'
            in '\uFEAD'..'\uFEAE' -> '\u0631'
            in '\uFEAF'..'\uFEB0' -> '\u0632'
            in '\uFEB1'..'\uFEB4' -> '\u0633'
            in '\uFEB5'..'\uFEB8' -> '\u0634'
            in '\uFEB9'..'\uFEBC' -> '\u0635'
            in '\uFEBD'..'\uFEC0' -> '\u0636'
            in '\uFEC1'..'\uFEC4' -> '\u0637'
            in '\uFEC5'..'\uFEC8' -> '\u0638'
            in '\uFEC9'..'\uFECC' -> '\u0639'
            in '\uFECD'..'\uFED0' -> '\u063A'
            in '\uFED1'..'\uFED4' -> '\u0641'
            in '\uFED5'..'\uFED8' -> '\u0642'
            in '\uFED9'..'\uFEDC' -> '\u0643'
            in '\uFEDD'..'\uFEE0' -> '\u0644'
            in '\uFEE1'..'\uFEE4' -> '\u0645'
            in '\uFEE5'..'\uFEE8' -> '\u0646'
            in '\uFEE9'..'\uFEEC' -> '\u0647'
            in '\uFEED'..'\uFEEE' -> '\u0648'
            in '\uFEEF'..'\uFEF0' -> '\u0649'
            in '\uFEF1'..'\uFEF4' -> '\u064A'
            in '\uFEF5'..'\uFEFC' -> '\u0644' // Ligatures
            else -> ch
        }
    }
}
