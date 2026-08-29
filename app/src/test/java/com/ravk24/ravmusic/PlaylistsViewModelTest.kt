package com.ravk24.ravmusic

import com.ravk24.ravmusic.data.model.Playlist
import com.ravk24.ravmusic.data.model.PlaylistTrack
import com.ravk24.ravmusic.data.model.Song
import com.ravk24.ravmusic.data.model.moveItem
import com.ravk24.ravmusic.data.model.partitionDuplicates
import com.ravk24.ravmusic.data.repo.PlaylistStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcher = MainDispatcherRule(dispatcher)

    /** In-memory store with the same semantics as the Room one. */
    private class FakeStore : PlaylistStore {
        val lists = MutableStateFlow<List<Playlist>>(emptyList())
        val trackMap = MutableStateFlow<Map<Long, List<PlaylistTrack>>>(emptyMap())
        private var nextId = 1L

        override val playlists: Flow<List<Playlist>> = lists
        override fun tracks(playlistId: Long): Flow<List<PlaylistTrack>> = trackMap.map { it[playlistId].orEmpty() }
        override suspend fun create(name: String): Long {
            val id = nextId++
            lists.value = lists.value + Playlist(id, name.trim(), 0, 0L, id)
            return id
        }
        override suspend fun rename(playlistId: Long, name: String) {
            lists.value = lists.value.map { if (it.id == playlistId) it.copy(name = name) else it }
        }
        override suspend fun delete(playlistId: Long) {
            lists.value = lists.value.filterNot { it.id == playlistId }
        }
        override suspend fun duplicateCount(playlistId: Long, songs: List<Song>): Int =
            partitionDuplicates(songs, trackMap.value[playlistId].orEmpty().map { it.uri }).duplicates.size
        override suspend fun addSongs(playlistId: Long, songs: List<Song>, skipDuplicates: Boolean): Int {
            val current = trackMap.value[playlistId].orEmpty()
            val toAdd = if (skipDuplicates) partitionDuplicates(songs, current.map { it.uri }).new else songs
            val added = toAdd.mapIndexed { i, s ->
                PlaylistTrack(nextId++, playlistId, s.uri, s.title, s.artist, s.durationMs, current.size + i)
            }
            trackMap.value = trackMap.value + (playlistId to current + added)
            return added.size
        }
        override suspend fun removeTrack(trackId: Long) {
            trackMap.value = trackMap.value.mapValues { (_, v) -> v.filterNot { it.id == trackId } }
        }
        override suspend fun move(playlistId: Long, from: Int, to: Int) {
            trackMap.value = trackMap.value + (playlistId to moveItem(trackMap.value[playlistId].orEmpty(), from, to))
        }
        override suspend fun cleanUp(playlistId: Long, trackIds: Collection<Long>) {
            trackMap.value = trackMap.value + (playlistId to trackMap.value[playlistId].orEmpty().filterNot { it.id in trackIds })
        }
    }

    private fun song(id: Long) = Song(id, "content://media/external/audio/media/$id", "S$id", null, 60_000L, "f", "F")

    /**
     * The ViewModel's own scope on the test scheduler: its launches are foreground events (so
     * `advanceUntilIdle` runs them) without being children of `runTest`, which would otherwise
     * wait for the `stateIn` sharing coroutine forever. Cancelled at the end of each test.
     */
    private fun vmScope() = CoroutineScope(SupervisorJob() + dispatcher)

    @Test
    fun `create returns the id and addSongs skips duplicates when asked`() = runTest(dispatcher) {
        val store = FakeStore()
        val scope = vmScope()
        val vm = PlaylistsViewModel(store, scope = scope)
        val id = vm.create("  Late night ")
        assertEquals("Late night", store.lists.value.single().name)

        assertEquals(3, vm.addSongs(id, listOf(song(1), song(2), song(3)), skipDuplicates = false))
        assertEquals(2, vm.duplicateCount(id, listOf(song(2), song(3), song(4))))
        assertEquals(1, vm.addSongs(id, listOf(song(2), song(3), song(4)), skipDuplicates = true))
        assertEquals(listOf(1L, 2L, 3L, 4L), store.trackMap.value[id]!!.map { it.mediaStoreId })

        assertEquals(3, vm.addSongs(id, listOf(song(2), song(3), song(4)), skipDuplicates = false))
        assertEquals(7, store.trackMap.value[id]!!.size)
        scope.cancel()
    }

    @Test
    fun `move remove cleanUp rename delete reach the store`() = runTest(dispatcher) {
        val store = FakeStore()
        val scope = vmScope()
        val vm = PlaylistsViewModel(store, scope = scope)
        val id = vm.create("Focus")
        vm.addSongs(id, listOf(song(1), song(2), song(3)), skipDuplicates = false)
        val ids = store.trackMap.value[id]!!.map { it.id }

        vm.move(id, 2, 0); testScheduler.advanceUntilIdle()
        assertEquals(listOf(3L, 1L, 2L), store.trackMap.value[id]!!.map { it.mediaStoreId })

        vm.removeTrack(ids[0]); testScheduler.advanceUntilIdle()
        assertEquals(listOf(3L, 2L), store.trackMap.value[id]!!.map { it.mediaStoreId })

        vm.cleanUp(id, listOf(ids[2])); testScheduler.advanceUntilIdle()
        assertEquals(listOf(2L), store.trackMap.value[id]!!.map { it.mediaStoreId })

        vm.rename(id, "Deep focus"); testScheduler.advanceUntilIdle()
        assertEquals("Deep focus", store.lists.value.single().name)

        vm.delete(id); testScheduler.advanceUntilIdle()
        assertEquals(0, store.lists.value.size)
        scope.cancel()
    }
}
