# RavMusic — Build Plan

A personal, offline, ad-free Android music player. This folder is the human-readable plan that sits
beside the code: the product spec, the phase roadmap, and the decisions that shaped it.

| Document | What it is |
|---|---|
| [music-player-spec.md](music-player-spec.md) | The product spec — features F1–F9, data model, constraints, build order |
| [decisions.md](decisions.md) | Settled design/engineering decisions, with dates and rationale |
| [phases/](phases/) | One page per build phase: goal, scope, OpenSpec change, verification, status |

The authoritative *machine-readable* plan for each phase lives in `openspec/changes/<change>/`
(proposal, specs, design, tasks). This folder explains; OpenSpec executes.

## Roadmap

| # | Phase | OpenSpec change | Status |
|---|---|---|---|
| 0 | Build configuration — AGP 9 built-in Kotlin, Compose, minSdk 26 | *(pre-OpenSpec, commit `19196ac`)* | ✅ Done |
| 1 | [Skeleton](phases/01-skeleton.md) — theme, bottom nav, permission flow, app icon | `app-skeleton` | ✅ Done |
| 2 | [Library](phases/02-library.md) — MediaStore scanner, folder browser | `library-browser` | ✅ Done |
| 3 | [Playback core](phases/03-playback-core.md) — service, tap-to-play, mini player | `playback-core` | ✅ Done |
| 4 | [Playlists](phases/04-playlists.md) — Room, CRUD, multi-select | `playlists` | ✅ Done |
| 5 | [Now Playing](phases/05-now-playing.md) — full screen, seek, shuffle/repeat, queue | `now-playing` | ✅ Done |
| 6 | [Sleep timer](phases/06-sleep-timer.md) | `sleep-timer` | ✅ Done |
| 7 | [Polish](phases/07-polish.md) — settings, missing files, empty states, motion, drag-select | `polish` | ✅ Done |
| 8 | [Ship](phases/08-ship.md) — release build, sign, sideload | `release` | ✅ Done |
| 9 | Open with — appear in Android's "Open with" / share sheet for audio files (1.1.0, D-58) | `open-with` | ✅ Done |
| 10 | Search — filter in playlist / folder detail, search across all playlists (1.1.0, D-59) | `search` | ✅ Done |

Each phase ends with something installable on the actual phone.

## How a phase runs

```
/opsx:explore <topic>      think it through (optional)
/opsx:propose "<change>"   writes proposal / specs / design / tasks under openspec/changes/<change>/
/opsx:apply <change>       implements the tasks, ticking them off as it goes
/opsx:archive <change>     folds the delta specs into openspec/specs/ once shipped
```

## Building

```powershell
# JAVA_HOME is not set globally on this machine; Android Studio's bundled JDK works.
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug                       # APK -> app/build/outputs/apk/debug/app-debug.apk
.\gradlew.bat testDebugUnitTest                   # JVM unit tests
.\gradlew.bat connectedDebugAndroidTest           # Compose UI tests (needs a device/emulator)
```

## Release build, signing, sideloading (Phase 8)

The phone runs a shrunk (R8 + resource shrinking), locally signed release APK. Signing secrets are
read from `keystore.properties` at the repo root (git-ignored) or from environment variables with the
same names; without them `assembleRelease` still works and produces an **unsigned** APK.

```properties
# keystore.properties  (never committed)
RAVMUSIC_STORE_FILE=C:/Users/<you>/.android/ravmusic-release.jks
RAVMUSIC_STORE_PASSWORD=...
RAVMUSIC_KEY_ALIAS=ravmusic
RAVMUSIC_KEY_PASSWORD=...
```

The keystore was created once on 2026-08-29 with
`keytool -genkeypair -keystore %USERPROFILE%\.android\ravmusic-release.jks -storetype PKCS12 -alias ravmusic -keyalg RSA -keysize 2048 -validity 10000`.
**Back it up** (e.g. alongside your other secrets): a new keystore means the phone must uninstall the
old build first, which deletes the playlists.

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleRelease                     # -> app/build/outputs/apk/release/app-release.apk (+ mapping/release/mapping.txt)
$bt = "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0"
& "$bt\apksigner.bat" verify --print-certs app\build\outputs\apk\release\app-release.apk   # signed by CN=RavMusic
& "$bt\aapt2.exe" dump permissions app\build\outputs\apk\release\app-release.apk           # no INTERNET, ever
```

**Updating the phone (three steps):** bump `versionCode` (and `versionName` if you like) in
`app/build.gradle.kts` → `assembleRelease` → copy `app-release.apk` to the phone and open it (or
`adb install -r app-release.apk`). Same signature = installs over the old version; playlists and
settings survive. The first install needs "Install unknown apps" enabled for whatever opens the
file (Files, Drive, the browser…). A debug build cannot be updated by a release build (different
signature): uninstall it first.

## Emulator

An AVD was created with the command-line tools (`Sdk/cmdline-tools/latest`):

```powershell
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
& "$sdk\emulator\emulator.exe" -list-avds                       # RavMusic_API36 (Pixel 7, API 36, Google APIs)
& "$sdk\emulator\emulator.exe" -avd RavMusic_API36               # windowed
& "$sdk\emulator\emulator.exe" -avd RavMusic_API36 -no-window -no-audio -no-boot-anim   # headless
& "$sdk\platform-tools\adb.exe" shell getprop sys.boot_completed # "1" once booted
```

Useful during testing:

```powershell
adb shell pm grant  com.ravk24.ravmusic android.permission.READ_MEDIA_AUDIO   # READ_EXTERNAL_STORAGE on API <= 32
adb shell pm revoke com.ravk24.ravmusic android.permission.READ_MEDIA_AUDIO
adb shell cmd uimode night yes    # system dark mode on / no = off

# Test audio: push files, then make MediaStore index them. No ffmpeg needed — a Python `wave` script
# writing a 440 Hz WAV is enough; MediaStore reads the duration from the header.
adb push "alpha song.wav" /sdcard/Music/
adb shell content call --uri content://media/ --method scan_volume --arg external_primary            # API 29+
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Music/alpha%20song.wav   # API 26–28, per file
adb shell content query --uri content://media/external/audio/media --projection _id:title:bucket_display_name:duration:is_music   # _data instead of bucket_display_name on API 26–28
```

Run `adb` commands that mention device paths (`/sdcard/...`) from PowerShell: Git Bash rewrites them into
Windows paths.

## Hard constraints (never change these)

- No ads, accounts, telemetry, subscriptions, API keys, or backend.
- The manifest does **not** declare `INTERNET`. The app physically cannot go online.
- Lightweight: no analytics SDKs, no crash reporters, no bloat dependencies.
- Personal use only — locally signed APK, sideloaded, never on the Play Store.
