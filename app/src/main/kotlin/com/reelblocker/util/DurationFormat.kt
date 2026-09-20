package com.reelblocker.util

object DurationFormat {
    /** e.g. 45_000 -> "0m", 125_000 -> "2m", 4_500_000 -> "1h 15m". */
    fun format(millis: Long): String {
        val totalMinutes = millis / 60_000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}
