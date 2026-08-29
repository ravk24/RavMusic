## Context

See proposal.md — Why. Current state after `playlists`: `PlayerConnection` mirrors a Media3 `MediaController` into `PlayerState(nowPlaying, isPlaying, positionMs, durationMs)`; `PlayerViewModel` (Activity-scoped) runs a 500 ms ticker while playing; `AppNavigation` receives player state as a value and four player lambdas; `MiniPlayer(onExpand)` is wired to a no-op; `ReorderableTrackList` implements long-press drag reorder for `PlaylistTrack` rows; Shuffle play already sets `shuffleModeEnabled` on the controller. Requirements are in `specs/{now-playing,shuffle-repeat,queue}/spec.md` plus the `mini-player`/`playback` deltas.

Constraints: no new dependencies; Media3 through the `MediaController` only (no ExoPlayer-only APIs such as `setShuffleOrder`); fixed palette + gradient art; Nav3 shell whose bottom slot already knows about detail routes; mockups 1f / 1k.

## Goals / Non-Goals

**Goals:**
- Everything the screen shows is derivable from the controller, so a reconnect after process recreation shows the same state.
- The play-order walk (what the queue sheet lists) is pure and JVM-tested; the only Media3-specific part is reading `Timeline.getNextWindowIndex`.
- One route, one bundle of actions, one generic reorderable list.

**Non-Goals:**
- Expand/collapse animation (Phase 7); sleep timer (Phase 6); showing the queue anywhere but the sheet; persisting shuffle/repeat across service restarts.

## Decisions

### D1. Now Playing is a Nav3 route, not a sheet
`NowPlaying` is a `@Serializable` `NavKey` pushed onto whatever is below; the shell's bottom column renders neither the navigation bar nor the mini player while it is the top entry. Back and the collapse affordance both `removeLastOrNull()`. A `LaunchedEffect(playerState.hasQueue)` pops the entry when the queue disappears.
*Alternatives:* a `ModalBottomSheet` — back-stack semantics for free are worth more than the sheet look; Phase 7 animates the same route.

### D2. Play order comes from the timeline walk, exposed on `PlayerState`
`playOrder(count, currentIndex, shuffled, first, next)` (pure) walks `timeline.getFirstWindowIndex(shuffle)` → `getNextWindowIndex(i, REPEAT_MODE_OFF, shuffle)` and returns media indices in play order plus the position of the current song. `PlayerConnection.publish` maps those to `QueueEntry(songId, title, artist, mediaIndex)` via `getMediaItemAt`. Repeat is deliberately passed as OFF to the walk so the list never loops.

### D3. `RepeatMode` is an app enum mapped at the controller boundary
`RepeatMode { OFF, ALL, ONE }` with `next()` (OFF → ALL → ONE → OFF), converted to/from `Player.REPEAT_MODE_*` in `PlayerConnection`. Keeps `PlayerState` free of Media3 types.

### D4. Reorder: `moveMediaItem` when shuffle is off; freeze the shown order when shuffle is on
Shuffle off: `moveMediaItem(fromMediaIndex, toMediaIndex)` — uninterrupted. Shuffle on: the controller cannot set a custom shuffle order, so the connection rebuilds the queue as the displayed play order (`setMediaItems(itemsInPlayOrder, currentPos, currentPositionMs)`), turns shuffle off, then moves the item. The current song re-prepares at the same position (a brief gap), which the spec accepts by defining the behaviour ("fixes the order, shuffle off"). Jump uses `seekToDefaultPosition(mediaIndex)` in both modes.

### D5. `PlayerActions` bundle
One `class PlayerActions(onPlayPause, onDismiss, onPlaySongs, onShufflePlay, onSeek, onNext, onPrevious, onToggleShuffle, onCycleRepeat, onJumpTo, onMoveInQueue, onRefreshPosition)` constructed in `AppRoot` from `PlayerViewModel` and passed to `AppNavigation` in place of the four existing lambdas; tests build it with `PlayerActions.none()`.

### D6. Fast position ticker only while the screen is visible
`NowPlayingScreen` runs `LaunchedEffect(isPlaying) { while (isPlaying) { delay(250); onRefreshPosition() } }`; the ViewModel's 500 ms ticker keeps serving the mini player. The spec's "≥ 4×/s while visible" is met without changing the global rate.

### D7. Seek bar is a Material 3 `Slider` with local scrub state
`scrub: Float?` holds the value while dragging; `onValueChangeFinished` calls `onSeek((scrub × duration).toLong())` and clears it. Elapsed shows the scrub while dragging. Duration 0 (unknown) disables the slider.

### D8. Generic `ReorderableList<T>`
`ReorderableTrackList` becomes `ReorderableList(items, key, listState, onMove, modifier, itemContent: (index, item, handleModifier))`; the playlist detail wraps rows in its `SwipeToDismissBox` inside `itemContent`, the queue sheet uses plain rows. Same gesture code, one place (D-35 stays true).

### D9. File layout
```
app/src/main/java/com/ravk24/ravmusic/
  playback/PlayerState.kt          + RepeatMode, QueueEntry, shuffleEnabled, repeatMode, queue, queueIndex, remaining, hasNext/hasPrevious
  playback/PlayOrder.kt            pure playOrder()
  playback/PlayerBridge.kt         + seekTo, next, previous, setShuffle, setRepeat, jumpToQueuePosition, moveInQueue
  playback/PlayerConnection.kt     implementations, timeline walk in publish()
  playback/PlayerActions.kt        the bundle
  PlayerViewModel.kt               forwards
  ui/navigation/{Routes,AppNavigation}.kt   NowPlaying route, bottom slot rule, PlayerActions
  ui/nowplaying/{NowPlayingScreen,QueueSheet}.kt
  ui/playlists/ReorderableList.kt  generic
  ui/components/AppIcons.kt        + SkipNext, SkipPrevious, Repeat, RepeatOne, ExpandMore, Bedtime
```

## Risks / Trade-offs

- [Timeline walk cost on every `onEvents`] → queues are at most a folder or playlist (hundreds); the walk is O(n) and runs on the main thread only on player events, not on the position ticker.
- [`Slider` interaction in Compose tests] → tests drive `performTouchInput { swipeRight() }` on the slider and assert the seek callback received a value in the upper half; exact position is not asserted.
- [Reorder under shuffle re-prepares the current item] → documented in the spec; the alternative (silently ignoring the drag) is worse.
- [`previous` semantics] → `seekToPrevious()` uses Media3's `maxSeekToPreviousPosition` (3 s) — matches the spec's "restart or previous".
- [Queue sheet inside a pushed route with a ModalBottomSheet] → same pattern already used by the add-to-playlist sheet; back closes the sheet first.

## Migration Plan

No persisted data changes. Rollback is `git revert`.
