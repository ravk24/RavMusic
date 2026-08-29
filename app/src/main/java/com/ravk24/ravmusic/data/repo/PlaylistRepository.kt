package com.ravk24.ravmusic.data.repo

import com.ravk24.ravmusic.data.db.PlaylistDao
import com.ravk24.ravmusic.data.db.PlaylistEntity
import com.ravk24.ravmusic.data.db.PlaylistTrackEntity
import com.ravk24.ravmusic.data.model.Playlist
import com.ravk24.ravmusic.data.model.PlaylistTrack
import com.ravk24.ravmusic.data.model.Song
import com.ravk24.ravmusic.data.model.moveItem
import com.ravk24.ravmusic.data.model.partitionDuplicates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** What the ViewModel needs from playlist storage; the Room-backed implementation is [PlaylistRepository]. */
interface PlaylistStore {
    val playlists: Flow<List<Playlist>>
    fun tracks(playlistId: Long): Flow<List<PlaylistTrack>>
    suspend fun create(name: String): Long
    suspend fun rename(playlistId: Long, name: String)
    suspend fun delete(playlistId: Long)
    suspend fun duplicateCount(playlistId: Long, songs: List<Song>): Int
    /** Appends [songs]; with [skipDuplicates] only songs not already present. Returns how many were added. */
    suspend fun addSongs(playlistId: Long, songs: List<Song>, skipDuplicates: Boolean): Int
    suspend fun removeTrack(trackId: Long)
    suspend fun move(playlistId: Long, from: Int, to: Int)
    suspend fun cleanUp(playlistId: Long, trackIds: Collection<Long>)
}

class PlaylistRepository(
    private val dao: PlaylistDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : PlaylistStore {

    override val playlists: Flow<List<Playlist>> = dao.observePlaylists().map { rows ->
        rows.map { Playlist(it.id, it.name, it.songCount, it.totalDurationMs, it.createdAt) }
    }

    override fun tracks(playlistId: Long): Flow<List<PlaylistTrack>> =
        dao.observeTracks(playlistId).map { rows -> rows.map { it.toModel() } }

    override suspend fun create(name: String): Long {
        val now = clock()
        return dao.insertPlaylist(
            PlaylistEntity(name = name.trim(), createdAt = now, sortOrder = dao.maxSortOrder() + 1),
        )
    }

    override suspend fun rename(playlistId: Long, name: String) = dao.rename(playlistId, name.trim())

    override suspend fun delete(playlistId: Long) = dao.deletePlaylist(playlistId)

    override suspend fun duplicateCount(playlistId: Long, songs: List<Song>): Int {
        if (songs.isEmpty()) return 0
        val existing = dao.existingUris(playlistId, songs.map { it.uri })
        return partitionDuplicates(songs, existing).duplicates.size
    }

    override suspend fun addSongs(playlistId: Long, songs: List<Song>, skipDuplicates: Boolean): Int {
        if (songs.isEmpty()) return 0
        val toAdd = if (skipDuplicates) {
            partitionDuplicates(songs, dao.existingUris(playlistId, songs.map { it.uri })).new
        } else {
            songs
        }
        if (toAdd.isEmpty()) return 0
        val start = dao.maxPosition(playlistId) + 1
        dao.insertTracks(
            toAdd.mapIndexed { i, song ->
                PlaylistTrackEntity(
                    playlistId = playlistId,
                    mediaStoreUri = song.uri,
                    title = song.title,
                    artist = song.artist,
                    durationMs = song.durationMs,
                    position = start + i,
                )
            },
        )
        return toAdd.size
    }

    override suspend fun removeTrack(trackId: Long) = dao.deleteTracks(listOf(trackId))

    override suspend fun move(playlistId: Long, from: Int, to: Int) {
        val current = dao.tracks(playlistId)
        val reordered = moveItem(current, from, to)
        if (reordered != current) dao.updatePositions(reordered.map { it.id })
    }

    override suspend fun cleanUp(playlistId: Long, trackIds: Collection<Long>) {
        if (trackIds.isNotEmpty()) dao.deleteTracks(trackIds.toList())
    }

    private fun PlaylistTrackEntity.toModel() =
        PlaylistTrack(id, playlistId, mediaStoreUri, title, artist, durationMs, position)
}
