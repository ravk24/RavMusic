package com.ravk24.ravmusic

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.ravk24.ravmusic.data.db.RavMusicDatabase
import com.ravk24.ravmusic.data.mediastore.MediaStoreScanner
import com.ravk24.ravmusic.data.mediastore.UriSongResolver
import com.ravk24.ravmusic.data.repo.LibraryRepository
import com.ravk24.ravmusic.data.repo.PlaylistRepository
import com.ravk24.ravmusic.data.settings.SettingsRepository
import com.ravk24.ravmusic.playback.PlayerConnection
import kotlinx.coroutines.flow.first

/**
 * Application entry point. Owns the [AppContainer] so every phase (library, playback,
 * playlists, settings) has a single, obvious place to construct its dependencies. Manual DI by design.
 */
class RavMusicApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** The one Preferences DataStore file ("settings.preferences_pb"); a process-wide singleton by contract. */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** App-wide dependency container: one instance per process, created lazily on first use. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    /** Persisted preferences: theme override and the short-audio threshold. */
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext.settingsDataStore) }

    /**
     * The in-memory library (last MediaStore query). App-scoped so it outlives screens and tabs.
     * Every query reads the current threshold from Settings (design D2 of `polish`).
     */
    val libraryRepository: LibraryRepository by lazy {
        LibraryRepository(
            scanner = MediaStoreScanner(appContext.contentResolver),
            minDurationMs = { settingsRepository.minDurationMs.first() },
        )
    }

    /** Resolves a file opened from outside the app (`open-with`) into a playable song. */
    val uriSongResolver: UriSongResolver by lazy { UriSongResolver(appContext.contentResolver) }

    /** The UI's client of the playback service. One per process; connected by the player ViewModel. */
    val playerConnection: PlayerConnection by lazy { PlayerConnection(appContext) }

    /** Persisted playlists (Room). Opened lazily on first use. */
    val database: RavMusicDatabase by lazy { RavMusicDatabase.build(appContext) }

    val playlistRepository: PlaylistRepository by lazy { PlaylistRepository(database.playlistDao()) }
}
