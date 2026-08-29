package com.ravk24.ravmusic.playback

import com.ravk24.ravmusic.data.model.Song

/**
 * Everything the shell can ask the player to do, bundled so `AppNavigation` takes one value
 * (design D5). Built from `PlayerViewModel` in `AppRoot`; tests start from [none] and copy in
 * the lambdas they care about.
 */
data class PlayerActions(
    val onPlayPause: () -> Unit,
    val onDismiss: () -> Unit,
    val onPlaySongs: (songs: List<Song>, index: Int, origin: String) -> Unit,
    val onShufflePlay: (songs: List<Song>, origin: String) -> Unit,
    val onSeek: (positionMs: Long) -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onToggleShuffle: () -> Unit,
    val onCycleRepeat: () -> Unit,
    val onJumpTo: (queuePosition: Int) -> Unit,
    val onMoveInQueue: (from: Int, to: Int) -> Unit,
    val onRefreshPosition: () -> Unit,
    val onSetSleepTimer: (durationMs: Long) -> Unit,
    val onSleepEndOfTrack: () -> Unit,
    val onExtendSleepTimer: (extraMs: Long) -> Unit,
    val onCancelSleepTimer: () -> Unit,
) {
    companion object {
        fun none() = PlayerActions(
            onPlayPause = {},
            onDismiss = {},
            onPlaySongs = { _, _, _ -> },
            onShufflePlay = { _, _ -> },
            onSeek = {},
            onNext = {},
            onPrevious = {},
            onToggleShuffle = {},
            onCycleRepeat = {},
            onJumpTo = {},
            onMoveInQueue = { _, _ -> },
            onRefreshPosition = {},
            onSetSleepTimer = {},
            onSleepEndOfTrack = {},
            onExtendSleepTimer = {},
            onCancelSleepTimer = {},
        )
    }
}
