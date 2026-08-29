package com.ravk24.ravmusic.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.ravk24.ravmusic.data.mediastore.MIN_SONG_DURATION_MS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** The real repository over a real DataStore file in a temp folder — no Android needed. */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun file() = File(tmp.root, "settings.preferences_pb")

    /** One DataStore per scope; the scope must be cancelled before the same file is reopened. */
    private fun TestScope.store(file: File = file()): Pair<DataStore<Preferences>, CoroutineScope> {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }) to scope
    }

    @Test
    fun `defaults are system theme and the 30 s threshold`() = runTest {
        val (ds, scope) = store()
        val repo = SettingsRepository(ds)
        assertEquals(ThemeMode.SYSTEM, repo.themeMode.first())
        assertEquals(MIN_SONG_DURATION_MS, repo.minDurationMs.first())
        scope.cancel()
    }

    @Test
    fun `writes round-trip and negative thresholds clamp to zero`() = runTest {
        val (ds, scope) = store()
        val repo = SettingsRepository(ds)
        repo.setThemeMode(ThemeMode.DARK)
        repo.setMinDuration(15_000L)
        assertEquals(ThemeMode.DARK, repo.themeMode.first())
        assertEquals(15_000L, repo.minDurationMs.first())
        repo.setMinDuration(-5L)
        assertEquals(0L, repo.minDurationMs.first())
        scope.cancel()
    }

    @Test
    fun `values survive reopening the same file`() = runTest {
        val (first, scope1) = store()
        SettingsRepository(first).apply {
            setThemeMode(ThemeMode.LIGHT)
            setMinDuration(120_000L)
        }
        scope1.cancel()

        val (second, scope2) = store()
        val reopened = SettingsRepository(second)
        assertEquals(ThemeMode.LIGHT, reopened.themeMode.first())
        assertEquals(120_000L, reopened.minDurationMs.first())
        scope2.cancel()
    }
}
