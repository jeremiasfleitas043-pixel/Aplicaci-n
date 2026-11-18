package com.example.data.system

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object Formatters {
    private val spanishLocale = Locale("es", "ES")

    fun formatBytesOrUnavailable(bytes: Long?, unavailableLabel: String = "No disponible"): String {
        if (bytes == null || bytes < 0L) return unavailableLabel
        return formatBytes(bytes)
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        return when {
            bytes >= gb -> String.format(spanishLocale, "%.2f GB", bytes / gb)
            bytes >= mb -> String.format(spanishLocale, "%.1f MB", bytes / mb)
            bytes >= kb -> String.format(spanishLocale, "%.1f KB", bytes / kb)
            else -> "$bytes B"
        }
    }

    fun formatPercent(used: Long, total: Long): Int {
        if (total <= 0L) return 0
        return ((used.toDouble() / total.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
    }

    fun formatDateTime(timestampMillis: Long): String {
        val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", spanishLocale)
        return formatter.format(Date(timestampMillis))
    }

    fun formatDaysAgo(lastUsedMillis: Long?, nowMillis: Long = System.currentTimeMillis()): String {
        if (lastUsedMillis == null || lastUsedMillis <= 0L) return "Sin registro reciente"
        val diffMillis = (nowMillis - lastUsedMillis).coerceAtLeast(0L)
        val days = (diffMillis / (1000L * 60L * 60L * 24L)).toInt()
        return when {
            days == 0 -> "Usada hoy"
            days == 1 -> "Hace 1 día"
            else -> "Hace $days días"
        }
    }
}
