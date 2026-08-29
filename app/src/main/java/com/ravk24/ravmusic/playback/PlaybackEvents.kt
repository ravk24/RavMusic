package com.ravk24.ravmusic.playback

/**
 * One-shot events the service broadcasts to connected controllers as custom session commands
 * (design D5 of `polish`). Unlike session extras these are not state: a controller that connects
 * later never sees them, which is exactly right for a transient notice.
 */
object PlaybackEvents {
    /** A queued file could not be opened and was skipped; [ARG_TITLE] names it. */
    const val SKIPPED_MISSING = "com.ravk24.ravmusic.event.skipped_missing"
    const val ARG_TITLE = "title"
}
