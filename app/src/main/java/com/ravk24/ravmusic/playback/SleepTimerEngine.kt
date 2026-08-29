package com.ravk24.ravmusic.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What the UI and the service know about the sleep timer. Times are elapsed-realtime millis. */
sealed interface SleepTimerState {
    data object Off : SleepTimerState
    data class Countdown(val endAtElapsedMs: Long) : SleepTimerState
    data object EndOfTrack : SleepTimerState
}

/** The two things the timer does to the player. Implemented over the ExoPlayer in the service. */
interface SleepTimerActions {
    var volume: Float
    fun pause()
}

const val SLEEP_FADE_MS = 10_000L
const val SLEEP_EXTEND_MS = 15 * 60_000L

/**
 * The sleep timer itself (spec F6, design D1): a countdown that fades the volume to zero over the
 * last [fadeMs], then pauses and restores the volume so resuming plays normally. Pure Kotlin over
 * an injectable clock so it is unit-tested with virtual time; the service hosts one instance.
 */
class SleepTimerEngine(
    private val actions: SleepTimerActions,
    private val scope: CoroutineScope,
    private val clock: () -> Long,
    private val fadeMs: Long = SLEEP_FADE_MS,
    private val stepMs: Long = 250L,
) {
    private val _state = MutableStateFlow<SleepTimerState>(SleepTimerState.Off)
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    private var job: Job? = null
    private var endAt = 0L

    /** Volume before the fade began, while a fade is in progress. */
    private var originalVolume: Float? = null

    /** Starts (or replaces) a countdown of [durationMs]. */
    fun set(durationMs: Long) {
        stopJob(restoreVolume = true)
        endAt = clock() + durationMs.coerceAtLeast(0L)
        start()
    }

    /** Pushes the end of a running countdown back by [extraMs]; no-op unless a countdown is active. */
    fun extend(extraMs: Long) {
        if (_state.value !is SleepTimerState.Countdown) return
        stopJob(restoreVolume = true)
        endAt += extraMs
        start()
    }

    /** Pause when the current song ends (see [onTrackEnded]). */
    fun endOfTrack() {
        stopJob(restoreVolume = true)
        _state.value = SleepTimerState.EndOfTrack
    }

    fun cancel() {
        stopJob(restoreVolume = true)
        _state.value = SleepTimerState.Off
    }

    /** Called by the host when the player moved to the next item by itself. */
    fun onTrackEnded() {
        if (_state.value is SleepTimerState.EndOfTrack) {
            actions.pause()
            _state.value = SleepTimerState.Off
        }
    }

    private fun start() {
        _state.value = SleepTimerState.Countdown(endAt)
        job = scope.launch { run() }
    }

    private suspend fun run() {
        val untilFade = endAt - fadeMs - clock()
        if (untilFade > 0) delay(untilFade)
        val original = actions.volume
        originalVolume = original
        while (true) {
            val remaining = endAt - clock()
            if (remaining <= 0) break
            actions.volume = original * (remaining.toFloat() / fadeMs).coerceIn(0f, 1f)
            delay(minOf(stepMs, remaining))
        }
        actions.volume = 0f
        actions.pause()
        actions.volume = original
        originalVolume = null
        job = null
        _state.value = SleepTimerState.Off
    }

    private fun stopJob(restoreVolume: Boolean) {
        job?.cancel()
        job = null
        if (restoreVolume) originalVolume?.let { actions.volume = it }
        originalVolume = null
    }
}
