# Phase 3 — Playback core

**OpenSpec change:** `playback-core` · **Status:** ✅ Done (2026-08-29)

**Result:** 48 JVM unit tests green; 44 Compose/instrumented tests green on the API 36 and API 26 emulators,
including `PlaybackServiceTest`, which drives the real service through the `MediaController` bridge with a bundled
3 s WAV (play / pause / clear, missing file mid-queue skipped, missing last file ends the queue). adb walkthrough on
both: tap-to-play, foreground service + media notification, playback survives Home / screen-off / leaving the app,
system pause and headset key reflected in the app, swipe-away clears session and notification. `AUDIO_BECOMING_NOISY`
cannot be injected from the shell (protected broadcast); ExoPlayer's built-in handling is relied on.

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
- Mini player: title/artist, title, artist, play/pause, thin progress line; tap expands
  (stub until phase 5), swipe-away stops and clears the queue
- Missing-file handling at playback time: a song whose file no longer opens is skipped to the next one
  (decision D-26 — replaces the original "pre-filter at queue-build time" idea)

## Out of scope
Full Now Playing, shuffle/repeat UI, sleep timer, playlists.

## Capabilities
`playback` (new), `mini-player` (new)

## Key risks
- Foreground-service start restrictions on API 34+ → start from a user action only
- Mini player docking (Scaffold `bottomBar` stack vs overlay) — decided: `bottomBar` column (D-27)
- Process death while playing → `MediaController` reconnect on Activity recreate

## Verification
- Play, background the app, lock the screen: audio continues, notification controls work
- Unplug headphones (emulator: `adb shell cmd media_session`) → pauses
- Kill the Activity while playing → reopening reconnects to the same session

## Task progress
See `openspec/changes/playback-core/tasks.md` (15 tasks, six groups) — moves to
`openspec/changes/archive/` when the change is archived.
