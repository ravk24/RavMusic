# Phase 8 — Ship

**OpenSpec change:** `release` (tasks only; no spec-level behaviour) · **Status:** ⏳ Planned

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
