package com.ravk24.ravmusic.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibrarySnapshotTest {

    private fun song(id: Long, title: String, folderId: String, folderName: String) = Song(
        id = id,
        uri = "content://media/external/audio/media/$id",
        title = title,
        artist = null,
        durationMs = 200_000L,
        folderId = folderId,
        folderName = folderName,
    )

    private val songs = listOf(
        song(1, "Zebra", "m", "Music"),
        song(2, "apple", "m", "Music"),
        song(3, "Copper Sky", "d", "download"),
        song(4, "Beta Song", "r", "Rock"),
        song(5, "Alpha", "b", "beta"),
        song(6, "Omega", "a", "Alpha"),
    )

    @Test
    fun `folders are grouped by id with counts`() {
        val snapshot = buildLibrarySnapshot(songs, scannedAt = 42L)
        assertEquals(5, snapshot.folders.size)
        assertEquals(2, snapshot.folders.first { it.id == "m" }.songCount)
        assertEquals("Music", snapshot.folders.first { it.id == "m" }.name)
        assertEquals(6, snapshot.totalSongs)
        assertEquals(42L, snapshot.scannedAt)
    }

    @Test
    fun `folders are sorted by name ignoring case`() {
        val names = buildLibrarySnapshot(songs, 0L).folders.map { it.name }
        assertEquals(listOf("Alpha", "beta", "download", "Music", "Rock"), names)
    }

    @Test
    fun `songs within a folder are sorted by title ignoring case`() {
        val titles = buildLibrarySnapshot(songs, 0L).songsIn("m").map { it.title }
        assertEquals(listOf("apple", "Zebra"), titles)
    }

    @Test
    fun `songsIn unknown folder is empty`() {
        assertTrue(buildLibrarySnapshot(songs, 0L).songsIn("nope").isEmpty())
    }

    @Test
    fun `empty input gives empty snapshot`() {
        val snapshot = buildLibrarySnapshot(emptyList(), 7L)
        assertTrue(snapshot.songs.isEmpty())
        assertTrue(snapshot.folders.isEmpty())
        assertEquals(0, snapshot.totalSongs)
    }

    @Test
    fun `equal titles fall back to id order`() {
        val dupes = listOf(song(9, "Same", "m", "Music"), song(3, "same", "m", "Music"))
        assertEquals(listOf(3L, 9L), buildLibrarySnapshot(dupes, 0L).songs.map { it.id })
    }
}
