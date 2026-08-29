## 1. State and bridge

- [ ] 1.1 Add `RepeatMode` (with `next()`), `QueueEntry`, and the `shuffleEnabled` / `repeatMode` / `queue` / `queueIndex` fields with `remaining`, `hasNext`, `hasPrevious` to `playback/PlayerState.kt`; add pure `playOrder()` in `playback/PlayOrder.kt`; verify `PlayOrderTest` (linear, shuffled walk via a fake `next`, current position, remaining) and `RepeatModeTest` / `PlayerStateTest` additions pass
- [ ] 1.2 Add `seekTo`, `next`, `previous`, `setShuffle`, `setRepeat`, `jumpToQueuePosition`, `moveInQueue` to `PlayerBridge` and implement them in `PlayerConnection` (repeat mapping, timeline walk in `publish`, move policy: `moveMediaItem` when shuffle is off, freeze-then-move when on); verify `PlaybackServiceTest` additions with the 3 s tone: seek moves position, next/previous change the item, repeat One keeps item 0 playing past 3.5 s, repeat All wraps to item 0 after the last, shuffle toggle keeps the current song and reports a queue, jump plays the chosen entry, move with shuffle off reorders `queue`, move with shuffle on turns shuffle off and applies the move
- [ ] 1.3 Create `playback/PlayerActions.kt` and forward the new commands from `PlayerViewModel` (`refreshPosition()` public for the fast ticker); verify `PlayerViewModelTest` additions show each action reaches the bridge

## 2. Shell

- [ ] 2.1 Add `NowPlaying` to `Routes.kt`; in `AppNavigation` take a `PlayerActions` (replacing the four player lambdas), wire the mini player's `onExpand` to push `NowPlaying`, hide both the navigation bar and the mini player while it is the top entry, pop it when the queue disappears, and update `AppRoot` and every test call site (`PlayerActions.none()`); verify `NowPlayingNavigationTest`: mini player tap → `screen_now_playing`, no `bottom_bar` / `mini_player`, collapse and system back return to the tab with the mini player, queue cleared → screen pops

## 3. Screen

- [ ] 3.1 Add `AppIcons.SkipNext`, `SkipPrevious`, `Repeat`, `RepeatOne`, `ExpandMore`, `Bedtime`; verify they render in a preview
- [ ] 3.2 Create `ui/nowplaying/NowPlayingScreen.kt` (collapse, "Playing from" + origin, art, title, artist, `Slider` seek with scrub state and elapsed/total, shuffle / prev / play-pause / next / repeat with state tints, "Sleep timer" inert chip, "Queue · N left" chip, 250 ms ticker while playing); verify `NowPlayingScreenTest`: texts, elapsed/total, slider release calls `onSeek` with a value in the upper half, each button calls back, repeat icon reflects Off/All/One, shuffle tint state, sleep chip does nothing, queue chip calls back

## 4. Queue

- [ ] 4.1 Generalise `ReorderableTrackList` into `ReorderableList<T>` and re-implement the playlist detail on it (swipe wrapper inside `itemContent`); verify `PlaylistDetailScreenTest` (swipe and drag cases) still passes
- [ ] 4.2 Create `ui/nowplaying/QueueSheet.kt` (`ModalBottomSheet`, header "Queue · N left", rows in play order with the current one highlighted, tap → `onJump(position)`, handle drag → `onMove(from, to)`); verify `QueueSheetTest`: rows, highlight, count, tap jumps, drag moves
- [ ] 4.3 Wire the queue chip, jump and move through `PlayerActions` in the `NowPlaying` entry; verify `NowPlayingNavigationTest` gains a case that opening the sheet and tapping a row calls the jump action with that position

## 5. Integration

- [ ] 5.1 Run `.\gradlew.bat assembleDebug testDebugUnitTest` and `connectedDebugAndroidTest` on the API 36 and API 26 emulators; verify `BUILD SUCCESSFUL`, every test passes, and the merged manifest still has 0 hits for `INTERNET`
- [ ] 5.2 Manual walkthrough on both emulators with the WAVs: play a playlist → tap the mini player → Now Playing shows origin/art/title; drag the seek bar → `dumpsys media_session` position jumps; shuffle on/off and repeat cycle reflected in `dumpsys media_session`; repeat One keeps the same song after it ends; queue chip → sheet lists the remaining songs; tap a queued song → it plays; drag a queued song below the current one → it plays next; collapse and system back return with the mini player; screenshots light + dark + sheet; verify each `now-playing` / `shuffle-repeat` / `queue` scenario and note deviations here

## 6. Docs

- [ ] 6.1 Update `Build-plan/README.md` (row 5 ✅), `Build-plan/phases/05-now-playing.md` (status, result, task progress) and `Build-plan/decisions.md` (D-37…); commit on `main` and push; verify `git status` is clean and `git log -1` shows the commit
