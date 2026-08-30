# Phase 8 — Ship

**OpenSpec change:** `release` (tasks only; no spec-level behaviour) · **Status:** ✅ Done (2026-08-29)

**Result:** `app-release.apk` 1.0 (versionCode 1) — **2.97 MB** (2,970,595 bytes) with R8 + resource
shrinking on and no project keep rules needed; signed with APK Signature Scheme v2 by
`CN=RavMusic, O=ravk24, C=IN` from `%USERPROFILE%\.android\ravmusic-release.jks` (alias `ravmusic`,
PKCS12, RSA 2048, valid 10 000 days; password only in the git-ignored `keystore.properties`).
`aapt2 dump permissions` lists exactly `READ_MEDIA_AUDIO`, `READ_EXTERNAL_STORAGE (maxSdk 32)`,
`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `WAKE_LOCK` — Media3's
`ACCESS_NETWORK_STATE` is stripped (D-55), and there is no `INTERNET`. `mapping.txt` sits in
`app/build/outputs/mapping/release/`. Smoke test on the release build, API 36 and API 26 emulators (debug build uninstalled first): fresh install → real permission
dialog → Folders → play → 20 s on Home with playback still advancing → Now Playing with the sleep-timer
and queue chips → long-press → "Added 1 to Road" → Settings Dark → `am force-stop` + relaunch still dark
→ `adb install -r` of the same APK over itself → the playlist and the Dark setting survive; `logcat`
shows no crash and no `ClassNotFound`/`NoSuchMethod` from R8. Without `keystore.properties` the same
command produces `app-release-unsigned.apk`. A copy of the shipped artifact and its `mapping.txt`
are kept in `app/release/` (git-ignored).
The phone was not attached to this machine, so the sideload itself follows the README recipe
(copy the APK, allow "Install unknown apps" once, open it).

**1.0.1 (versionCode 2), 2026-08-29:** Now Playing controls fix after the first phone screenshot — the art
now shrinks first on short screens and a minimum 16 dp gap sits above the chips (the play/pause circle
used to touch them), play/pause is the blurple accent with a white icon, the four side buttons share one
28 dp icon size, and the chips are smaller (`labelMedium`, 14 dp icons) with theme-token colours when the
sleep timer is active. Regression test `NowPlayingScreenTest.shortScreen_keepsChipsBelowTheControls`.

**1.0.2 (versionCode 3), 2026-08-29:** album art is out for good (D-56) — every gradient placeholder removed
(Now Playing, mini player, playlist cards, playlist header, add-to-playlist sheet); the Settings footer now
reads the version and "Built by Ravi Kant" in the secondary-text colour (D-57).

**1.1.0 (versionCode 4), 2026-08-30:** two post-ship features, changes `open-with` and `search` (D-58, D-59).
The app now appears in Android's "Open with" / share sheet for audio files and plays the chosen file as a
one-song queue straight into Now Playing; playlist and folder detail gained a title/artist filter, and the
Playlists home a search across every playlist (tap a hit to play that playlist from that song, or open it).
Manifest adds `VIEW` / `SEND` intent filters only — still no `INTERNET`. `app/release/RavMusic-1.1.0.apk`,
2,987,199 bytes (≈2.99 MB), same permissions as 1.0.2; 111 JVM + 130 instrumented tests green on API 36 and API 26.

## Goal
A signed release APK on the phone, and a repeatable way to update it.

## In scope
- Turn on R8 / resource shrinking for `release` (`optimization { enable = true }` in the AGP 9 DSL;
  currently off), add keep rules only if something breaks (`app/src/main/keepRules/`)
- Create a local keystore once; **never commit it** (`*.jks` / `*.keystore` are git-ignored); note
  the alias and location outside the repo
- `Build > Generate Signed Bundle / APK > APK` in Android Studio, or `gradlew assembleRelease` with
  signing config read from `local.properties` / environment
- Sideload: enable "Install unknown apps" for the file manager used; same signature = in-place update
- Bump `versionCode` / `versionName` per release
- Sanity pass on the release build: no `INTERNET` permission in the final merged manifest, APK size
  well under 10 MB, cold start, playback in background

## Out of scope
Play Store, crash reporting, analytics — permanently.

## Verification
- `aapt dump permissions app-release.apk` lists only storage + foreground-service permissions
- Install over the previous version without uninstalling; playlists survive
- Size of `app-release.apk` recorded here
