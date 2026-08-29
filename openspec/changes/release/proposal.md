## Why

Every feature in the spec is built and verified on debug builds. What the phone actually runs should be a shrunk, locally signed release APK that can be updated in place for years (spec: "locally signed release APK, sideloaded", never the Play Store). Nothing user-visible changes; this is packaging.

## What Changes

- **Release build**: R8 code shrinking and resource shrinking on for `release` (the AGP 9 `optimization { enable = true }` DSL), with the project's keep rules in `app/src/main/keepRules/`.
- **Signing**: a signing config read from a git-ignored `keystore.properties` (or environment variables) so `gradlew assembleRelease` produces a signed APK on this machine and an unsigned one anywhere the secrets are absent. The keystore lives outside the repository and is git-ignored.
- **Repeatable update path**: documented in `Build-plan/README.md` — bump `versionCode`, `assembleRelease`, install over the previous build; same signature means playlists and settings survive.
- **Sanity checks** on the release artifact: permissions (no `INTERNET`), size, cold start, playback in the background, install-over-self keeps data.

Explicitly not in this change: Play Store, app bundles, crash reporting, analytics, version bumps beyond `1.0` (this is the first release).

## Capabilities

### New Capabilities
None.

### Modified Capabilities
None — no spec-level behaviour changes (`skip_specs: true`).

## Impact

- `app/build.gradle.kts` (release build type, signing config), `Build-plan/README.md` (build/sign/sideload how-to), `keystore.properties` (signing entries, git-ignored, not committed).
- A keystore created once under the user's home directory; its alias and location recorded in the phase page, never its password.
- No code, dependency or permission changes.
