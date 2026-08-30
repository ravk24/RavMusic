package com.ravk24.ravmusic.data.model

/** One search result: a playlist track and the name of the playlist it sits in. */
data class PlaylistSearchHit(val track: PlaylistTrack, val playlistName: String)

/**
 * Search across every playlist (change `search`): tracks whose title or artist contains [query],
 * ordered by playlist (home-grid order) then position. A song in two playlists is two hits. A
 * blank query yields nothing — the screen shows a hint instead of every track on the device.
 * Tracks whose playlist is not in [playlists] (deleted between emissions) are dropped.
 */
fun searchPlaylists(playlists: List<Playlist>, tracks: List<PlaylistTrack>, query: String): List<PlaylistSearchHit> {
    if (!isFiltering(query)) return emptyList()
    val order = playlists.withIndex().associate { (index, playlist) -> playlist.id to index }
    val names = playlists.associate { it.id to it.name }
    return tracks
        .filter { it.playlistId in names && matchesQuery(it.title, it.artist, query) }
        .sortedWith(compareBy({ order.getValue(it.playlistId) }, { it.position }, { it.id }))
        .map { PlaylistSearchHit(it, names.getValue(it.playlistId)) }
}
