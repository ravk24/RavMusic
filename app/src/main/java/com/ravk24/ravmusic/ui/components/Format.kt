package com.ravk24.ravmusic.ui.components

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
