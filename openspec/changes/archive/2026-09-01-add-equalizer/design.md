# Design: add-equalizer

## Context

See `proposal.md` for motivation. What shapes the approach:

- `PlaybackService` is the one place audio is played (spec F4): a Media3 `MediaSessionService` owning an ExoPlayer. Audio effects must live there, attached to the player's audio session id.
- UI talks to the service only through a `MediaController`; the controller does not expose the audio session id, so the UI can never create the effects itself.
- Persistent preferences use Preferences DataStore (`SettingsRepository`, design D1 of `settings`). The sleep timer shows the established pattern for service-owned features: custom session commands in, `sessionExtras` out.
- Equalizer capabilities (band count, centre frequencies, level range, preset names, whether bass boost / virtualizer exist) are device-dependent and only discoverable from live `android.media.audiofx` effect instances — i.e. only inside the service.

## Goals / Non-Goals

**Goals:**
- Effects fully owned by the service, driven by persisted state, so they apply on any playback with no UI involvement.
- One source of truth for equalizer state; no transient runtime state to sync.
- Survive device variance: any band count, empty preset lists, missing effects, effect-creation failures.

**Non-Goals:**
- Reverb, loudness enhancer, or any other `audiofx` effect.
- Per-song or per-headphone profiles; a single global setting is enough.
- Exposing our session to third-party equalizer apps (`ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION` broadcasts).

## Decisions

### D1: DataStore is the single source of truth; the service observes it
All equalizer state (enabled, preset index or Custom, custom band levels, bass boost / virtualizer strengths) lives in the existing Preferences DataStore behind a new `EqualizerSettingsRepository` (same file, new keys, same style as `SettingsRepository`). The UI writes to the repository; `PlaybackService` collects the flow and applies changes to the effect instances.

*Why not session commands to change state (like the sleep timer)?* The sleep timer has genuinely transient runtime state (remaining time), so the service must own it. Equalizer state is 100% persistent settings — routing writes through the session would just add a second path that ends in DataStore anyway. Observing the store gives "applies on relaunch without opening the UI" for free and keeps the service the only writer of effect objects.

### D2: Effects are created eagerly in `onCreate`, disabled by default
The service creates `Equalizer`, `BassBoost` and `Virtualizer` on the player's audio session id at startup, each wrapped in try/catch (creation throws on unsupported devices), with `setEnabled(false)` until settings say otherwise. Eager creation means capabilities are known immediately and toggling later is just `setEnabled`, which is glitch-free mid-playback. Effect priority 0; released in `onDestroy`. If the audio session id changes (ExoPlayer can renew it; listen via `AnalyticsListener.onAudioSessionIdChanged`), release and recreate the effects and re-apply the stored state.

### D3: Capabilities travel over a custom session command, not `sessionExtras`
A new `EqualizerCommands.GET_CAPABILITIES` session command returns a Bundle: band count, per-band centre frequency, level range, preset names, and three "supported" flags (equalizer, bass boost, virtualizer). The equalizer view model requests it once when the surface opens.

*Why not `sessionExtras` like the sleep timer?* The sleep-timer collector already overwrites `sessionExtras` wholesale; adding a second writer means merging extras and a subtle ordering bug for no benefit — capabilities are static per service lifetime, a one-shot request fits better. (Anything already writing `sessionExtras` stays untouched.)

### D4: Storage format survives device changes
- `eq_enabled: Boolean` (default false)
- `eq_preset: Int` (device preset index; `-1` = Custom, the default)
- `eq_band_levels: String` — comma-joined millibel values, e.g. `"300,0,-200,0,150"`
- `eq_bass_boost: Int`, `eq_virtualizer: Int` — strength 0–1000

When applying, band levels are clamped to the device's supported range; if the stored level count doesn't match the device's band count (restored backup, ROM change), the stored levels are ignored and treated as flat. A stored preset index beyond the device's preset count falls back to Custom.

### D5: UI is a modal bottom sheet opened from a Now Playing chip
`EqualizerSheet` follows `SleepTimerSheet`/`QueueSheet`: a `ModalBottomSheet` hosted by `NowPlayingScreen`, chip added to the existing secondary-chip row. Content: master switch, preset selector (horizontal chips), one vertical band slider per band labelled with its frequency (rotated `Slider`s in a `Row`), then bass boost and virtualizer sliders. Unsupported controls render disabled with a "Not supported on this device" note (spec: graceful degradation). A new `EqualizerViewModel` combines repository flows with the capabilities reply.

### D6: Slider writes are applied live but persisted debounced
Dragging a band fires many changes per second. The view model applies every change to DataStore, but writes during a drag are conflated (latest-wins via a `MutableStateFlow` collected with `collectLatest`/debounce ~100 ms) so the service hears a smooth stream and the disk isn't hammered. Releasing the slider guarantees a final persist.

## Risks / Trade-offs

- [Emulators often lack effect implementations] → the graceful-degradation path is first-class, and manual testing happens on a real device; unit tests cover the settings logic, not the audio path.
- [Audio session id renewal detaches effects silently] → recreate-and-reapply on `onAudioSessionIdChanged` (D2); verified manually by toggling tracks/outputs.
- [`audiofx` constructors and setters throw `RuntimeException`/`IllegalStateException` on flaky HALs] → every effect call goes through a small wrapper that catches, logs, and marks the effect unsupported rather than crashing the service.
- [DataStore write latency makes drags feel laggy through the service round-trip] → conflated writes (D6); if still audible-laggy, acceptable — the final value always lands.
- [Custom levels lost when band count changes across devices] → explicit reset-to-flat rule (D4); rare and self-healing.

## Migration Plan

Purely additive: new DataStore keys default to "effects off", so existing installs behave identically until the user opts in. No rollback concerns beyond removing the UI entry point.
