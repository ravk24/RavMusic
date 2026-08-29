package com.ravk24.ravmusic.data.repo

import com.ravk24.ravmusic.data.mediastore.MediaScanner
import com.ravk24.ravmusic.data.model.buildLibrarySnapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Owns the in-memory result of the last library query. App-scoped (one instance in
 * `AppContainer`) so it survives tab switches and rotation; never written to disk (D-05).
 */
class LibraryRepository(
    private val scanner: MediaScanner,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _state = MutableStateFlow<LibraryState>(LibraryState.Idle)
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    private val scanMutex = Mutex()

    /** Queries the library only if nothing has been loaded yet. */
    suspend fun ensureLoaded() {
        if (_state.value is LibraryState.Idle) refresh()
    }

    /**
     * Re-queries the library. A previous result stays visible (marked refreshing) until the new
     * one arrives. Concurrent calls are serialised; the second one simply runs afterwards.
     */
    suspend fun refresh() {
        scanMutex.withLock {
            _state.value = when (val current = _state.value) {
                is LibraryState.Loaded -> current.copy(refreshing = true)
                else -> LibraryState.Loading
            }
            val songs = withContext(ioDispatcher) { scanner.scan() }
            _state.value = LibraryState.Loaded(buildLibrarySnapshot(songs, clock()))
        }
    }

    /** Forgets the last result, e.g. when the audio permission is revoked. */
    fun clear() {
        _state.value = LibraryState.Idle
    }
}
