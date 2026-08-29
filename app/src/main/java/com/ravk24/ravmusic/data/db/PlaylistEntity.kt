package com.ravk24.ravmusic.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A user-created playlist (spec F3). Tracks live in [PlaylistTrackEntity]. */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdAt: Long,
    /** Reserved for a user-controlled home order; creation order for now. */
    val sortOrder: Long,
)
