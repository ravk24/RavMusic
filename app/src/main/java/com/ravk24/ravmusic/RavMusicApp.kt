package com.ravk24.ravmusic

import android.app.Application
import android.content.Context
import com.ravk24.ravmusic.data.mediastore.MediaStoreScanner
import com.ravk24.ravmusic.data.repo.LibraryRepository

/**
 * Application entry point. Owns the [AppContainer] so every phase (library, playback,
 * playlists) has a single, obvious place to construct its dependencies. Manual DI by design.
 */
class RavMusicApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** App-wide dependency container: one instance per process, created lazily on first use. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    /** The in-memory library (last MediaStore query). App-scoped so it outlives screens and tabs. */
    val libraryRepository: LibraryRepository by lazy {
        LibraryRepository(MediaStoreScanner(appContext.contentResolver))
    }
}
