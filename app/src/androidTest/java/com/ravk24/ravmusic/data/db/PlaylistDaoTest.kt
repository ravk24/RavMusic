package com.ravk24.ravmusic.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravk24.ravmusic.data.model.Song
import com.ravk24.ravmusic.data.repo.PlaylistRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistDaoTest {

    private lateinit var db: RavMusicDatabase
    private lateinit var repo: PlaylistRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), RavMusicDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        var clock = 1_000L
        repo = PlaylistRepository(db.playlistDao(), clock = { clock++ })
    }

    @After
    fun tearDown() = db.close()

    private fun song(id: Long, title: String = "S$id", durationMs: Long = 60_000L) =
        Song(id, "content://media/external/audio/media/$id", title, if (id % 2 == 0L) "Artist $id" else null, durationMs, "f", "Folder")

    @Test
    fun createRenameDeleteCascade() = runBlocking {
        val id = repo.create("  Late night ")
        repo.addSongs(id, listOf(song(1), song(2)), skipDuplicates = false)
        var lists = repo.playlists.first()
        assertEquals("Late night", lists.single().name)
        assertEquals(2, lists.single().songCount)
        assertEquals(120_000L, lists.single().totalDurationMs)

        repo.rename(id, "Late nights")
        assertEquals("Late nights", repo.playlists.first().single().name)

        repo.delete(id)
        lists = repo.playlists.first()
        assertTrue(lists.isEmpty())
        assertTrue(db.playlistDao().tracks(id).isEmpty())
    }

    @Test
    fun tracksAppendInOrderAndSnapshotMetadata() = runBlocking {
        val id = repo.create("Focus")
        repo.addSongs(id, listOf(song(3, "Third"), song(1, "First")), skipDuplicates = false)
        repo.addSongs(id, listOf(song(2, "Second")), skipDuplicates = false)
        val tracks = repo.tracks(id).first()
        assertEquals(listOf("Third", "First", "Second"), tracks.map { it.title })
        assertEquals(listOf(0, 1, 2), tracks.map { it.position })
        assertEquals("Artist 2", tracks[2].artist)
        assertEquals(null, tracks[1].artist)
        assertEquals(3L, tracks[0].mediaStoreId)
    }

    @Test
    fun duplicatesAreDetectedAndSkippable() = runBlocking {
        val id = repo.create("Road trip")
        repo.addSongs(id, listOf(song(1), song(2), song(3)), skipDuplicates = false)
        assertEquals(2, repo.duplicateCount(id, listOf(song(2), song(3), song(4))))
        assertEquals(1, repo.addSongs(id, listOf(song(2), song(3), song(4)), skipDuplicates = true))
        assertEquals(listOf(1L, 2L, 3L, 4L), repo.tracks(id).first().map { it.mediaStoreId })
        assertEquals(3, repo.addSongs(id, listOf(song(2), song(3), song(4)), skipDuplicates = false))
        assertEquals(7, repo.tracks(id).first().size)
    }

    @Test
    fun moveRemoveCleanUpPersistOrder() = runBlocking {
        val id = repo.create("Order")
        repo.addSongs(id, listOf(song(1), song(2), song(3), song(4)), skipDuplicates = false)
        repo.move(id, 3, 0)
        assertEquals(listOf(4L, 1L, 2L, 3L), repo.tracks(id).first().map { it.mediaStoreId })
        repo.move(id, 0, 2)
        val afterMoves = repo.tracks(id).first()
        assertEquals(listOf(1L, 2L, 4L, 3L), afterMoves.map { it.mediaStoreId })
        assertEquals(listOf(0, 1, 2, 3), afterMoves.map { it.position })

        repo.removeTrack(afterMoves[1].id)
        assertEquals(listOf(1L, 4L, 3L), repo.tracks(id).first().map { it.mediaStoreId })

        val remaining = repo.tracks(id).first()
        repo.cleanUp(id, listOf(remaining[0].id, remaining[2].id))
        assertEquals(listOf(4L), repo.tracks(id).first().map { it.mediaStoreId })
        assertEquals(1, repo.playlists.first().single().songCount)
    }

    @Test
    fun playlistsOrderedByCreation() = runBlocking {
        repo.create("B")
        repo.create("A")
        repo.create("C")
        assertEquals(listOf("B", "A", "C"), repo.playlists.first().map { it.name })
    }
}
