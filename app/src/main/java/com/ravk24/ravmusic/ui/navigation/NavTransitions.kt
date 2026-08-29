package com.ravk24.ravmusic.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene

/**
 * How screens move (design D7 of `polish`). Ordinary pushes cross-fade with a slight horizontal
 * slide, pops mirror them; Now Playing is a sheet-like route that slides up over whatever is
 * showing and slides back down on collapse or predictive back, staying on top the whole time.
 * Nothing bounces: plain tweens, 260 ms for screens, 320 ms for the player.
 */
object NavTransitions {
    const val SCREEN_MS = 260
    const val PLAYER_MS = 320

    private const val PLAYER_MARKER = "com.ravk24.ravmusic.player"

    /** Attach to the Now Playing entry (`entry<NowPlaying>(metadata = …)`) so the specs can recognise it. */
    val playerMetadata: Map<String, Any> = mapOf(PLAYER_MARKER to true)

    private fun isNowPlaying(scene: Scene<NavKey>): Boolean =
        scene.entries.lastOrNull()?.metadata?.containsKey(PLAYER_MARKER) == true

    /** Push. Entering Now Playing: slide up over the current screen, which stays put underneath. */
    val push: AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
        if (isNowPlaying(targetState)) {
            (slideInVertically(tween(PLAYER_MS)) { it } togetherWith ExitTransition.KeepUntilTransitionsFinished)
                .apply { targetContentZIndex = 1f }
        } else {
            (fadeIn(tween(SCREEN_MS)) + slideInHorizontally(tween(SCREEN_MS)) { it / 8 }) togetherWith
                (fadeOut(tween(SCREEN_MS)) + slideOutHorizontally(tween(SCREEN_MS)) { -it / 8 })
        }
    }

    /** Pop. Leaving Now Playing: slide it down off the screen that is already waiting underneath. */
    val pop: AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
        if (isNowPlaying(initialState)) {
            EnterTransition.None togetherWith slideOutVertically(tween(PLAYER_MS)) { it }
        } else {
            (fadeIn(tween(SCREEN_MS)) + slideInHorizontally(tween(SCREEN_MS)) { -it / 8 }) togetherWith
                (fadeOut(tween(SCREEN_MS)) + slideOutHorizontally(tween(SCREEN_MS)) { it / 8 })
        }
    }

    /** Predictive back follows the gesture with the same shapes as [pop]. */
    val predictivePop: AnimatedContentTransitionScope<Scene<NavKey>>.(Int) -> ContentTransform = { _ -> pop() }
}
