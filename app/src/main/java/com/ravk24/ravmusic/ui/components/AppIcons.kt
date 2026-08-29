package com.ravk24.ravmusic.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The handful of icons the shell needs, built from the 24dp Material path data used in the
 * design canvas. Keeps the app free of the material-icons artifacts (core is no longer a
 * transitive dependency of Material 3; extended is ~10 MB unshrunk).
 */
object AppIcons {

    private fun materialIcon(name: String, pathData: String, autoMirror: Boolean = false): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = autoMirror,
        ).addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black)).build()

    /** Vertical three-dot overflow. */
    val MoreVert: ImageVector by lazy {
        materialIcon(
            "MoreVert",
            "M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z",
        )
    }

    /** Back arrow; mirrored automatically in RTL layouts. */
    val ArrowBack: ImageVector by lazy {
        materialIcon(
            "ArrowBack",
            "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z",
            autoMirror = true,
        )
    }

    val PlayArrow: ImageVector by lazy { materialIcon("PlayArrow", "M8 5v14l11-7z") }

    val Shuffle: ImageVector by lazy {
        materialIcon(
            "Shuffle",
            "M10.59 9.17L5.41 4 4 5.41l5.17 5.17 1.42-1.41zM14.5 4l2.04 2.04L4 18.59 5.41 20 17.96 7.46 20 9.5V4h-5.5zm.33 9.41l-1.41 1.41 3.13 3.13L14.5 20H20v-5.5l-2.04 2.04-3.13-3.13z",
        )
    }

    val DragHandle: ImageVector by lazy {
        materialIcon(
            "DragHandle",
            "M11 18c0 1.1-.9 2-2 2s-2-.9-2-2 .9-2 2-2 2 .9 2 2zm-2-8c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0-6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm6 4c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z",
        )
    }

    val Add: ImageVector by lazy { materialIcon("Add", "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z") }

    val Close: ImageVector by lazy {
        materialIcon("Close", "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z")
    }

    val Check: ImageVector by lazy { materialIcon("Check", "M9 16.2L4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4z") }

    val Delete: ImageVector by lazy {
        materialIcon("Delete", "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z")
    }

    val Pause: ImageVector by lazy { materialIcon("Pause", "M6 19h4V5H6v14zm8-14v14h4V5h-4z") }

    /** Row chevron; mirrored automatically in RTL layouts. */
    val ChevronRight: ImageVector by lazy {
        materialIcon(
            "ChevronRight",
            "M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z",
            autoMirror = true,
        )
    }

    val Folder: ImageVector by lazy {
        ImageVector.Builder(
            name = "Folder",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(10f, 4f)
                horizontalLineTo(4f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(12f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(16f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(8f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                horizontalLineToRelative(-8f)
                lineToRelative(-2f, -2f)
                close()
            }
        }.build()
    }

    val QueueMusic: ImageVector by lazy {
        ImageVector.Builder(
            name = "QueueMusic",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(15f, 6f)
                horizontalLineTo(3f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(12f)
                verticalLineTo(6f)
                close()
                moveToRelative(0f, 4f)
                horizontalLineTo(3f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(12f)
                verticalLineToRelative(-2f)
                close()
                moveTo(3f, 16f)
                horizontalLineToRelative(8f)
                verticalLineToRelative(-2f)
                horizontalLineTo(3f)
                verticalLineToRelative(2f)
                close()
                moveToRelative(14f, -8f)
                verticalLineToRelative(8.18f)
                curveToRelative(-0.31f, -0.11f, -0.65f, -0.18f, -1f, -0.18f)
                curveToRelative(-1.66f, 0f, -3f, 1.34f, -3f, 3f)
                reflectiveCurveToRelative(1.34f, 3f, 3f, 3f)
                reflectiveCurveToRelative(3f, -1.34f, 3f, -3f)
                verticalLineTo(10f)
                horizontalLineToRelative(3f)
                verticalLineTo(8f)
                horizontalLineToRelative(-5f)
                close()
            }
        }.build()
    }

    /** Music note used by the empty state (artboard 1h). */
    val MusicNote: ImageVector by lazy {
        ImageVector.Builder(
            name = "MusicNote",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 3f)
                verticalLineToRelative(10.55f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = false, 14f, 17f)
                verticalLineTo(7f)
                horizontalLineToRelative(4f)
                verticalLineTo(3f)
                horizontalLineToRelative(-6f)
                close()
            }
        }.build()
    }
}
