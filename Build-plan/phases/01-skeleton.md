# Phase 1 — Skeleton

**OpenSpec change:** `app-skeleton` · **Status:** ✅ Done (2026-08-29)

**Result:** 11 JVM unit tests + 16 Compose UI tests green on API 36 and API 26 emulators; scripted
adb walkthrough of every `app-shell` / `theme` / `audio-permission` scenario passed on both. The launcher
icon (decision D-14) was pulled forward from Phase 7 and shipped here.

## Goal
An installable shell every later phase plugs into: the single activity, the fixed-palette theme,
the two-tab bottom navigation, and the audio-permission flow — settled once.

## In scope
- Package rename to `com.ravk24.ravmusic`
- `MainActivity` (edge-to-edge) + `RavMusicApp` / `AppContainer` (manual DI home)
- Material 3 theme: fixed navy/blurple palette, light + dark following the system
- Navigation 3 shell: **Playlists** (default) / **Folders** tabs, back-stack rules, per-tab state retention
- Placeholder tab content
- Audio permission: `READ_MEDIA_AUDIO` (13+) / `READ_EXTERNAL_STORAGE` (≤12), "No music found" state,
  deny / permanent-deny → system settings / revoke-on-return handling
- Overflow → stub Settings screen (title, back, privacy footer)
- Manifest: activity + storage permissions; still **no** `INTERNET`

## Out of scope (later phases)
MediaStore, Media3, mini player, Room, persisted theme override, real Settings rows.

## Capabilities introduced (`openspec/specs/` after archive)
`app-shell`, `theme`, `audio-permission`

## Key risks
- Navigation 3 API is young → all Nav3 usage confined to `ui/navigation/`
- Package rename after an old debug install → `adb uninstall com.example.ravmusic` once
- Edge-to-edge differences on API 26–29 → verified on an API 26 emulator

## Verification
- `assembleDebug testDebugUnitTest` green; `connectedDebugAndroidTest` green on API 36 emulator
- Merged manifest: `minSdkVersion=26`, both storage permissions, no `INTERNET`
- Walkthrough: fresh install → empty state → grant → tabs → Settings → back → exit; rotation; dark mode

## Task progress
See `openspec/changes/app-skeleton/tasks.md` (24 tasks, six groups).
