package com.ravk24.ravmusic

import com.ravk24.ravmusic.data.mediastore.MediaScanner
import com.ravk24.ravmusic.data.model.Song
import com.ravk24.ravmusic.data.repo.LibraryRepository
import com.ravk24.ravmusic.data.repo.LibraryState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private class CountingScanner : MediaScanner {
        var calls = 0
        override fun scan(): List<Song> {
            calls++
            return listOf(
                Song(1, "content://media/external/audio/media/1", "Song", null, 100_000L, "f", "Folder"),
            )
        }
    }

    private fun viewModel(scanner: CountingScanner) =
        LibraryViewModel(LibraryRepository(scanner, ioDispatcher = mainDispatcher.dispatcher))

    @Test
    fun `granted loads the library once`() = runTest(mainDispatcher.dispatcher) {
        val scanner = CountingScanner()
        val vm = viewModel(scanner)
        vm.onPermissionChanged(granted = true)
        vm.onPermissionChanged(granted = true)
        assertTrue(vm.state.value is LibraryState.Loaded)
        assertEquals(1, scanner.calls)
    }

    @Test
    fun `not granted clears the library`() = runTest(mainDispatcher.dispatcher) {
        val scanner = CountingScanner()
        val vm = viewModel(scanner)
        vm.onPermissionChanged(granted = true)
        vm.onPermissionChanged(granted = false)
        assertEquals(LibraryState.Idle, vm.state.value)
    }

    @Test
    fun `refresh rescans`() = runTest(mainDispatcher.dispatcher) {
        val scanner = CountingScanner()
        val vm = viewModel(scanner)
        vm.onPermissionChanged(granted = true)
        vm.refresh()
        assertEquals(2, scanner.calls)
        assertTrue(vm.state.value is LibraryState.Loaded)
    }
}
