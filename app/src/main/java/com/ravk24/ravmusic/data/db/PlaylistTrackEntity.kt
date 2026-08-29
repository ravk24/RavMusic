package com.ravk24.ravmusic.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One song in a playlist. Title, artist and duration are snapshotted so the playlist renders
 * without querying MediaStore; [mediaStoreUri] is the stable content URI used to play it.
 */
@Entity(
    tableName = "playlist_tracks",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["playlistId", "position"])],
)
data class PlaylistTrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val playlistId: Long,
    val mediaStoreUri: String,
    val title: String,
    val artist: String?,
    val durationMs: Long,
    val position: Int,
)
