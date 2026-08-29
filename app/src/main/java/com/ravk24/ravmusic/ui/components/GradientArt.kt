package com.ravk24.ravmusic.ui.components

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Gradient-as-album-art (decision D-02): the six gradient pairs from the design canvas, chosen
 * deterministically from a seed so a song always gets the same art.
 */
val ArtGradientPairs: List<Pair<Long, Long>> = listOf(
    0xFFA960EE to 0xFFFF5EA0,
    0xFF635BFF to 0xFF80E9FF,
    0xFFFF5EA0 to 0xFFFF8A5C,
    0xFF0A2540 to 0xFF635BFF,
    0xFFFF8A5C to 0xFFFFCB57,
    0xFF80E9FF to 0xFFA960EE,
)

/** Pure: which of the six pairs a seed maps to. */
fun artGradientIndex(seed: Long): Int = Math.floorMod(seed, ArtGradientPairs.size.toLong()).toInt()

/** The diagonal gradient brush for a seed (a song or playlist id). */
fun artGradient(seed: Long): Brush {
    val (start, end) = ArtGradientPairs[artGradientIndex(seed)]
    return Brush.linearGradient(listOf(Color(start), Color(end)))
}
