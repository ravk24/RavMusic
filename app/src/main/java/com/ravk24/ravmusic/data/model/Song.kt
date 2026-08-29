package com.ravk24.ravmusic.data.model

/**
 * One audio file from the device library.
 *
 * Pure Kotlin on purpose: [uri] is the MediaStore content URI as a string (not `android.net.Uri`,
 * which is a stub on the JVM) so the model and everything built on it is unit-testable. The
 * playback phase parses it at the player boundary; the playlists phase stores the same string.
 */
data class Song(
    val id: Long,
    val uri: String,
    val title: String,
    val artist: String?,
    val durationMs: Long,
    val folderId: String,
    val folderName: String,
)

/** MediaStore writes this literal for untagged artists. */
const val UNKNOWN_ARTIST_TAG = "<unknown>"

/** Normalises a raw MediaStore artist value: null, blank, or `<unknown>` become `null`. */
fun normaliseArtist(raw: String?): String? {
    val trimmed = raw?.trim().orEmpty()
    return if (trimmed.isEmpty() || trimmed == UNKNOWN_ARTIST_TAG) null else trimmed
}
