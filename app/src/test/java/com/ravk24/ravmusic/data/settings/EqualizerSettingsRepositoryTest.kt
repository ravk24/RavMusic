package com.ravk24.ravmusic.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
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
class EqualizerSettingsRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun file() = File(tmp.root, "settings.preferences_pb")

    /** One DataStore per scope; the scope must be cancelled before the same file is reopened. */
    private fun TestScope.store(file: File = file()): Pair<DataStore<Preferences>, CoroutineScope> {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }) to scope
    }

    @Test
    fun `defaults are effects off with no preset and no levels`() = runTest {
        val (ds, scope) = store()
        assertEquals(EqualizerSettings(), EqualizerSettingsRepository(ds).settings.first())
        scope.cancel()
    }

    @Test
    fun `a snapshot round-trips and strengths clamp on the way in`() = runTest {
        val (ds, scope) = store()
        val repo = EqualizerSettingsRepository(ds)
        repo.save(
            EqualizerSettings(
                enabled = true,
                presetIndex = 3,
                bandLevels = listOf(300, 0, -200, 0, 150),
                bassBoost = 400,
                virtualizer = 9999,
            ),
        )
        val read = repo.settings.first()
        assertEquals(true, read.enabled)
        assertEquals(3, read.presetIndex)
        assertEquals(listOf(300, 0, -200, 0, 150), read.bandLevels)
        assertEquals(400, read.bassBoost)
        assertEquals(EQ_MAX_STRENGTH, read.virtualizer)
        scope.cancel()
    }

    @Test
    fun `values survive reopening the same file`() = runTest {
        val saved = EqualizerSettings(enabled = true, bandLevels = listOf(-100, 0, 100), bassBoost = 250)
        val (first, scope1) = store()
        EqualizerSettingsRepository(first).save(saved)
        scope1.cancel()

        val (second, scope2) = store()
        assertEquals(saved, EqualizerSettingsRepository(second).settings.first())
        scope2.cancel()
    }
}
