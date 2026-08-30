package com.ravk24.ravmusic.data.model

import com.ravk24.ravmusic.data.repo.LibraryState

/** A playlist as the home grid shows it. */
data class Playlist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val totalDurationMs: Long,
    val createdAt: Long,
)

/** One row of a playlist; metadata is the snapshot taken when the song was added. */
data class PlaylistTrack(
    val id: Long,
    val playlistId: Long,
    val uri: String,
    val title: String,
    val artist: String?,
    val durationMs: Long,
    val position: Int,
) {
    /** The MediaStore id encoded in the content URI's last segment, or -1 if it is not numeric. */
    val mediaStoreId: Long get() = uri.substringAfterLast('/').toLongOrNull() ?: -1L

    /** Adapts a track to the playback path, which speaks [Song]. Folder fields are irrelevant here. */
    fun toSong(): Song = Song(
        id = mediaStoreId,
        uri = uri,
        title = title,
        artist = artist,
        durationMs = durationMs,
        folderId = "",
        folderName = "",
    )
}

/**
 * Missing = the library is loaded and the track's URI is not in it (design D3). While the library
 * is not loaded nothing is flagged, so a fresh launch never greys rows by mistake.
 */
fun missingTrackIds(tracks: List<PlaylistTrack>, library: LibraryState): Set<Long> {
    val snapshot = (library as? LibraryState.Loaded)?.snapshot ?: return emptySet()
    val present = snapshot.songs.mapTo(HashSet()) { it.uri }
    return tracks.filter { it.uri !in present }.mapTo(HashSet()) { it.id }
}

/** What playing a playlist hands to the player: the playable songs in playlist order and where to start. */
data class PlaylistPlayPlan(val songs: List<Song>, val startIndex: Int)

/**
 * The one rule for playing a playlist (spec "Playing a playlist"), shared by the detail screen and
 * search results: missing tracks are dropped, the queue starts at the first track whose URI is
 * [tappedUri] (or at 0 when nothing / a missing track was tapped), and an all-missing or empty
 * playlist gives null.
 */
fun planPlaylistPlay(tracks: List<PlaylistTrack>, missing: Set<Long>, tappedUri: String?): PlaylistPlayPlan? {
    val songs = tracks.filter { it.id !in missing }.map { it.toSong() }
    if (songs.isEmpty()) return null
    val start = songs.indexOfFirst { it.uri == tappedUri }.coerceAtLeast(0)
    return PlaylistPlayPlan(songs, start)
}

/** Songs already in the playlist (by URI) versus the ones that would be new. Order is preserved. */
data class DuplicatePartition(val new: List<Song>, val duplicates: List<Song>)

fun partitionDuplicates(songs: List<Song>, existingUris: Collection<String>): DuplicatePartition {
    val existing = existingUris.toHashSet()
    val (dupes, fresh) = songs.partition { it.uri in existing }
    return DuplicatePartition(new = fresh, duplicates = dupes)
}

/** Moves the element at [from] to [to] (indices in the original list); out-of-range → unchanged copy. */
fun <T> moveItem(list: List<T>, from: Int, to: Int): List<T> {
    if (from !in list.indices || to !in list.indices || from == to) return list.toList()
    val result = list.toMutableList()
    val item = result.removeAt(from)
    result.add(to, item)
    return result
}
