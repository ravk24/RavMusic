package com.ravk24.ravmusic.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistSearchTest {

    private val playlists = listOf(
        Playlist(2L, "Focus", 2, 120_000L, 0L),
        Playlist(1L, "Late night", 2, 120_000L, 0L),
    )

    private fun track(id: Long, playlistId: Long, position: Int, title: String, artist: String? = null) =
        PlaylistTrack(id, playlistId, "content://media/external/audio/media/$id", title, artist, 60_000L, position)

    private val tracks = listOf(
        track(10, 1L, 0, "Glass Rain", "Hyaline"),
        track(11, 1L, 1, "Paper Boats"),
        track(20, 2L, 1, "Rain Check", "Nocturne Ave"),
        track(21, 2L, 0, "Glass Rain", "Hyaline"),
        track(30, 9L, 0, "Rain of a deleted playlist"),
    )

    @Test
    fun `blank query yields nothing`() {
        assertTrue(searchPlaylists(playlists, tracks, "").isEmpty())
        assertTrue(searchPlaylists(playlists, tracks, "   ").isEmpty())
    }

    @Test
    fun `hits carry the playlist name and follow grid order then position`() {
        val hits = searchPlaylists(playlists, tracks, "rain")
        assertEquals(listOf(21L, 20L, 10L), hits.map { it.track.id })
        assertEquals(listOf("Focus", "Focus", "Late night"), hits.map { it.playlistName })
    }

    @Test
    fun `the same song in two playlists is two hits and orphans are dropped`() {
        val hits = searchPlaylists(playlists, tracks, "glass")
        assertEquals(listOf(21L, 10L), hits.map { it.track.id })
        assertTrue(searchPlaylists(playlists, tracks, "deleted").isEmpty())
    }

    @Test
    fun `artist matches too and no match is empty`() {
        assertEquals(listOf(20L), searchPlaylists(playlists, tracks, "nocturne").map { it.track.id })
        assertTrue(searchPlaylists(playlists, tracks, "zzz").isEmpty())
    }
}
