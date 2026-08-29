# Phase 3 — Playback core

**OpenSpec change:** `playback-core` · **Status:** ⏳ Planned

## Goal
Rock-solid background playback and the first "it's a real app" moment: tap a song in a folder and it
plays, with a mini player docked above the bottom nav (spec **F4**, part of **F7**).

## In scope
- `PlaybackService : MediaSessionService` + ExoPlayer in `playback/`; foreground service with
  `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` and `foregroundServiceType="mediaPlayback"`
- `MediaController` bridge in the UI process (`PlayerViewModel`, `StateFlow` of playing / position / item)
- Tap-to-play from folder detail: ad-hoc queue = that folder's songs from the tapped one
- Queue *source* tracked ("Playing from: <folder/playlist>") for Now Playing later
- Media3 default notification, lockscreen, headset buttons, audio focus, `AUDIO_BECOMING_NOISY` → pause
- Mini player: art thumbnail (gradient), title, artist, play/pause, thin progress line; tap expands
  (stub until phase 5), swipe-away stops and clears the queue
- Missing-file handling at queue-build time: pre-filter URIs that no longer resolve

## Out of scope
Full Now Playing, shuffle/repeat UI, sleep timer, playlists.

## Capabilities
`playback` (new), `mini-player` (new)

## Key risks
- Foreground-service start restrictions on API 34+ → start from a user action only
- Mini player docking (Scaffold `bottomBar` stack vs overlay) — decide here, shell already has the seam
- Process death while playing → `MediaController` reconnect on Activity recreate

## Verification
- Play, background the app, lock the screen: audio continues, notification controls work
- Unplug headphones (emulator: `adb shell cmd media_session`) → pauses
- Kill the Activity while playing → reopening reconnects to the same session
