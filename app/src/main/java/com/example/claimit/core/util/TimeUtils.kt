package com.example.claimit.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.exp
import kotlin.math.ln

object TimeUtils {

    /**
     * Calculates time decay score e^(-lambda * delta_hours).
     * Score is 1.0 for same hour, decaying gracefully over days.
     */
    fun calculateTimeDecay(timestampMs1: Long, timestampMs2: Long, halfLifeHours: Double = 48.0): Float {
        val diffHours = Math.abs(timestampMs1 - timestampMs2) / (1000.0 * 60.0 * 60.0)
        val lambda = ln(2.0) / halfLifeHours
        return exp(-lambda * diffHours).toFloat().coerceIn(0.0f, 1.0f)
    }

    fun formatRelativeTime(timestampMs: Long): String {
        val now = System.currentTimeMillis()
        val diffMs = now - timestampMs
        val diffMinutes = diffMs / (1000 * 60)
        val diffHours = diffMinutes / 60
        val diffDays = diffHours / 24

        return when {
            diffMinutes < 1 -> "Just now"
            diffMinutes < 60 -> "${diffMinutes}m ago"
            diffHours < 24 -> "${diffHours}h ago"
            diffDays < 7 -> "${diffDays}d ago"
            else -> {
                val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US)
                sdf.format(Date(timestampMs))
            }
        }
    }

    fun formatTrackingTimestamp(timestampMs: Long): String {
        val sdf = SimpleDateFormat("yyyy.MM.dd-HH:mm:ss", Locale.US)
        return sdf.format(Date(timestampMs))
    }
}
