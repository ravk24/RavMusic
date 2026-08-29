package com.ravk24.ravmusic.playback

import com.ravk24.ravmusic.data.model.Song

/** A queue to hand to the player: songs in play order, where to start, and where it came from. */
data class QueuePlan(
    val songs: List<Song>,
    val startIndex: Int,
    val origin: String,
)

/**
 * Pure queue-building rule (spec F4: "queue = the folder that started playback"): the folder's
 * songs in their displayed order, starting at the tapped one. Returns null for an empty folder;
 * an out-of-range start index is clamped so a stale tap never crashes.
 */
fun planQueue(songs: List<Song>, startIndex: Int, origin: String): QueuePlan? {
    if (songs.isEmpty()) return null
    return QueuePlan(
        songs = songs.toList(),
        startIndex = startIndex.coerceIn(0, songs.lastIndex),
        origin = origin,
    )
}
