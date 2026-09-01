package com.ravk24.ravmusic

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.ravk24.ravmusic.data.settings.EQ_CUSTOM_PRESET
import com.ravk24.ravmusic.data.settings.EqualizerSettings
import com.ravk24.ravmusic.data.settings.EqualizerSettingsRepository
import com.ravk24.ravmusic.playback.EqCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** State mapping and the Custom-on-band-touch rule over a real DataStore file. */
@OptIn(ExperimentalCoroutinesApi::class)
class EqualizerViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val caps = EqCapabilities(
        equalizerSupported = true,
        bandCount = 3,
        centerFreqsMilliHz = listOf(60_000, 910_000, 14_000_000),
        minLevelMb = -1500,
        maxLevelMb = 1500,
        presetNames = listOf("Normal", "Rock"),
        presetBandLevels = listOf(listOf(0, 0, 0), listOf(500, -100, 500)),
        bassBoostSupported = true,
        virtualizerSupported = true,
    )

    private fun kotlinx.coroutines.test.TestScope.repo(): Pair<EqualizerSettingsRepository, CoroutineScope> {
        val scope = CoroutineScope(SupervisorJob() + dispatcherRule.dispatcher)
        val ds = PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(tmp.root, "s.preferences_pb") })
        return EqualizerSettingsRepository(ds) to scope
    }

    private fun viewModel(repo: EqualizerSettingsRepository, caps: EqCapabilities? = this.caps) =
        EqualizerViewModel(repo, { onResult -> onResult(caps) }, persistDelayMs = 100L)

    @Test
    fun `loads defaults and capabilities`() = runTest {
        val (repo, scope) = repo()
        val vm = viewModel(repo)
        runCurrent()
        val state = vm.state.value
        assertTrue(state.loaded)
        assertEquals(caps, state.capabilities)
        assertFalse(state.settings.enabled)
        assertEquals(EQ_CUSTOM_PRESET, state.selectedPreset)
        assertEquals(listOf(0, 0, 0), state.displayedBandLevels)
        scope.cancel()
    }

    @Test
    fun `selecting a preset shows its shape`() = runTest {
        val (repo, scope) = repo()
        val vm = viewModel(repo)
        runCurrent()
        vm.selectPreset(1)
        assertEquals(1, vm.state.value.selectedPreset)
        assertEquals(listOf(500, -100, 500), vm.state.value.displayedBandLevels)
        scope.cancel()
    }

    @Test
    fun `touching a band switches to Custom seeded from the preset shape`() = runTest {
        val (repo, scope) = repo()
        val vm = viewModel(repo)
        runCurrent()
        vm.selectPreset(1)
        vm.setBandLevel(0, 1200)
        val state = vm.state.value
        assertEquals(EQ_CUSTOM_PRESET, state.selectedPreset)
        assertEquals(listOf(1200, -100, 500), state.displayedBandLevels)
        scope.cancel()
    }

    @Test
    fun `band levels clamp to the device range`() = runTest {
        val (repo, scope) = repo()
        val vm = viewModel(repo)
        runCurrent()
        vm.setBandLevel(2, 99_999)
        assertEquals(listOf(0, 0, 1500), vm.state.value.displayedBandLevels)
        scope.cancel()
    }

    @Test
    fun `edits persist after the quiet period with the last value winning`() = runTest {
        val (repo, scope) = repo()
        val vm = viewModel(repo)
        runCurrent()
        vm.setEnabled(true)
        vm.setBassBoost(300)
        vm.setBassBoost(700)
        advanceTimeBy(200L)
        runCurrent()
        val saved = repo.settings.first()
        assertTrue(saved.enabled)
        assertEquals(700, saved.bassBoost)
        scope.cancel()
    }

    @Test
    fun `a stored out-of-range preset reads as Custom`() = runTest {
        val (repo, scope) = repo()
        repo.save(EqualizerSettings(enabled = true, presetIndex = 42))
        val vm = viewModel(repo)
        runCurrent()
        assertEquals(EQ_CUSTOM_PRESET, vm.state.value.selectedPreset)
        scope.cancel()
    }
}
