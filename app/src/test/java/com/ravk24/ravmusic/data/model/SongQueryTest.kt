package com.ravk24.ravmusic.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SongQueryTest {

    private fun song(id: Long, title: String, artist: String? = null) =
        Song(id, "content://media/external/audio/media/$id", title, artist, 60_000L, "f", "Folder")

    private fun track(id: Long, title: String, artist: String? = null) =
        PlaylistTrack(id, 1L, "content://media/external/audio/media/$id", title, artist, 60_000L, id.toInt())

    @Test
    fun `matches title or artist, case-insensitively, anywhere in the text`() {
        assertTrue(matchesQuery("Glass Rain", "Hyaline", "glass"))
        assertTrue(matchesQuery("Glass Rain", "Hyaline", "RAIN"))
        assertTrue(matchesQuery("Glass Rain", "Hyaline", "yal"))
        assertFalse(matchesQuery("Glass Rain", "Hyaline", "paper"))
    }

    @Test
    fun `null artist only matches on the title`() {
        assertTrue(matchesQuery("Paper Boats", null, "boats"))
        assertFalse(matchesQuery("Paper Boats", null, "unknown"))
    }

    @Test
    fun `query is trimmed and a blank query matches everything`() {
        assertTrue(matchesQuery("Glass Rain", null, "  glass "))
        assertTrue(matchesQuery("anything", null, ""))
        assertTrue(matchesQuery("anything", null, "   "))
        assertFalse(isFiltering("   "))
        assertTrue(isFiltering(" a "))
        assertEquals("a b", normaliseQuery("  a b "))
    }

    @Test
    fun `matching keeps order and returns the same list for a blank query`() {
        val songs = listOf(song(1, "Midnight Freeway", "Nocturne Ave"), song(2, "Glass Rain", "Hyaline"), song(3, "Paper Boats"))
        assertSame(songs, songs.matching(" "))
        assertEquals(listOf(2L), songs.matching("rain").map { it.id })
        assertEquals(listOf(1L, 2L), songs.matching("n").filter { it.artist != null }.map { it.id })
        assertTrue(songs.matching("zzz").isEmpty())

        val tracks = listOf(track(11, "Midnight Freeway", "Nocturne Ave"), track(12, "Glass Rain", "Hyaline"))
        assertSame(tracks, tracks.matching(""))
        assertEquals(listOf(11L), tracks.matching("nocturne").map { it.id })
    }
}
