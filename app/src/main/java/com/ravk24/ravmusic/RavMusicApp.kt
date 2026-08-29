package com.ravk24.ravmusic

import android.app.Application

/**
 * Application entry point. Owns the [AppContainer] so later phases (library, playback,
 * playlists) have a single, obvious place to construct their dependencies. Manual DI by design.
 */
class RavMusicApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
    }
}

/**
 * App-wide dependency container. Intentionally empty in the skeleton phase.
 */
class AppContainer
