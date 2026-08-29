package com.ravk24.ravmusic.ui.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.ravk24.ravmusic.playback.SleepTimerState
import com.ravk24.ravmusic.ui.components.AppIcons
import com.ravk24.ravmusic.ui.components.formatRemaining

/** Presets offered by the picker (spec F6). */
val SleepTimerPresetMinutes: List<Int> = listOf(15, 30, 45, 60)

private const val MAX_CUSTOM_MINUTES = 600

/**
 * The sleep-timer picker (design D4). Off: presets, custom minutes, end of current track.
 * Active: the remaining time with extend and cancel. Every action closes the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    state: SleepTimerState,
    remainingMs: Long?,
    onPreset: (minutes: Int) -> Unit,
    onCustom: (minutes: Int) -> Unit,
    onEndOfTrack: () -> Unit,
    onExtend: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var custom by rememberSaveable { mutableStateOf("") }
    val customMinutes = custom.trim().toIntOrNull()?.takeIf { it in 1..MAX_CUSTOM_MINUTES }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("sleep_sheet"),
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(AppIcons.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(
                    text = "Sleep timer",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            when (state) {
                SleepTimerState.Off -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SleepTimerPresetMinutes.forEach { minutes ->
                            FilterChip(
                                selected = false,
                                onClick = { onPreset(minutes); onDismiss() },
                                label = { Text("$minutes min") },
                                modifier = Modifier.testTag("sleep_preset_$minutes"),
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = custom,
                            onValueChange = { custom = it.filter { ch -> ch.isDigit() }.take(3) },
                            singleLine = true,
                            label = { Text("Custom minutes") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sleep_custom_field"),
                        )
                        Button(
                            onClick = { customMinutes?.let { onCustom(it); onDismiss() } },
                            enabled = customMinutes != null,
                            modifier = Modifier.testTag("sleep_custom_set"),
                        ) { Text("Set") }
                    }
                    TextButton(
                        onClick = { onEndOfTrack(); onDismiss() },
                        modifier = Modifier.testTag("sleep_end_of_track"),
                    ) { Text("End of current track") }
                }

                is SleepTimerState.Countdown, SleepTimerState.EndOfTrack -> {
                    Text(
                        text = if (state is SleepTimerState.EndOfTrack) {
                            "Pausing at the end of this track"
                        } else {
                            "Pausing in ${formatRemaining(remainingMs ?: 0L)}"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("sleep_remaining"),
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (state is SleepTimerState.Countdown) {
                            Button(
                                onClick = { onExtend(); onDismiss() },
                                modifier = Modifier.testTag("sleep_extend"),
                            ) { Text("Extend 15 min") }
                        }
                        TextButton(
                            onClick = { onCancel(); onDismiss() },
                            modifier = Modifier.testTag("sleep_cancel"),
                        ) { Text("Cancel timer") }
                    }
                }
            }
        }
    }
}
