## Context

See proposal.md — Why. Current state after `now-playing`: `PlaybackService` owns the ExoPlayer inside a `MediaSession` built with the default callback; `PlayerConnection` mirrors the controller into `PlayerState` and forwards commands; `PlayerActions` bundles the shell's player lambdas; `NowPlayingScreen` shows an inert "Sleep timer" `AssistChip` (`np_sleep_chip`) and already runs a 250 ms refresh loop while visible. Requirements are in `specs/sleep-timer/spec.md` and the `now-playing` delta.

Constraints: the timer must not depend on the activity (spec F6); no new permissions (exact-alarm permissions are out — the playback wake lock already keeps the CPU awake while music plays); Media3 through the `MediaController` only; mockup 1f chip states.

## Goals / Non-Goals

**Goals:**
- Countdown, fade and pause logic that is pure enough to unit-test with a virtual clock.
- One channel between UI and service for both directions: custom session commands in, session extras out.
- Timer state on `PlayerState` like everything else, so the chip is a plain function of state.

**Non-Goals:**
- Alarm-manager scheduling, clock-time scheduling, notification actions, mini-player display.

## Decisions

### D1. `SleepTimerEngine` runs in the service, over a tiny player-actions interface
`playback/SleepTimerEngine(actions: SleepTimerActions, scope: CoroutineScope, clock: () -> Long, fadeMs = 10_000, stepMs = 250)`. `SleepTimerActions { var volume: Float; fun pause(); }` is implemented by the service over the ExoPlayer. `set(durationMs)` cancels any job, records `endAt = clock() + duration`, launches: `delay(endAt - fadeMs - now)`, then steps the volume down linearly every `stepMs` until `endAt`, then `pause()`, `volume = original`, state → Off. `extend(ms)` moves `endAt` and restarts the job (restoring the volume if a fade had begun). `cancel()` restores the volume and clears. `endOfTrack()` sets a flag consumed by the service's `onMediaItemTransition(AUTO)`: `pause()`, `seekTo(0)` on the new item, state → Off. State is a `StateFlow<SleepTimerState>`; JVM tests drive it with `runTest` and a virtual clock.
*Alternatives:* `AlarmManager` — needs exact-alarm permissions on 31+ and is overkill while a wake lock is held; a WorkManager job — not precise.

### D2. Custom session commands in, session extras out
`SleepTimerCommands` defines `SessionCommand`s `ravmusic.sleep.set` (args `duration_ms` or `end_of_track`), `ravmusic.sleep.extend` (`extra_ms`), `ravmusic.sleep.cancel`. The service's `MediaSession.Callback.onConnect` adds them to the available session commands; `onCustomCommand` dispatches to the engine. The engine's state is published with `session.setSessionExtras(bundle)` (`sleep_end_at` elapsed-realtime millis, or `sleep_end_of_track = true`, or neither), which reaches every controller through `MediaController.Listener.onExtrasChanged` and is readable on connect via `controller.sessionExtras`.
*Alternatives:* `MediaController.setPlaybackSpeed`-style abuse of standard commands — no; a bound-service AIDL — a second IPC path.

### D3. `SleepTimerState` on `PlayerState`, remaining time computed in the UI
`sealed interface SleepTimerState { Off; Countdown(endAtElapsedMs); EndOfTrack }`. The UI computes `remaining = endAt - SystemClock.elapsedRealtime()` on each 250 ms tick (the screen's existing loop), so no extra messages flow while counting. `formatRemaining(ms)` → "mm:ss" or "h:mm:ss" (pure).

### D4. The chip and the sheet
`np_sleep_chip` becomes enabled: Off → label "Sleep timer" (outlined); active → "Sleep · 32:14 · tap to extend" / "Sleep · end of track" in the accent (mockup 1f, `Lavender` container with `LavenderBorder`). Tap opens `SleepTimerSheet`: Off → presets 15/30/45/60 as `FilterChip`s, a custom-minutes field with Set (1–600), "End of current track"; active → remaining time, "Extend 15 min", "Cancel timer". The sheet is disabled/hidden when no queue is loaded (chip disabled).

### D5. Manual pause and the fade
If the user pauses before the countdown ends, the fade still runs (volume changes are harmless while paused) and the final `pause()` is a no-op; the volume is restored either way, so the next play is at full volume. Cancel/extend during the fade restore the volume immediately.

### D6. File layout
```
app/src/main/java/com/ravk24/ravmusic/
  playback/SleepTimerEngine.kt      engine + SleepTimerActions + SleepTimerState (pure)
  playback/SleepTimerCommands.kt    SessionCommand names/keys, bundle <-> state mapping (pure-ish)
  playback/PlaybackService.kt       hosts the engine, session callback, transition listener, extras
  playback/PlayerState.kt           + sleepTimer
  playback/PlayerBridge.kt / PlayerConnection.kt   commands + extras listener
  playback/PlayerActions.kt, PlayerViewModel.kt    forwards
  ui/nowplaying/SleepTimerSheet.kt, NowPlayingScreen.kt (chip)
  ui/components/Format.kt           formatRemaining
```

## Risks / Trade-offs

- [Doze deferring the coroutine delay on a long timer] → the player holds `WAKE_MODE_LOCAL` while playing, which keeps the CPU awake; verified with `adb shell dumpsys deviceidle force-idle` during a short timer. If a device still defers it, the fade begins late but the pause still happens.
- [Service killed while paused after the timer fires] → nothing to preserve: the timer is already off.
- [Volume fade on the ExoPlayer affects only this app's stream] → intended.
- [Extras arrive asynchronously after connect] → `PlayerConnection` reads `sessionExtras` on connect and again on `onExtrasChanged`; the chip simply follows `PlayerState`.

## Migration Plan

No persisted data. Rollback is `git revert`.
