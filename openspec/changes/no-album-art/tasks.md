## 1. Code

- [x] 1.1 Delete `ui/components/GradientArt.kt` and `GradientArtTest.kt`; remove the gradient boxes from `NowPlayingScreen` (weighted spacer instead), `MiniPlayer`, `PlaylistsScreen.PlaylistCard` (bordered text tile, min height 88 dp), `PlaylistDetailScreen` header and `PlaylistDialogs` sheet rows; verify `assembleDebug testDebugUnitTest` is green and `grep artGradient` finds nothing
- [x] 1.2 Settings footer: "v<version>\nBuilt by Ravi Kant" in `onSurfaceVariant`; bump to versionCode 3 / 1.0.2; verify `SettingsScreenTest` asserts the credit and the absence of "INTERNET"
- [x] 1.3 Update `NowPlayingScreenTest.shortScreen_keepsChipsBelowTheControls` (no `np_art`); run the full connected suite on API 36 and API 26 (uninstall the release build first); verify all green

## 2. Docs and ship

- [x] 2.1 Docs: D-02 marked superseded, D-28 annotated, new D-56 "No album art, ever"; `music-player-spec.md` mini player / Now Playing bullets; phase pages 01, 02, 03, 04, 05, 07; root `README.md`; `08-ship.md` line for 1.0.2; verify `grep -ri "album art\|gradient art" Build-plan README.md` only hits the superseded/historical notes
- [x] 2.2 Screenshots on API 36 (light + dark): Playlists grid, mini player, Now Playing, Settings footer; `assembleRelease` → `app/release/RavMusic-1.0.2.apk` signed by CN=RavMusic with unchanged permissions; commit, push, archive the change and fix the `now-playing` Purpose line
