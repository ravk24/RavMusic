package com.ravk24.ravmusic.ui.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ravk24.ravmusic.EqualizerUiState
import com.ravk24.ravmusic.EqualizerViewModel
import com.ravk24.ravmusic.RavMusicApp
import com.ravk24.ravmusic.data.settings.EQ_CUSTOM_PRESET
import com.ravk24.ravmusic.data.settings.EQ_MAX_STRENGTH
import com.ravk24.ravmusic.ui.components.AppIcons
import kotlin.math.roundToInt

private val BAND_SLIDER_HEIGHT = 160.dp
private const val UNSUPPORTED_NOTE = "Not supported on this device"

/**
 * The equalizer surface (design D5 of `add-equalizer`): master switch, device presets plus
 * Custom, one vertical slider per band, bass boost and virtualizer. Everything below the switch
 * is disabled while effects are off but keeps showing the stored values.
 */
@Composable
fun EqualizerSheet(onDismiss: () -> Unit) {
    val container = (LocalContext.current.applicationContext as RavMusicApp).container
    val viewModel: EqualizerViewModel = viewModel(
        factory = viewModelFactory {
            initializer { EqualizerViewModel(container.equalizerSettingsRepository, container.playerConnection) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    EqualizerSheetContent(
        state = state,
        onEnabled = viewModel::setEnabled,
        onPreset = viewModel::selectPreset,
        onCustom = viewModel::selectCustom,
        onBandLevel = viewModel::setBandLevel,
        onBassBoost = viewModel::setBassBoost,
        onVirtualizer = viewModel::setVirtualizer,
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheetContent(
    state: EqualizerUiState,
    onEnabled: (Boolean) -> Unit,
    onPreset: (Int) -> Unit,
    onCustom: () -> Unit,
    onBandLevel: (band: Int, levelMb: Int) -> Unit,
    onBassBoost: (Int) -> Unit,
    onVirtualizer: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val caps = state.capabilities
    val on = state.settings.enabled

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("eq_sheet"),
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(AppIcons.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(
                    text = "Equalizer",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = on,
                    onCheckedChange = onEnabled,
                    enabled = state.loaded && caps?.anySupported == true,
                    modifier = Modifier.testTag("eq_switch"),
                )
            }

            if (caps != null && !caps.anySupported) {
                Text(
                    text = "Audio effects are not supported on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("eq_unsupported"),
                )
            }

            if (caps != null && caps.equalizerSupported) {
                if (caps.presetNames.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = state.selectedPreset == EQ_CUSTOM_PRESET,
                                onClick = onCustom,
                                enabled = on,
                                label = { Text("Custom") },
                                modifier = Modifier.testTag("eq_preset_custom"),
                            )
                        }
                        itemsIndexed(caps.presetNames) { index, name ->
                            FilterChip(
                                selected = state.selectedPreset == index,
                                onClick = { onPreset(index) },
                                enabled = on,
                                label = { Text(name) },
                                modifier = Modifier.testTag("eq_preset_$index"),
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    repeat(caps.bandCount) { band ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            VerticalSlider(
                                value = (state.displayedBandLevels.getOrNull(band) ?: 0).toFloat(),
                                onValueChange = { onBandLevel(band, it.roundToInt()) },
                                valueRange = caps.minLevelMb.toFloat()..caps.maxLevelMb.toFloat(),
                                enabled = on,
                                modifier = Modifier.testTag("eq_band_$band"),
                            )
                            Text(
                                text = formatBandFrequency(caps.centerFreqsMilliHz.getOrNull(band) ?: 0),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else if (caps != null && caps.anySupported) {
                LabeledNote(label = "Equalizer bands", note = UNSUPPORTED_NOTE)
            }

            StrengthSlider(
                label = "Bass boost",
                strength = state.settings.bassBoost,
                supported = caps?.bassBoostSupported == true,
                enabled = on,
                onChange = onBassBoost,
                tag = "eq_bass_boost",
            )
            StrengthSlider(
                label = "Virtualizer",
                strength = state.settings.virtualizer,
                supported = caps?.virtualizerSupported == true,
                enabled = on,
                onChange = onVirtualizer,
                tag = "eq_virtualizer",
            )
        }
    }
}

/** A 0–1000 `audiofx` strength as a labelled percent slider; unsupported renders disabled with a note. */
@Composable
private fun StrengthSlider(
    label: String,
    strength: Int,
    supported: Boolean,
    enabled: Boolean,
    onChange: (Int) -> Unit,
    tag: String,
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (supported) "${strength / 10}%" else UNSUPPORTED_NOTE,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = strength.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 0f..EQ_MAX_STRENGTH.toFloat(),
            enabled = supported && enabled,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag),
        )
    }
}

@Composable
private fun LabeledNote(label: String, note: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = note, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * A stock [Slider] turned 90° anticlockwise: measured with swapped constraints so its length
 * becomes the fixed [BAND_SLIDER_HEIGHT] and it occupies a thin vertical strip.
 */
@Composable
private fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = Modifier.height(BAND_SLIDER_HEIGHT), contentAlignment = Alignment.Center) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            modifier = modifier
                .graphicsLayer {
                    rotationZ = 270f
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        Constraints(
                            minWidth = constraints.minHeight,
                            maxWidth = constraints.maxHeight,
                            minHeight = constraints.minWidth,
                            maxHeight = constraints.maxWidth,
                        ),
                    )
                    layout(placeable.height, placeable.width) {
                        placeable.place(-placeable.width, 0)
                    }
                },
        )
    }
}

/** An `audiofx` centre frequency (milliHertz) as a short label: "60 Hz", "14 kHz". */
fun formatBandFrequency(milliHz: Int): String {
    val hz = milliHz / 1000
    return if (hz >= 1000) "${hz / 1000} kHz" else "$hz Hz"
}
