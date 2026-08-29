package com.ravk24.ravmusic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravk24.ravmusic.data.repo.LibraryRepository
import com.ravk24.ravmusic.data.repo.LibraryState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Activity-scoped bridge between the UI and [LibraryRepository]. The Folders navigation entry is
 * popped on every tab switch, so library state cannot live in an entry-scoped ViewModel; this one
 * is created in `AppRoot`, next to the permission ViewModel, and the repository holds the data.
 */
class LibraryViewModel(private val repository: LibraryRepository) : ViewModel() {

    val state: StateFlow<LibraryState> = repository.state

    /** Load once the permission is granted; forget the library when it is not. */
    fun onPermissionChanged(granted: Boolean) {
        if (granted) {
            viewModelScope.launch { repository.ensureLoaded() }
        } else {
            repository.clear()
        }
    }

    /** Pull-to-refresh / Rescan. */
    fun refresh() {
        viewModelScope.launch { repository.refresh() }
    }
}
