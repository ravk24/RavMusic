## Why

The mini player is the app's only player UI: there is no way to seek, skip, see what is queued, or turn shuffle and repeat on and off — even though the session already supports all of them and Shuffle play already turns shuffle on invisibly. Phase 5 of the build order ("Now Playing") adds the full-screen player (spec F5 and the rest of F7, mockups 1f / 1k) that the mini player has been reserving its tap for since Phase 3.

## What Changes

- **Now Playing screen**, pushed full screen when the mini player is tapped: large gradient art, title, artist, "Playing from: <origin>", a seek bar with elapsed/total, and the transport row shuffle — previous — play/pause — next — repeat. A collapse affordance and system back return to where the user was. Neither the bottom navigation bar nor the mini player is shown while it is open.
- **Shuffle and repeat**: the shuffle toggle and the repeat cycle (Off → All → One → Off) read and write the session's state; both persist for the session and are visible again when the screen is reopened. Repeat-one loops the song; repeat-all continues from the last song to the first.
- **Queue sheet** from a "Queue · N left" chip: the current song and everything that will play after it, in the order it will play (the shuffled order when shuffle is on); tap a row to jump to it; drag the handle to reorder. Reordering while shuffle is on fixes the shown order as the queue and turns shuffle off.
- A **sleep-timer chip** placeholder in the secondary row, inert until the sleep-timer change.
- Position updates at least four times a second while the screen is visible (unchanged elsewhere).
- The `mini-player` "Tap reserved for Now Playing" requirement is fulfilled (tap expands); the `playback` requirements drop the "no repeat in this change" limitation and expose shuffle, repeat and the queue.

Explicitly not in this change: sleep-timer behaviour, crossfade, lyrics, equaliser, the expand/collapse animation between the mini player and the screen (Phase 7).

## Capabilities

### New Capabilities
- `now-playing`: the full-screen player — how it opens and closes, what it shows, seeking, transport controls, refresh rate, and the secondary chips.
- `shuffle-repeat`: shuffle and repeat semantics — toggling, cycling, persistence within the session, looping/wrapping behaviour, and how shuffle interacts with queue reordering.
- `queue`: the queue sheet — contents and order, the remaining count, jumping, reordering, and staying in sync with the session.

### Modified Capabilities
- `mini-player`: "Tap reserved for Now Playing" — tapping the body now opens the Now Playing screen (scenario kept and rewritten).
- `playback`: "Queue built from a folder" no longer states that playback stops with no repeat; "UI reflects the session" additionally exposes shuffle, repeat mode, the play-order queue and the current position in it.

## Impact

- `playback/PlayerState` gains shuffle, repeat, queue and index fields; `PlayerBridge`/`PlayerConnection` gain seek, next/previous, shuffle/repeat setters, jump and move; a pure play-order walk over Media3's timeline.
- `PlayerViewModel` forwards the new commands; the shell's many player lambdas are bundled into one `PlayerActions` value passed to `AppNavigation`.
- New `NowPlaying` route in `ui/navigation/`, `ui/nowplaying/{NowPlayingScreen,QueueSheet}.kt`, six icons; `ReorderableTrackList` is generalised into `ReorderableList<T>` shared by the playlist detail and the queue sheet.
- Tests: JVM tests for the play-order walk and repeat cycle; instrumented service tests for seek/skip/repeat/shuffle/jump/move; Compose tests for the screen, the sheet and the navigation. No new dependencies, no manifest changes, no `INTERNET`.
