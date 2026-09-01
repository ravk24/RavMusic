package com.ravk24.ravmusic.ui.nowplaying

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import android.os.SystemClock
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ravk24.ravmusic.playback.NowPlaying
import com.ravk24.ravmusic.playback.PlayerActions
import com.ravk24.ravmusic.playback.PlayerState
import com.ravk24.ravmusic.playback.QueueEntry
import com.ravk24.ravmusic.playback.RepeatMode
import com.ravk24.ravmusic.playback.SLEEP_EXTEND_MS
import com.ravk24.ravmusic.playback.SleepTimerState
import com.ravk24.ravmusic.ui.components.AppIcons
import com.ravk24.ravmusic.ui.components.UNKNOWN_ARTIST_LABEL
import com.ravk24.ravmusic.ui.components.formatDuration
import com.ravk24.ravmusic.ui.components.formatRemaining
import com.ravk24.ravmusic.ui.theme.RavMusicTheme
import kotlinx.coroutines.delay

/** One size for shuffle / previous / next / repeat; the play/pause circle stays the big one. */
private val SIDE_ICON_DP = 28.dp
private val CHIP_ICON_DP = 14.dp

/**
 * The full-screen player (design canvas artboards 1f / 1k). Everything shown comes from
 * [state]; every control goes through [actions]. Runs a 250 ms position ticker while playing
 * (design D6) and hosts the queue sheet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NowPlayingScreen(
    state: PlayerState,
    actions: PlayerActions,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = state.nowPlaying
    var scrub by remember { mutableStateOf<Float?>(null) }
    var queueOpen by rememberSaveable { mutableStateOf(false) }
    var sleepOpen by rememberSaveable { mutableStateOf(false) }
    var equalizerOpen by rememberSaveable { mutableStateOf(false) }

    // Tick once a second while a countdown runs so the chip keeps counting even when paused.
    val timer = state.sleepTimer
    var nowTick by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(timer is SleepTimerState.Countdown) {
        if (timer is SleepTimerState.Countdown) {
            while (true) {
                nowTick = SystemClock.elapsedRealtime()
                delay(1_000)
            }
        }
    }
    val remainingMs: Long? = (timer as? SleepTimerState.Countdown)?.let { (it.endAtElapsedMs - nowTick).coerceAtLeast(0L) }

    LaunchedEffect(state.isPlaying) {
        if (state.isPlaying) {
            while (true) {
                delay(250)
                actions.onRefreshPosition()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .testTag("screen_now_playing"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCollapse, modifier = Modifier.testTag("np_collapse")) {
                Icon(AppIcons.ExpandMore, contentDescription = "Collapse")
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Playing from",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = now?.origin?.ifBlank { "Queue" } ?: "",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("np_origin"),
                )
            }
            IconButton(onClick = { queueOpen = true }, modifier = Modifier.testTag("np_queue_icon")) {
                Icon(AppIcons.QueueMusic, contentDescription = "Queue")
            }
        }

        // Free space above the song details keeps the controls anchored to the lower half;
        // it shrinks first on short screens so the transport row and the chips keep their room.
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = now?.title ?: "",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("np_title"),
        )
        Text(
            text = now?.let { it.artist ?: UNKNOWN_ARTIST_LABEL } ?: "",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = 4.dp)
                .testTag("np_artist"),
        )

        Spacer(modifier = Modifier.height(16.dp))

        val duration = state.durationMs
        val fraction = scrub ?: state.progress
        Slider(
            value = fraction,
            onValueChange = { scrub = it },
            onValueChangeFinished = {
                scrub?.let { actions.onSeek((it * duration).toLong()) }
                scrub = null
            },
            enabled = duration > 0L,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("np_seek"),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val elapsedMs = scrub?.let { (it * duration).toLong() } ?: state.positionMs
            Text(
                text = formatDuration(elapsedMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("np_elapsed"),
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("np_total"),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            IconButton(onClick = actions.onToggleShuffle, modifier = Modifier.testTag("np_shuffle")) {
                Icon(
                    imageVector = AppIcons.Shuffle,
                    contentDescription = if (state.shuffleEnabled) "Shuffle on" else "Shuffle off",
                    tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(SIDE_ICON_DP),
                )
            }
            IconButton(onClick = actions.onPrevious, modifier = Modifier.testTag("np_prev")) {
                Icon(AppIcons.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(SIDE_ICON_DP))
            }
            IconButton(
                onClick = actions.onPlayPause,
                modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .testTag("np_play_pause"),
            ) {
                Icon(
                    imageVector = if (state.isPlaying) AppIcons.Pause else AppIcons.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(34.dp),
                )
            }
            IconButton(onClick = actions.onNext, modifier = Modifier.testTag("np_next")) {
                Icon(AppIcons.SkipNext, contentDescription = "Next", modifier = Modifier.size(SIDE_ICON_DP))
            }
            IconButton(onClick = actions.onCycleRepeat, modifier = Modifier.testTag("np_repeat")) {
                Icon(
                    imageVector = if (state.repeatMode == RepeatMode.ONE) AppIcons.RepeatOne else AppIcons.Repeat,
                    contentDescription = when (state.repeatMode) {
                        RepeatMode.OFF -> "Repeat off"
                        RepeatMode.ALL -> "Repeat all"
                        RepeatMode.ONE -> "Repeat one"
                    },
                    tint = if (state.repeatMode == RepeatMode.OFF) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(SIDE_ICON_DP),
                )
            }
        }

        Spacer(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 16.dp),
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            val sleepActive = timer !is SleepTimerState.Off
            AssistChip(
                onClick = { sleepOpen = true },
                enabled = state.hasQueue,
                label = {
                    Text(
                        text = when (timer) {
                            SleepTimerState.Off -> "Sleep timer"
                            is SleepTimerState.Countdown -> "Sleep · ${formatRemaining(remainingMs ?: 0L)} · tap to extend"
                            SleepTimerState.EndOfTrack -> "Sleep · end of track"
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                leadingIcon = { Icon(AppIcons.Bedtime, contentDescription = null, modifier = Modifier.size(CHIP_ICON_DP)) },
                colors = if (sleepActive) {
                    AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    AssistChipDefaults.assistChipColors()
                },
                border = if (sleepActive) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                } else {
                    AssistChipDefaults.assistChipBorder(enabled = state.hasQueue)
                },
                modifier = Modifier.testTag("np_sleep_chip"),
            )
            AssistChip(
                onClick = { queueOpen = true },
                label = { Text("Queue · ${state.remaining} left", style = MaterialTheme.typography.labelMedium) },
                leadingIcon = { Icon(AppIcons.QueueMusic, contentDescription = null, modifier = Modifier.size(CHIP_ICON_DP)) },
                modifier = Modifier.testTag("np_queue_chip"),
            )
            AssistChip(
                onClick = { equalizerOpen = true },
                label = { Text("Equalizer", style = MaterialTheme.typography.labelMedium) },
                leadingIcon = { Icon(AppIcons.GraphicEq, contentDescription = null, modifier = Modifier.size(CHIP_ICON_DP)) },
                modifier = Modifier.testTag("np_eq_chip"),
            )
        }
    }

    if (equalizerOpen) {
        EqualizerSheet(onDismiss = { equalizerOpen = false })
    }

    if (sleepOpen) {
        SleepTimerSheet(
            state = timer,
            remainingMs = remainingMs,
            onPreset = { minutes -> actions.onSetSleepTimer(minutes * 60_000L) },
            onCustom = { minutes -> actions.onSetSleepTimer(minutes * 60_000L) },
            onEndOfTrack = actions.onSleepEndOfTrack,
            onExtend = { actions.onExtendSleepTimer(SLEEP_EXTEND_MS) },
            onCancel = actions.onCancelSleepTimer,
            onDismiss = { sleepOpen = false },
        )
    }

    if (queueOpen) {
        QueueSheet(
            state = state,
            onJump = { position -> actions.onJumpTo(position) },
            onMove = actions.onMoveInQueue,
            onDismiss = { queueOpen = false },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NowPlayingPreview() {
    RavMusicTheme {
        NowPlayingScreen(
            state = PlayerState(
                nowPlaying = NowPlaying(4L, "Copper Sky", "Nocturne Ave", "Late night"),
                isPlaying = true,
                positionMs = 118_000L,
                durationMs = 312_000L,
                shuffleEnabled = true,
                repeatMode = RepeatMode.ALL,
                queue = (0..41).map { QueueEntry(it.toLong(), "Song $it", null, it) },
                queueIndex = 2,
            ),
            actions = PlayerActions.none(),
            onCollapse = {},
        )
    }
}
