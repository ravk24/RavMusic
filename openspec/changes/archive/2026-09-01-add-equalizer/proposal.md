# Add Equalizer

## Why

RavMusic plays whatever the device's default audio chain produces; there is no way to shape the sound for a genre, a pair of headphones, or a room. A built-in equalizer with bass boost and virtualizer is the most-requested class of audio feature in a local music player and rounds out the playback experience without touching the app's minimal visual identity.

## What Changes

- New in-app **Equalizer** surface (opened from a chip on Now Playing) with:
  - A master on/off switch for all audio effects.
  - The device's equalizer **presets** plus a **Custom** mode with a slider per band (typically 5 bands, device-dependent).
  - **Bass boost** and **Virtualizer** strength sliders (each independently effective only where the device supports it).
- Effects are applied to the app's own playback only, via Android audio effects (`android.media.audiofx`) attached to the ExoPlayer audio session inside `PlaybackService`.
- All equalizer state (enabled, preset choice, custom band levels, bass boost strength, virtualizer strength) is persisted on the device and re-applied automatically on relaunch and whenever playback starts.
- Devices where an effect cannot be created (no effect implementation) show the control as unavailable instead of crashing or silently failing.
- Now Playing gains an equalizer chip alongside the existing queue and sleep-timer chips.

## Capabilities

### New Capabilities

- `equalizer`: The audio-effects feature — enabling/disabling effects, choosing presets, adjusting per-band levels, bass boost and virtualizer strengths, persistence of all of it, and how the effects attach to the playback session and degrade on unsupported devices.

### Modified Capabilities

- `now-playing`: The "Secondary chips" requirement gains an equalizer chip that opens the equalizer surface.

## Impact

- **Code**: `PlaybackService` (owns and applies the effects, follows the player's audio session id), a new equalizer settings store (Preferences DataStore, same pattern as `SettingsRepository`), a new equalizer UI (sheet/screen under `ui/`), `NowPlayingScreen` (new chip), a small view model to bridge UI ↔ store.
- **APIs**: `android.media.audiofx.Equalizer`, `BassBoost`, `Virtualizer` — all available since API 9, well within minSdk 26. No new dependencies.
- **Behavior**: Effects apply only to RavMusic's audio session, not system-wide. No change to the media notification, session commands, or library features.
