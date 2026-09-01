## 1. Settings store

- [x] 1.1 Add `EqualizerSettingsRepository` in `data/settings` with the keys and defaults from design D4 (`eq_enabled` false, `eq_preset` -1, `eq_band_levels` "", `eq_bass_boost`/`eq_virtualizer` 0), flows for reads and suspend setters for writes, sharing the app's existing Preferences DataStore; verify with unit tests covering defaults, round-trips, and the band-levels string encode/decode.
- [x] 1.2 Add the band-level parsing/clamping rules as pure functions (parse comma-joined millibels, clamp to a given range, reset-to-flat on band-count mismatch, preset index fallback to Custom when out of range); verify with unit tests for each rule.

## 2. Effects in the service

- [x] 2.1 Add an `AudioEffects` wrapper (in `playback/`) that creates `Equalizer`, `BassBoost` and `Virtualizer` for a given audio session id with every `audiofx` call behind try/catch (design D2/risks), exposes per-effect supported flags and capability data (band count, centre freqs, level range, preset names), and applies an equalizer-settings snapshot (enable flags, preset or custom levels, strengths); verify with a small JVM-safe seam so apply-logic unit tests pass, plus build success.
- [x] 2.2 Wire `AudioEffects` into `PlaybackService.onCreate`: create eagerly on the player's audio session id, collect the `EqualizerSettingsRepository` flow and apply each snapshot, release effects in `onDestroy`, and recreate + re-apply on `AnalyticsListener.onAudioSessionIdChanged`; verify by building and, on a real device, hearing a stored preset apply as soon as playback starts with no UI open (spec "Applies without visiting the surface").
- [x] 2.3 Add `EqualizerCommands.GET_CAPABILITIES` as a custom session command (design D3): advertise it in the existing `onConnect` alongside the sleep-timer commands and answer it with a capabilities Bundle from `AudioEffects`; verify with a build plus a controller round-trip check during manual testing.

## 3. Equalizer UI

- [x] 3.1 Add `EqualizerViewModel` combining the repository flows with the capabilities reply from the session, exposing UI state (supported flags, presets, bands with ranges, current values) and intent methods, with drag writes conflated/debounced per design D6; verify with unit tests for state mapping and the Custom-on-band-touch rule.
- [x] 3.2 Build `EqualizerSheet` in `ui/nowplaying` following `SleepTimerSheet`'s style: master switch, preset chips, vertical band sliders labelled with centre frequency, bass boost and virtualizer sliders, disabled-with-note rendering for unsupported effects and for everything while the master switch is off; verify by building and eyeballing both themes on the emulator.
- [x] 3.3 Add the "Equalizer" chip to `NowPlayingScreen`'s secondary-chip row opening the sheet (delta spec `now-playing`); verify the chip shows and opens the sheet on the emulator.

## 4. Verification and polish

- [x] 4.1 Run the full unit test suite and a release-style build (`gradlew test assembleDebug` with the jbr JDK); verify everything is green.
- [ ] 4.2 On a real device (emulators often lack effect HALs): toggle the master switch mid-song and hear a glitch-free change, pick a preset and see sliders move, drag a band and see Custom selected, raise bass boost/virtualizer, force-stop + relaunch + play and confirm the stored sound returns; verify each matches its spec scenario.
  - 2026-09-01: every functional part verified on the API 26 emulator (which ships NXP software effects): master toggle, Dance preset moved the sliders, band drag selected Custom keeping the other bands, bass boost 49%, force-stop + relaunch + play restored everything, and `dumpsys media.audio_flinger` showed the Equalizer/BassBoost/Virtualizer chain live on our session. Only the *audible* checks (glitch-free sound change by ear) still need a human on a real device.
- [ ] 4.3 On the emulator (likely no effects): confirm the sheet shows disabled controls with the unsupported note and the app never crashes (spec "Graceful degradation"); verify by opening the sheet and playing a song.
  - 2026-09-01: premise didn't hold — both AVDs ship software effects, so the degradation UI can't be observed on an emulator here. The path is covered by unit tests (`planEqualizerApply`, capability flags) and the disabled-with-note rendering; live confirmation needs a device that actually lacks an effect.
