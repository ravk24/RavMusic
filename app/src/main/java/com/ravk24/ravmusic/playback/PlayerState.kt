package com.ravk24.ravmusic.playback

/** The song the session is on, with the label of where the queue came from ("Rock"). */
data class NowPlaying(
    val songId: Long,
    val title: String,
    val artist: String?,
    val origin: String,
)

/**
 * What the UI knows about the player. `nowPlaying == null` means no queue is loaded, which is
 * exactly when the mini player is hidden. Pure Kotlin; produced by the controller bridge.
 */
data class PlayerState(
    val nowPlaying: NowPlaying? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
) {
    val hasQueue: Boolean get() = nowPlaying != null

    /** 0f..1f for the progress line; 0f while the duration is unknown. */
    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}
