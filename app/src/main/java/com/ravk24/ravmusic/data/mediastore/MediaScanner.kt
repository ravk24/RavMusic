package com.ravk24.ravmusic.data.mediastore

import com.ravk24.ravmusic.data.model.Song

/**
 * Default "Skip short audio" threshold: notification sounds, voice notes and the like are hidden
 * (spec F1). The value actually applied comes from Settings at query time (design D2 of `polish`).
 */
const val MIN_SONG_DURATION_MS = 30_000L

/**
 * Source of songs. The real one queries MediaStore; tests supply a lambda. Blocking by design.
 * [minDurationMs] is the threshold to apply to this query; 0 hides nothing.
 */
fun interface MediaScanner {
    fun scan(minDurationMs: Long): List<Song>
}
