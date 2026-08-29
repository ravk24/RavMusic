# Phase 6 — Sleep timer

**OpenSpec change:** `sleep-timer` · **Status:** ⏳ Planned

## Goal
Fall asleep to music without it playing all night (spec **F6**).

## In scope
- Moon chip in Now Playing → picker: 15 / 30 / 45 / 60 min, custom minutes, **End of current track**
- Timer runs as a coroutine *inside* `PlaybackService` (survives the app being backgrounded), driven by
  a custom `SessionCommand` (`SET_SLEEP_TIMER`, `CANCEL_SLEEP_TIMER`) and reported back via session extras
- Last 10 s: fade volume to 0, then **pause** (not stop) and restore volume so resume works normally
- "End of current track" hooks `onMediaItemTransition`
- Active timer shown in Now Playing ("Sleep · 32:14 · tap to extend"); tap → cancel or extend

## Out of scope
Scheduling for a future time; system alarm integration.

## Capabilities
`sleep-timer` (new)

## Key risks
- Doze / app-standby could delay a long timer → foreground service keeps the process alive; verify
  with `adb shell dumpsys deviceidle force-idle`

## Verification
- Set 1 min (custom): fade starts at 50 s, pause at 60 s, volume restored on resume
- "End of current track" with 20 s left: pauses exactly at the transition
- Background the app for the whole duration: still fires
