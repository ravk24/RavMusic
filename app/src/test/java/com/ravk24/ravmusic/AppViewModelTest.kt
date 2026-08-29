package com.ravk24.ravmusic

import androidx.lifecycle.SavedStateHandle
import com.ravk24.ravmusic.permission.PermissionChecker
import com.ravk24.ravmusic.permission.PermissionState
import org.junit.Assert.assertEquals
import org.junit.Test

class AppViewModelTest {

    private class FakeChecker(
        private val granted: Boolean,
        private val rationale: Boolean,
    ) : PermissionChecker {
        override fun isGranted() = granted
        override fun shouldShowRationale() = rationale
    }

    @Test
    fun `initial state is Unknown until refreshed`() {
        val vm = AppViewModel(SavedStateHandle())
        assertEquals(PermissionState.Unknown, vm.permissionState.value)
    }

    @Test
    fun `never requested and not granted is requestable`() {
        val vm = AppViewModel(SavedStateHandle())
        vm.refresh(FakeChecker(granted = false, rationale = false))
        assertEquals(PermissionState.Denied(canRequest = true), vm.permissionState.value)
    }

    @Test
    fun `requested, no rationale, not granted is permanently denied`() {
        val vm = AppViewModel(SavedStateHandle())
        vm.markRequested()
        vm.refresh(FakeChecker(granted = false, rationale = false))
        assertEquals(PermissionState.Denied(canRequest = false), vm.permissionState.value)
    }

    @Test
    fun `requested with rationale is still requestable`() {
        val vm = AppViewModel(SavedStateHandle())
        vm.markRequested()
        vm.refresh(FakeChecker(granted = false, rationale = true))
        assertEquals(PermissionState.Denied(canRequest = true), vm.permissionState.value)
    }

    @Test
    fun `granted wins regardless of request history`() {
        val vm = AppViewModel(SavedStateHandle())
        vm.refresh(FakeChecker(granted = true, rationale = false))
        assertEquals(PermissionState.Granted, vm.permissionState.value)
        vm.markRequested()
        vm.refresh(FakeChecker(granted = true, rationale = false))
        assertEquals(PermissionState.Granted, vm.permissionState.value)
    }

    @Test
    fun `hasRequested survives in saved state`() {
        val handle = SavedStateHandle()
        AppViewModel(handle).markRequested()
        // A new ViewModel restored from the same saved state must remember the request.
        val restored = AppViewModel(handle)
        restored.refresh(FakeChecker(granted = false, rationale = false))
        assertEquals(PermissionState.Denied(canRequest = false), restored.permissionState.value)
    }

    @Test
    fun `evaluate decision table`() {
        assertEquals(PermissionState.Granted, AppViewModel.evaluate(granted = true, showRationale = false, hasRequested = false))
        assertEquals(PermissionState.Denied(true), AppViewModel.evaluate(granted = false, showRationale = false, hasRequested = false))
        assertEquals(PermissionState.Denied(true), AppViewModel.evaluate(granted = false, showRationale = true, hasRequested = true))
        assertEquals(PermissionState.Denied(false), AppViewModel.evaluate(granted = false, showRationale = false, hasRequested = true))
    }
}
