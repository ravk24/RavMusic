package com.ravk24.ravmusic.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** A playlist with its aggregate count and duration, as read by the home grid. */
data class PlaylistSummaryRow(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val sortOrder: Long,
    val songCount: Int,
    val totalDurationMs: Long,
)

@Dao
interface PlaylistDao {

    @Query(
        """
        SELECT p.id, p.name, p.createdAt, p.sortOrder,
               COUNT(t.id) AS songCount,
               COALESCE(SUM(t.durationMs), 0) AS totalDurationMs
        FROM playlists p LEFT JOIN playlist_tracks t ON t.playlistId = p.id
        GROUP BY p.id
        ORDER BY p.sortOrder ASC, p.id ASC
        """,
    )
    fun observePlaylists(): Flow<List<PlaylistSummaryRow>>

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC, id ASC")
    fun observeTracks(playlistId: Long): Flow<List<PlaylistTrackEntity>>

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC, id ASC")
    suspend fun tracks(playlistId: Long): List<PlaylistTrackEntity>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun playlist(id: Long): PlaylistEntity?

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM playlists")
    suspend fun maxSortOrder(): Long

    @Query("SELECT DISTINCT mediaStoreUri FROM playlist_tracks WHERE playlistId = :playlistId AND mediaStoreUri IN (:uris)")
    suspend fun existingUris(playlistId: Long, uris: List<String>): List<String>

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun maxPosition(playlistId: Long): Int

    @Insert
    suspend fun insertTracks(tracks: List<PlaylistTrackEntity>)

    @Query("DELETE FROM playlist_tracks WHERE id IN (:ids)")
    suspend fun deleteTracks(ids: List<Long>)

    @Query("UPDATE playlist_tracks SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)

    /** Rewrites positions 0..n-1 in the given order. */
    @Transaction
    suspend fun updatePositions(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> updatePosition(id, index) }
    }
}
