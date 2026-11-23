package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Universal 12-Hour Time utility for restaurant staff attendance, shifts, and timestamps.
 * Guarantees standard 12-hour format (AM/PM - ص / م) everywhere across the application.
 */
object Time12hUtil {

    /**
     * Formats a raw time string (whether 24h like "21:00" or already 12h) into clean 12h Arabic notation:
     * e.g., "09:00" -> "09:00 ص", "21:30" -> "09:30 م", "00:15" -> "12:15 ص"
     */
    fun format12h(timeStr: String?): String {
        if (timeStr == null) return "09:00 ص"
        val clean = timeStr.trim()
        if (clean.isBlank()) return "09:00 ص"
        if (clean.contains("ص") || clean.contains("م") || clean.contains("AM", ignoreCase = true) || clean.contains("PM", ignoreCase = true)) {
            return clean
        }
        val parts = clean.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return clean
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val isPm = h >= 12
        val h12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return String.format(Locale.US, "%02d:%02d %s", h12, m, if (isPm) "م" else "ص")
    }

    /**
     * Converts a timestamp in milliseconds into a 12-hour formatted time string:
     * e.g., "02:45 م"
     */
    fun formatTimestamp12h(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val rawH = cal.get(Calendar.HOUR)
        val h12 = if (rawH == 0) 12 else rawH
        val m = cal.get(Calendar.MINUTE)
        val isPm = cal.get(Calendar.AM_PM) == Calendar.PM
        return String.format(Locale.US, "%02d:%02d %s", h12, m, if (isPm) "م" else "ص")
    }

    /**
     * Converts a timestamp into full date + 12h time string:
     * e.g., "2026-10-17 02:45 م"
     */
    fun formatTimestampWithDate12h(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        return "$dateStr ${formatTimestamp12h(millis)}"
    }

    /**
     * Parses a 12-hour or 24-hour time string into total minutes from start of day (0..1439).
     * Handles both "09:30 ص" and "21:30" and "09:30 PM".
     */
    fun parseTimeToMinutes12h(timeStr: String?): Int {
        if (timeStr == null) return 540 // 09:00 AM default
        val clean = timeStr.trim()
        if (clean.isBlank()) return 540
        val isPm = clean.contains("م") || clean.contains("PM", ignoreCase = true) || clean.contains("مساء", ignoreCase = true)
        val isAm = clean.contains("ص") || clean.contains("AM", ignoreCase = true) || clean.contains("صباح", ignoreCase = true)
        val numbers = clean.replace("[^0-9:]".toRegex(), "").split(":")
        var h = numbers.getOrNull(0)?.toIntOrNull() ?: 0
        val m = numbers.getOrNull(1)?.toIntOrNull() ?: 0

        if (isPm) {
            if (h < 12) h += 12
        } else if (isAm) {
            if (h == 12) h = 0
        }
        return (h * 60 + m).coerceIn(0, 1439)
    }

    /**
     * Builds a standard 12-hour formatted string from hour12 (1..12), minute (0..59), and isPm boolean.
     */
    fun build12hString(hour12: Int, minute: Int, isPm: Boolean): String {
        val hClamped = hour12.coerceIn(1, 12)
        val mClamped = minute.coerceIn(0, 59)
        return String.format(Locale.US, "%02d:%02d %s", hClamped, mClamped, if (isPm) "م" else "ص")
    }
}
