package com.ravk24.ravmusic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.ravk24.ravmusic.data.model.OpenRequest
import com.ravk24.ravmusic.permission.PermissionChecker
import com.ravk24.ravmusic.permission.PermissionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * App-wide state that outlives individual screens: the audio-permission state and the pending
 * "open this file" request handed in by the system (`open-with`).
 */
class AppViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _permissionState = MutableStateFlow<PermissionState>(PermissionState.Unknown)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    private val _pendingOpen = MutableStateFlow<OpenRequest?>(null)

    /**
     * The file the system asked us to open, until the shell has resolved and played it. Kept in
     * memory only (not saved state), so a configuration change re-runs the resolution but a
     * relaunch from recents does not replay the file.
     */
    val pendingOpen: StateFlow<OpenRequest?> = _pendingOpen.asStateFlow()

    private var openSeq = 0

    /** A new open request; replaces any request still pending. */
    fun submitOpen(uri: String, mimeType: String?) {
        _pendingOpen.value = OpenRequest(uri, mimeType, ++openSeq)
    }

    /** Clears the request with [seq]; a newer request submitted meanwhile is left in place. */
    fun consumeOpen(seq: Int) {
        _pendingOpen.update { current -> if (current?.seq == seq) null else current }
    }

    /**
     * Whether the app has asked for the permission at least once this install. Kept in saved
     * state so process death does not make a permanently-denied permission look requestable.
     */
    private var hasRequested: Boolean
        get() = savedStateHandle[KEY_HAS_REQUESTED] ?: false
        set(value) {
            savedStateHandle[KEY_HAS_REQUESTED] = value
        }

    /** Call right before launching the system permission dialog. */
    fun markRequested() {
        hasRequested = true
    }

    /**
     * Re-evaluate the permission. Called on every resume so grants or revocations made in
     * system Settings while the app was backgrounded are honoured immediately.
     */
    fun refresh(checker: PermissionChecker) {
        _permissionState.value = evaluate(
            granted = checker.isGranted(),
            showRationale = checker.shouldShowRationale(),
            hasRequested = hasRequested,
        )
    }

    companion object {
        private const val KEY_HAS_REQUESTED = "audio_permission_requested"

        /**
         * Pure decision table. "Permanently denied" (canRequest = false) is only inferred once a
         * request has actually been made, because a fresh install also reports no rationale.
         */
        fun evaluate(granted: Boolean, showRationale: Boolean, hasRequested: Boolean): PermissionState =
            when {
                granted -> PermissionState.Granted
                !hasRequested -> PermissionState.Denied(canRequest = true)
                showRationale -> PermissionState.Denied(canRequest = true)
                else -> PermissionState.Denied(canRequest = false)
            }
    }
}
