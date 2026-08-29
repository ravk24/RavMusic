package com.ravk24.ravmusic.data.repo

import com.ravk24.ravmusic.data.mediastore.MIN_SONG_DURATION_MS
import com.ravk24.ravmusic.data.mediastore.MediaScanner
import com.ravk24.ravmusic.data.model.Song
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryRepositoryTest {

    /** Records the repository state and the threshold observed at the moment each scan runs. */
    private class CountingScanner(var songs: List<Song>) : MediaScanner {
        var calls = 0
        var stateDuringScan: () -> LibraryState = { LibraryState.Idle }
        val observed = mutableListOf<LibraryState>()
        val thresholds = mutableListOf<Long>()
        override fun scan(minDurationMs: Long): List<Song> {
            calls++
            observed += stateDuringScan()
            thresholds += minDurationMs
            return songs
        }
    }

    private fun song(id: Long, title: String = "T$id") = Song(
        id = id, uri = "content://media/external/audio/media/$id", title = title, artist = null,
        durationMs = 100_000L, folderId = "f", folderName = "Folder",
    )

    @Test
    fun `starts idle`() = runTest {
        val repo = LibraryRepository(CountingScanner(emptyList()), StandardTestDispatcher(testScheduler))
        assertEquals(LibraryState.Idle, repo.state.value)
    }

    @Test
    fun `ensureLoaded goes Loading then Loaded`() = runTest {
        val scanner = CountingScanner(listOf(song(1)))
        var clock = 100L
        val repo = LibraryRepository(scanner, StandardTestDispatcher(testScheduler), clock = { clock })
        scanner.stateDuringScan = { repo.state.value }
        assertEquals(LibraryState.Idle, repo.state.value)

        repo.ensureLoaded()

        assertEquals(listOf<LibraryState>(LibraryState.Loading), scanner.observed)
        val loaded = repo.state.value as LibraryState.Loaded
        assertEquals(1, loaded.snapshot.totalSongs)
        assertEquals(100L, loaded.snapshot.scannedAt)
        assertEquals(false, loaded.refreshing)
        assertEquals(1, scanner.calls)
    }

    @Test
    fun `second ensureLoaded does not rescan`() = runTest {
        val scanner = CountingScanner(listOf(song(1)))
        val repo = LibraryRepository(scanner, StandardTestDispatcher(testScheduler))
        repo.ensureLoaded()
        repo.ensureLoaded()
        assertEquals(1, scanner.calls)
    }

    @Test
    fun `refresh keeps the old snapshot visible while refreshing`() = runTest {
        val scanner = CountingScanner(listOf(song(1)))
        val repo = LibraryRepository(scanner, StandardTestDispatcher(testScheduler))
        scanner.stateDuringScan = { repo.state.value }
        repo.ensureLoaded()

        scanner.songs = listOf(song(1), song(2))
        repo.refresh()

        val during = scanner.observed[1] as LibraryState.Loaded
        assertTrue(during.refreshing)
        assertEquals(1, during.snapshot.totalSongs)
        val after = repo.state.value as LibraryState.Loaded
        assertEquals(false, after.refreshing)
        assertEquals(2, after.snapshot.totalSongs)
        assertEquals(2, scanner.calls)
    }

    @Test
    fun `clear returns to Idle and ensureLoaded rescans afterwards`() = runTest {
        val scanner = CountingScanner(listOf(song(1)))
        val repo = LibraryRepository(scanner, StandardTestDispatcher(testScheduler))
        repo.ensureLoaded()
        repo.clear()
        assertEquals(LibraryState.Idle, repo.state.value)
        repo.ensureLoaded()
        assertEquals(2, scanner.calls)
    }

    @Test
    fun `threshold is read on every query and recorded on the snapshot`() = runTest {
        val scanner = CountingScanner(listOf(song(1)))
        var threshold = 15_000L
        val repo = LibraryRepository(scanner, StandardTestDispatcher(testScheduler), minDurationMs = { threshold })

        repo.ensureLoaded()
        assertEquals(listOf(15_000L), scanner.thresholds)
        assertEquals(15_000L, (repo.state.value as LibraryState.Loaded).snapshot.minDurationMs)

        threshold = 0L
        repo.refresh()
        assertEquals(listOf(15_000L, 0L), scanner.thresholds)
        assertEquals(0L, (repo.state.value as LibraryState.Loaded).snapshot.minDurationMs)
    }

    @Test
    fun `default threshold is the 30 s constant`() = runTest {
        val scanner = CountingScanner(listOf(song(1)))
        val repo = LibraryRepository(scanner, StandardTestDispatcher(testScheduler))
        repo.ensureLoaded()
        assertEquals(listOf(MIN_SONG_DURATION_MS), scanner.thresholds)
    }

    @Test
    fun `refreshIfLoaded only re-queries a loaded library`() = runTest {
        val scanner = CountingScanner(listOf(song(1)))
        val repo = LibraryRepository(scanner, StandardTestDispatcher(testScheduler))

        repo.refreshIfLoaded()
        assertEquals(0, scanner.calls)
        assertEquals(LibraryState.Idle, repo.state.value)

        repo.ensureLoaded()
        repo.refreshIfLoaded()
        assertEquals(2, scanner.calls)
    }
}
