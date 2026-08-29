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
