package com.ravk24.ravmusic.data.model

import com.ravk24.ravmusic.data.repo.LibraryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistModelsTest {

    private fun song(id: Long, title: String = "S$id") =
        Song(id, "content://media/external/audio/media/$id", title, null, 60_000L, "f", "Folder")

    private fun track(id: Long, songId: Long) =
        PlaylistTrack(id, 1L, "content://media/external/audio/media/$songId", "T$songId", null, 60_000L, id.toInt())

    @Test
    fun `toSong parses the MediaStore id from the uri`() {
        val song = track(10L, 42L).toSong()
        assertEquals(42L, song.id)
        assertEquals("content://media/external/audio/media/42", song.uri)
        assertEquals("T42", song.title)
        assertEquals(-1L, PlaylistTrack(1L, 1L, "file:///x/y.wav", "y", null, 1L, 0).mediaStoreId)
    }

    @Test
    fun `missing tracks are those absent from a loaded library`() {
        val tracks = listOf(track(1L, 1L), track(2L, 2L), track(3L, 3L))
        val loaded = LibraryState.Loaded(buildLibrarySnapshot(listOf(song(1L), song(3L)), 0L))
        assertEquals(setOf(2L), missingTrackIds(tracks, loaded))
    }

    @Test
    fun `nothing is missing while the library is not loaded`() {
        val tracks = listOf(track(1L, 1L))
        assertTrue(missingTrackIds(tracks, LibraryState.Idle).isEmpty())
        assertTrue(missingTrackIds(tracks, LibraryState.Loading).isEmpty())
    }

    @Test
    fun `planPlaylistPlay drops missing tracks and starts at the tapped uri`() {
        val tracks = listOf(track(1L, 1L), track(2L, 2L), track(3L, 3L))
        val plan = planPlaylistPlay(tracks, setOf(2L), "content://media/external/audio/media/3")!!
        assertEquals(listOf(1L, 3L), plan.songs.map { it.id })
        assertEquals(1, plan.startIndex)
        // Tapping a missing track, or nothing, starts at the beginning.
        assertEquals(0, planPlaylistPlay(tracks, setOf(2L), "content://media/external/audio/media/2")!!.startIndex)
        assertEquals(0, planPlaylistPlay(tracks, emptySet(), null)!!.startIndex)
        assertNull(planPlaylistPlay(tracks, setOf(1L, 2L, 3L), null))
        assertNull(planPlaylistPlay(emptyList(), emptySet(), null))
    }

    @Test
    fun `partitionDuplicates keeps order and splits by uri`() {
        val songs = listOf(song(1L), song(2L), song(3L), song(4L))
        val result = partitionDuplicates(songs, setOf("content://media/external/audio/media/2", "content://media/external/audio/media/4"))
        assertEquals(listOf(1L, 3L), result.new.map { it.id })
        assertEquals(listOf(2L, 4L), result.duplicates.map { it.id })
        assertTrue(partitionDuplicates(emptyList(), emptySet()).new.isEmpty())
    }

    @Test
    fun `moveItem moves within range and copies otherwise`() {
        val list = listOf("a", "b", "c", "d")
        assertEquals(listOf("b", "a", "c", "d"), moveItem(list, 1, 0))
        assertEquals(listOf("b", "c", "d", "a"), moveItem(list, 0, 3))
        assertEquals(listOf("a", "d", "b", "c"), moveItem(list, 3, 1))
        assertEquals(list, moveItem(list, 2, 2))
        assertEquals(list, moveItem(list, 9, 0))
        assertEquals(list, moveItem(list, 0, -1))
    }
}
