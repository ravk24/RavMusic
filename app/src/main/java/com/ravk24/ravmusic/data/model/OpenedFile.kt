package com.ravk24.ravmusic.data.model

/** Queue origin label for a file opened from outside the app (file manager, share sheet). */
const val OPENED_FILE_ORIGIN = "Opened file"

/** Title used when nothing usable can be derived from the file. */
const val UNKNOWN_TITLE = "Unknown title"

private const val ACTION_VIEW = "android.intent.action.VIEW"
private const val ACTION_SEND = "android.intent.action.SEND"

/**
 * An "open this audio file" request as handed to the app by the system. Pure data: the URI as a
 * string, the declared MIME type (may be null) and a sequence number so the same file opened twice
 * in a row is two distinct requests.
 */
data class OpenRequest(val uri: String, val mimeType: String?, val seq: Int)

/**
 * Picks the URI an incoming intent is about, from its plain-string parts: `VIEW` carries it as the
 * intent data, `SEND` as the `EXTRA_STREAM` extra; both may fall back to the first `ClipData` item.
 * Anything else (the launcher's `MAIN`, an unknown action, a blank URI) is not a request.
 */
fun openRequestUri(action: String?, data: String?, stream: String?, clip: String?): String? {
    val chosen = when (action) {
        ACTION_VIEW -> data.nonBlank() ?: clip.nonBlank()
        ACTION_SEND -> stream.nonBlank() ?: clip.nonBlank()
        else -> null
    }
    return chosen
}

private fun String?.nonBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

/**
 * A display title from a file name: the last extension is dropped only when a stem remains
 * (`song.mp3` → `song`, `a.b.c.flac` → `a.b.c`, `.hidden` stays). A blank name, or one that is
 * only digits (a bare MediaStore id), gives [UNKNOWN_TITLE].
 */
fun titleFromFileName(name: String?): String {
    val trimmed = name?.trim().orEmpty()
    if (trimmed.isEmpty() || trimmed.all { it.isDigit() }) return UNKNOWN_TITLE
    val dot = trimmed.lastIndexOf('.')
    return if (dot > 0) trimmed.substring(0, dot) else trimmed
}

/**
 * A stable id for a file that has no MediaStore row. Always `<= -2`: MediaStore ids are positive
 * and `-1` is the "not a MediaStore URI" sentinel used by playlist tracks, so an opened file can
 * never be mistaken for a library or playlist song when the current-song row is highlighted.
 */
fun syntheticSongId(uri: String): Long = -((uri.hashCode().toLong() and 0x7fffffffL) + 2L)

/** A [Song] for a file outside the library; folder fields are irrelevant and left empty. */
fun openedSong(uri: String, title: String, artist: String? = null, durationMs: Long = 0L): Song = Song(
    id = syntheticSongId(uri),
    uri = uri,
    title = title,
    artist = normaliseArtist(artist),
    durationMs = durationMs,
    folderId = "",
    folderName = "",
)

/** A resolved open request: the song to play and the request's sequence number. */
data class OpenedFile(val song: Song, val seq: Int)
