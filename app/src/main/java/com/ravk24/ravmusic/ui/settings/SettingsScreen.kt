package com.ravk24.ravmusic.ui.settings

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ravk24.ravmusic.NoSettings
import com.ravk24.ravmusic.SettingsHost
import com.ravk24.ravmusic.data.repo.LibraryState
import com.ravk24.ravmusic.data.settings.SettingsRepository
import com.ravk24.ravmusic.data.settings.ThemeMode
import com.ravk24.ravmusic.ui.components.AppIcons
import com.ravk24.ravmusic.ui.components.formatScanTime
import com.ravk24.ravmusic.ui.components.songCountLabel
import com.ravk24.ravmusic.ui.components.thresholdLabel
import com.ravk24.ravmusic.ui.theme.RavMusicTheme

/**
 * Settings (artboard 1g): theme override, the "Skip short audio" threshold, the library info line
 * with Rescan, and the "Built by" footer. Everything is read from and written through [settings]
 * (design D4 of `polish`); Rescan is the same refresh as pull-to-refresh.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    settings: SettingsHost = NoSettings,
    libraryState: LibraryState = LibraryState.Idle,
    onRescan: () -> Unit = {},
    now: () -> Long = System::currentTimeMillis,
) {
    val versionName = rememberVersionName()
    val themeMode by settings.themeMode.collectAsStateWithLifecycle()
    val minDurationMs by settings.minDurationMs.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_settings"),
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back")) {
                        Icon(
                            imageVector = AppIcons.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            SectionTitle("Theme")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = themeMode == mode,
                        onClick = { settings.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
                        modifier = Modifier.testTag("theme_${mode.name.lowercase()}"),
                    ) {
                        Text(
                            when (mode) {
                                ThemeMode.SYSTEM -> "System"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            SectionTitle("Skip short audio")
            Text(
                text = "Hides notification sounds and voice notes shorter than this. Changing it rescans the library.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            val thresholds = SettingsRepository.THRESHOLDS_MS
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                thresholds.forEachIndexed { index, ms ->
                    SegmentedButton(
                        selected = minDurationMs == ms,
                        onClick = { settings.setMinDuration(ms) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = thresholds.size),
                        icon = {},
                        modifier = Modifier.testTag("min_duration_$ms"),
                    ) {
                        Text(thresholdLabel(ms), maxLines = 1)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            SectionTitle("Library")
            val loaded = libraryState as? LibraryState.Loaded
            val refreshing = loaded?.refreshing == true || libraryState == LibraryState.Loading
            Text(
                text = when {
                    loaded == null -> "Library not scanned yet"
                    else -> "Last scan · ${formatScanTime(loaded.snapshot.scannedAt, now())} · ${songCountLabel(loaded.snapshot.totalSongs)}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .testTag("settings_last_scan"),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onRescan,
                    enabled = loaded != null && !refreshing,
                    modifier = Modifier.testTag("settings_rescan"),
                ) {
                    Text("Rescan library")
                }
                if (refreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(22.dp)
                            .testTag("settings_rescan_progress"),
                        strokeWidth = 2.dp,
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
            Spacer(Modifier.weight(1f, fill = true))
            Text(
                text = "v$versionName\nBuilt by Ravi Kant",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .testTag("settings_footer"),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

@Composable
private fun rememberVersionName(): String {
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    return remember(context) {
        if (inPreview) {
            "1.0"
        } else {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, 0)
            }
            info.versionName ?: "?"
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    RavMusicTheme {
        SettingsScreen(onBack = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenDarkPreview() {
    RavMusicTheme(darkTheme = true) {
        SettingsScreen(onBack = {})
    }
}
