package com.ravk24.ravmusic

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravk24.ravmusic.permission.AndroidPermissionChecker
import com.ravk24.ravmusic.permission.audioPermissionFor
import com.ravk24.ravmusic.ui.navigation.AppNavigation
import com.ravk24.ravmusic.ui.theme.RavMusicTheme

/** The app's single activity. Everything visible is Compose. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RavMusicTheme {
                AppRoot()
            }
        }
    }
}

/**
 * Wires the Android-only side effects (permission dialog, system settings intent, resume
 * re-check) to the pure [AppViewModel] state and hands the result to [AppNavigation].
 */
@Composable
private fun AppRoot(viewModel: AppViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = checkNotNull(LocalActivity.current) { "AppRoot must be hosted in an Activity" }
    val checker = remember(activity) { AndroidPermissionChecker(activity) }
    val permissionState by viewModel.permissionState.collectAsStateWithLifecycle()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refresh(checker)
    }

    // Re-evaluate on every resume so grants/revocations made in system Settings are honoured.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh(checker)
    }

    AppNavigation(
        permissionState = permissionState,
        onRequestPermission = {
            viewModel.markRequested()
            launcher.launch(audioPermissionFor())
        },
        onOpenAppSettings = {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            )
            context.startActivity(intent)
        },
    )
}
