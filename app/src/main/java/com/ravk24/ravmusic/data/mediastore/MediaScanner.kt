package com.ravk24.ravmusic.data.mediastore

import com.ravk24.ravmusic.data.model.Song

/**
 * Audio shorter than this is hidden from the library: notification sounds, voice notes and the
 * like (spec F1). A single constant for now; the Settings change turns it into a preference.
 */
const val MIN_SONG_DURATION_MS = 30_000L

/** Source of songs. The real one queries MediaStore; tests supply a lambda. Blocking by design. */
fun interface MediaScanner {
    fun scan(): List<Song>
}
