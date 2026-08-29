package com.ravk24.ravmusic.ui.components

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "3:41", or "1:02:05" from an hour upwards; anything negative is "0:00". */
fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

/** "1 song" / "572 songs". */
fun songCountLabel(count: Int): String = if (count == 1) "1 song" else "$count songs"

/** Playlist totals: "2h 58m", "51m", "0m"; seconds are dropped. */
fun formatTotalDuration(ms: Long): String {
    val totalMinutes = (ms / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

/** "1 playlist" / "6 playlists". */
fun playlistCountLabel(count: Int): String = if (count == 1) "1 playlist" else "$count playlists"

/** Short-audio threshold as shown in Settings and the Folders footer: "Off", "15s", "30s", "1 min", "2 min". */
fun thresholdLabel(ms: Long): String = when {
    ms <= 0L -> "Off"
    ms < 60_000L -> "${ms / 1000L}s"
    else -> "${ms / 60_000L} min"
}

/**
 * Relative time of the last library query: "just now" under a minute, "N min ago" under an hour,
 * "N h ago" under a day, otherwise the calendar date ("29 Aug"). [zone] is injectable for tests.
 */
fun formatScanTime(scannedAt: Long, now: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    val elapsed = (now - scannedAt).coerceAtLeast(0L)
    return when {
        elapsed < 60_000L -> "just now"
        elapsed < 3_600_000L -> "${elapsed / 60_000L} min ago"
        elapsed < 86_400_000L -> "${elapsed / 3_600_000L} h ago"
        else -> Instant.ofEpochMilli(scannedAt).atZone(zone).format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))
    }
}

/** Countdown label: "32:14", "02:10", "1:02:14"; rounds up so a fresh 15-minute timer reads 15:00. */
fun formatRemaining(ms: Long): String {
    val totalSeconds = ((ms.coerceAtLeast(0L) + 999L) / 1000L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
}
