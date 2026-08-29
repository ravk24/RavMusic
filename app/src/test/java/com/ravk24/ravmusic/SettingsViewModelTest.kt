package com.ravk24.ravmusic

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.ravk24.ravmusic.data.mediastore.MIN_SONG_DURATION_MS
import com.ravk24.ravmusic.data.mediastore.MediaScanner
import com.ravk24.ravmusic.data.model.Song
import com.ravk24.ravmusic.data.repo.LibraryRepository
import com.ravk24.ravmusic.data.settings.SettingsRepository
import com.ravk24.ravmusic.data.settings.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcher = MainDispatcherRule(dispatcher)

    @get:Rule
    val tmp = TemporaryFolder()

    private val storeScope = CoroutineScope(SupervisorJob() + dispatcher)

    private class CountingScanner : MediaScanner {
        var calls = 0
        val thresholds = mutableListOf<Long>()
        override fun scan(minDurationMs: Long): List<Song> {
            calls++
            thresholds += minDurationMs
            return listOf(Song(1, "content://media/external/audio/media/1", "Song", null, 100_000L, "f", "Folder"))
        }
    }

    private fun settings() = SettingsRepository(
        PreferenceDataStoreFactory.create(scope = storeScope, produceFile = { File(tmp.root, "s.preferences_pb") }),
    )

    @After
    fun tearDown() = storeScope.cancel()

    @Test
    fun `initial values come from the store`() = runTest(dispatcher) {
        val repo = settings()
        repo.setThemeMode(ThemeMode.DARK)
        val vm = SettingsViewModel(repo, LibraryRepository(CountingScanner(), dispatcher))
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, vm.themeMode.value)
        assertEquals(MIN_SONG_DURATION_MS, vm.minDurationMs.value)
    }

    @Test
    fun `setThemeMode persists and is reflected`() = runTest(dispatcher) {
        val repo = settings()
        val vm = SettingsViewModel(repo, LibraryRepository(CountingScanner(), dispatcher))
        vm.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()
        assertEquals(ThemeMode.LIGHT, vm.themeMode.value)
        assertEquals(ThemeMode.LIGHT, repo.themeMode.first())
    }

    @Test
    fun `setMinDuration persists then re-queries the library exactly once with the new value`() = runTest(dispatcher) {
        val repo = settings()
        val scanner = CountingScanner()
        val library = LibraryRepository(scanner, dispatcher, minDurationMs = { repo.minDurationMs.first() })
        library.ensureLoaded()
        assertEquals(listOf(MIN_SONG_DURATION_MS), scanner.thresholds)

        val vm = SettingsViewModel(repo, library)
        vm.setMinDuration(15_000L)
        advanceUntilIdle()

        assertEquals(listOf(MIN_SONG_DURATION_MS, 15_000L), scanner.thresholds)
        assertEquals(2, scanner.calls)
        assertEquals(15_000L, vm.minDurationMs.value)
    }
}
