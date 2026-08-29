package com.ravk24.ravmusic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravk24.ravmusic.data.model.Playlist
import com.ravk24.ravmusic.data.model.PlaylistTrack
import com.ravk24.ravmusic.data.model.Song
import com.ravk24.ravmusic.data.repo.PlaylistStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Everything the shell needs from playlists, as one interface so tests can pass a fake or a real
 * ViewModel over an in-memory database (design D4).
 */
interface PlaylistsHost {
    val playlists: StateFlow<List<Playlist>>
    fun tracks(playlistId: Long): StateFlow<List<PlaylistTrack>>
    suspend fun create(name: String): Long
    fun rename(playlistId: Long, name: String)
    fun delete(playlistId: Long)
    suspend fun duplicateCount(playlistId: Long, songs: List<Song>): Int
    suspend fun addSongs(playlistId: Long, songs: List<Song>, skipDuplicates: Boolean): Int
    fun removeTrack(trackId: Long)
    fun move(playlistId: Long, from: Int, to: Int)
    fun cleanUp(playlistId: Long, trackIds: Collection<Long>)
}

/** A host with no playlists and no effects — for previews and screens rendered without the shell. */
object NoPlaylists : PlaylistsHost {
    private val none = kotlinx.coroutines.flow.MutableStateFlow<List<Playlist>>(emptyList())
    private val noTracks = kotlinx.coroutines.flow.MutableStateFlow<List<PlaylistTrack>>(emptyList())
    override val playlists: StateFlow<List<Playlist>> = none
    override fun tracks(playlistId: Long): StateFlow<List<PlaylistTrack>> = noTracks
    override suspend fun create(name: String): Long = 0L
    override fun rename(playlistId: Long, name: String) = Unit
    override fun delete(playlistId: Long) = Unit
    override suspend fun duplicateCount(playlistId: Long, songs: List<Song>): Int = 0
    override suspend fun addSongs(playlistId: Long, songs: List<Song>, skipDuplicates: Boolean): Int = 0
    override fun removeTrack(trackId: Long) = Unit
    override fun move(playlistId: Long, from: Int, to: Int) = Unit
    override fun cleanUp(playlistId: Long, trackIds: Collection<Long>) = Unit
}

/** Activity-scoped; the store's flows are cheap Room queries, cached per playlist id. */
class PlaylistsViewModel(
    private val store: PlaylistStore,
    scope: CoroutineScope? = null,
) : ViewModel(), PlaylistsHost {

    private val scope: CoroutineScope = scope ?: viewModelScope

    override val playlists: StateFlow<List<Playlist>> =
        store.playlists.stateIn(this.scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val trackFlows = HashMap<Long, StateFlow<List<PlaylistTrack>>>()

    override fun tracks(playlistId: Long): StateFlow<List<PlaylistTrack>> =
        trackFlows.getOrPut(playlistId) {
            store.tracks(playlistId).stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    override suspend fun create(name: String): Long = store.create(name)

    override fun rename(playlistId: Long, name: String) {
        scope.launch { store.rename(playlistId, name) }
    }

    override fun delete(playlistId: Long) {
        scope.launch { store.delete(playlistId) }
    }

    override suspend fun duplicateCount(playlistId: Long, songs: List<Song>): Int =
        store.duplicateCount(playlistId, songs)

    override suspend fun addSongs(playlistId: Long, songs: List<Song>, skipDuplicates: Boolean): Int =
        store.addSongs(playlistId, songs, skipDuplicates)

    override fun removeTrack(trackId: Long) {
        scope.launch { store.removeTrack(trackId) }
    }

    override fun move(playlistId: Long, from: Int, to: Int) {
        scope.launch { store.move(playlistId, from, to) }
    }

    override fun cleanUp(playlistId: Long, trackIds: Collection<Long>) {
        scope.launch { store.cleanUp(playlistId, trackIds) }
    }
}
