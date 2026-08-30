package com.ravk24.ravmusic.data.model

/** The query as matched: surrounding whitespace is ignored. */
fun normaliseQuery(raw: String): String = raw.trim()

/** Whether a query narrows anything at all; blank queries show everything. */
fun isFiltering(query: String): Boolean = normaliseQuery(query).isNotEmpty()

/**
 * The one matching rule for every search in the app (change `search`): case-insensitive
 * "contains" on the title or the artist; a blank query matches every song.
 */
fun matchesQuery(title: String, artist: String?, query: String): Boolean {
    val q = normaliseQuery(query)
    return q.isEmpty() || title.contains(q, ignoreCase = true) || artist?.contains(q, ignoreCase = true) == true
}

/** Songs matching [query], in their original order; the same list when the query is blank. */
@JvmName("matchingSongs")
fun List<Song>.matching(query: String): List<Song> =
    if (!isFiltering(query)) this else filter { matchesQuery(it.title, it.artist, query) }

/** Playlist tracks matching [query], in playlist order; the same list when the query is blank. */
@JvmName("matchingTracks")
fun List<PlaylistTrack>.matching(query: String): List<PlaylistTrack> =
    if (!isFiltering(query)) this else filter { matchesQuery(it.title, it.artist, query) }
