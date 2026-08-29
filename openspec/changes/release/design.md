## Context

See proposal.md. `app/build.gradle.kts` has `release { optimization { enable = false } }` and no signing config; `app/src/main/keepRules/rules.keep` exists from Phase 0. `*.jks` / `*.keystore` and `local.properties` are git-ignored. Both emulators (API 36, API 26) are available; the user's phone is not attached to this machine, so sideloading to the phone itself is documented, not performed.

## Goals / Non-Goals

**Goals:** one command (`gradlew assembleRelease`) yields a signed, shrunk APK on this machine; the same command still builds (unsigned) elsewhere; updates install over the previous version without losing playlists or settings.

**Non-Goals:** app bundles, Play Store, CI, obfuscation mapping management beyond keeping `mapping.txt` with the build output.

## Decisions

### D1. Signing secrets come from a git-ignored `keystore.properties`, falling back to environment variables
`RAVMUSIC_STORE_FILE`, `RAVMUSIC_STORE_PASSWORD`, `RAVMUSIC_KEY_ALIAS`, `RAVMUSIC_KEY_PASSWORD` in `keystore.properties` at the repo root (added to `.gitignore`), or as environment variables. The Gradle script reads them at configuration time; when `RAVMUSIC_STORE_FILE` is absent no signing config is created and `assembleRelease` produces `app-release-unsigned.apk`.
*Alternatives:* `local.properties` — Android Studio owns that file and warns it may rewrite it; Android Studio's wizard — not repeatable from the command line.

### D2. One keystore, outside the repository, created with `keytool`
`%USERPROFILE%\.android\ravmusic-release.jks`, alias `ravmusic`, RSA 2048, 10 000 days, generated once by this change with a random password that is written only to `local.properties`. Losing the keystore means the next update cannot install over the old one, so the phase page records the location and alias (never the password) and the README says to back it up.

### D3. R8 and resource shrinking on, keep rules only when something breaks
`optimization { enable = true }`; the release APK is smoke-tested on both emulators (permission flow, library, playback in the background, playlists, settings persistence, sleep timer). Libraries in use (Compose, Media3, Room, kotlinx.serialization, DataStore, Navigation 3) ship consumer keep rules; project rules are added to `app/src/main/keepRules/rules.keep` only for a verified failure.

### D4. Versioning stays `versionCode 1` / `versionName 1.0` for the first release
Each later release bumps `versionCode` by one (and `versionName` as wished) before `assembleRelease`; the README shows the three-step update.

## Risks / Trade-offs

- [R8 breaks reflection-based code] → smoke test on both APIs; Room and serialization generate code rather than reflect, and their consumer rules cover the rest.
- [Keystore lost] → documented backup advice; a new keystore means uninstall-and-reinstall once (playlists are lost unless exported — out of scope).
- [Password in `local.properties`] → the file is git-ignored and local; the user can move the values to environment variables at any time.

## Migration Plan

None. Rollback: set `optimization { enable = false }` and remove the signing config.
