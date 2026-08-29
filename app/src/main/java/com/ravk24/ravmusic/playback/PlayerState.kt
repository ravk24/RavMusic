package com.ravk24.ravmusic.playback

/** The song the session is on, with the label of where the queue came from ("Rock"). */
data class NowPlaying(
    val songId: Long,
    val title: String,
    val artist: String?,
    val origin: String,
)

/** Repeat cycle Off → All → One → Off (spec F5). App-level enum, mapped to Media3 at the controller boundary. */
enum class RepeatMode {
    OFF, ALL, ONE;

    fun next(): RepeatMode = when (this) {
        OFF -> ALL
        ALL -> ONE
        ONE -> OFF
    }
}

/** One song in the queue, in play order. [mediaIndex] is its index in the session's item list. */
data class QueueEntry(
    val songId: Long,
    val title: String,
    val artist: String?,
    val mediaIndex: Int,
)

/**
 * A file the service could not open and skipped (spec F1 edge case). [seq] increments per event
 * so the UI can show the same title twice; it is not persisted anywhere.
 */
data class SkipNotice(val title: String, val seq: Int)

/**
 * What the UI knows about the player. `nowPlaying == null` means no queue is loaded, which is
 * exactly when the mini player is hidden. [queue] is in the order songs will play (shuffled
 * order when shuffle is on) and [queueIndex] is the current song's position in it.
 * [skipped] is the last skip notice, if any, since the connection was made.
 */
data class PlayerState(
    val nowPlaying: NowPlaying? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<QueueEntry> = emptyList(),
    val queueIndex: Int = -1,
    val sleepTimer: SleepTimerState = SleepTimerState.Off,
    val skipped: SkipNotice? = null,
) {
    val hasQueue: Boolean get() = nowPlaying != null

    /** 0f..1f for the progress line; 0f while the duration is unknown. */
    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    /** Songs after the current one in play order ("Queue · N left"). */
    val remaining: Int get() = if (queueIndex < 0) 0 else (queue.size - queueIndex - 1).coerceAtLeast(0)

    val hasNext: Boolean get() = queueIndex >= 0 && queueIndex < queue.lastIndex

    val hasPrevious: Boolean get() = queueIndex > 0
}
