package com.ravk24.ravmusic.ui

import com.ravk24.ravmusic.PlaylistsHost
import com.ravk24.ravmusic.data.model.Playlist
import com.ravk24.ravmusic.data.model.PlaylistTrack
import com.ravk24.ravmusic.data.model.Song
import com.ravk24.ravmusic.data.model.moveItem
import com.ravk24.ravmusic.data.model.partitionDuplicates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory [PlaylistsHost] with the same semantics as the Room-backed one, for UI tests. */
class FakePlaylistsHost : PlaylistsHost {

    private val lists = MutableStateFlow<List<Playlist>>(emptyList())
    private val trackFlows = HashMap<Long, MutableStateFlow<List<PlaylistTrack>>>()
    private val all = MutableStateFlow<List<PlaylistTrack>>(emptyList())
    private var nextId = 1L

    override val playlists: StateFlow<List<Playlist>> = lists

    override val allTracks: StateFlow<List<PlaylistTrack>> = all

    private fun flowFor(id: Long) = trackFlows.getOrPut(id) { MutableStateFlow(emptyList()) }

    override fun tracks(playlistId: Long): StateFlow<List<PlaylistTrack>> = flowFor(playlistId)

    /** Mirrors the Room query: every track, by playlist id then position. */
    private fun refreshAll() {
        all.value = trackFlows.entries.sortedBy { it.key }.flatMap { (_, flow) -> flow.value.sortedBy { it.position } }
    }

    private fun refreshSummary(id: Long) {
        val t = flowFor(id).value
        lists.value = lists.value.map { if (it.id == id) it.copy(songCount = t.size, totalDurationMs = t.sumOf { x -> x.durationMs }) else it }
        refreshAll()
    }

    fun seed(name: String, songs: List<Song> = emptyList()): Long {
        val id = nextId++
        lists.value = lists.value + Playlist(id, name, 0, 0L, id)
        flowFor(id).value = songs.mapIndexed { i, s -> PlaylistTrack(nextId++, id, s.uri, s.title, s.artist, s.durationMs, i) }
        refreshSummary(id)
        return id
    }

    override suspend fun create(name: String): Long = seed(name.trim())

    override fun rename(playlistId: Long, name: String) {
        lists.value = lists.value.map { if (it.id == playlistId) it.copy(name = name) else it }
    }

    override fun delete(playlistId: Long) {
        lists.value = lists.value.filterNot { it.id == playlistId }
        trackFlows.remove(playlistId)
        refreshAll()
    }

    override suspend fun duplicateCount(playlistId: Long, songs: List<Song>): Int =
        partitionDuplicates(songs, flowFor(playlistId).value.map { it.uri }).duplicates.size

    override suspend fun addSongs(playlistId: Long, songs: List<Song>, skipDuplicates: Boolean): Int {
        val flow = flowFor(playlistId)
        val current = flow.value
        val toAdd = if (skipDuplicates) partitionDuplicates(songs, current.map { it.uri }).new else songs
        flow.value = current + toAdd.mapIndexed { i, s -> PlaylistTrack(nextId++, playlistId, s.uri, s.title, s.artist, s.durationMs, current.size + i) }
        refreshSummary(playlistId)
        return toAdd.size
    }

    override fun removeTrack(trackId: Long) {
        trackFlows.forEach { (id, flow) ->
            if (flow.value.any { it.id == trackId }) {
                flow.value = flow.value.filterNot { it.id == trackId }
                refreshSummary(id)
            }
        }
    }

    override fun move(playlistId: Long, from: Int, to: Int) {
        val flow = flowFor(playlistId)
        flow.value = moveItem(flow.value, from, to).mapIndexed { i, t -> t.copy(position = i) }
        refreshAll()
    }

    override fun cleanUp(playlistId: Long, trackIds: Collection<Long>) {
        val flow = flowFor(playlistId)
        flow.value = flow.value.filterNot { it.id in trackIds }
        refreshSummary(playlistId)
    }
}
