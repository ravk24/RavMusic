package com.ravk24.ravmusic.playback

import android.os.Bundle

/**
 * The session-level contract between the UI and the service for the sleep timer (design D2):
 * custom session commands in, session extras out.
 */
object SleepTimerCommands {
    const val SET = "com.ravk24.ravmusic.sleep.set"
    const val EXTEND = "com.ravk24.ravmusic.sleep.extend"
    const val CANCEL = "com.ravk24.ravmusic.sleep.cancel"

    /** Arg of [SET]: countdown length in millis. */
    const val ARG_DURATION_MS = "duration_ms"

    /** Arg of [SET]: true for "end of current track" (ignores [ARG_DURATION_MS]). */
    const val ARG_END_OF_TRACK = "end_of_track"

    /** Arg of [EXTEND]: how much to add, in millis. */
    const val ARG_EXTRA_MS = "extra_ms"

    /** Session extras keys the service publishes. */
    const val EXTRA_END_AT = "sleep_end_at"
    const val EXTRA_END_OF_TRACK = "sleep_end_of_track"

    /** Pure encoding used by both the Bundle mapping and the JVM tests. */
    data class Encoded(val endAtElapsedMs: Long?, val endOfTrack: Boolean)

    fun encode(state: SleepTimerState): Encoded = when (state) {
        SleepTimerState.Off -> Encoded(null, false)
        is SleepTimerState.Countdown -> Encoded(state.endAtElapsedMs, false)
        SleepTimerState.EndOfTrack -> Encoded(null, true)
    }

    fun decode(encoded: Encoded): SleepTimerState = when {
        encoded.endOfTrack -> SleepTimerState.EndOfTrack
        encoded.endAtElapsedMs != null && encoded.endAtElapsedMs > 0L -> SleepTimerState.Countdown(encoded.endAtElapsedMs)
        else -> SleepTimerState.Off
    }

    fun toExtras(state: SleepTimerState): Bundle {
        val e = encode(state)
        return Bundle().apply {
            if (e.endAtElapsedMs != null) putLong(EXTRA_END_AT, e.endAtElapsedMs)
            if (e.endOfTrack) putBoolean(EXTRA_END_OF_TRACK, true)
        }
    }

    fun fromExtras(extras: Bundle?): SleepTimerState {
        if (extras == null) return SleepTimerState.Off
        return decode(
            Encoded(
                endAtElapsedMs = if (extras.containsKey(EXTRA_END_AT)) extras.getLong(EXTRA_END_AT) else null,
                endOfTrack = extras.getBoolean(EXTRA_END_OF_TRACK, false),
            ),
        )
    }
}
