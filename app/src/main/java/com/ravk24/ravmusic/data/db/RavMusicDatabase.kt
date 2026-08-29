package com.ravk24.ravmusic.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The app's only persisted data: playlists. The library itself is never cached (decision D-05).
 * Schema JSON is exported to `app/schemas/` so future versions can ship tested migrations.
 */
@Database(
    entities = [PlaylistEntity::class, PlaylistTrackEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class RavMusicDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao

    companion object {
        const val NAME = "ravmusic.db"

        fun build(context: Context): RavMusicDatabase =
            Room.databaseBuilder(context.applicationContext, RavMusicDatabase::class.java, NAME).build()
    }
}
