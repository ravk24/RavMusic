## Why

Album art will never be a feature of RavMusic (decided 2026-08-29). The deterministic gradient tiles that stood in for it (D-02) now only take space and promise something that is not coming, and the docs still describe embedded art as "later". The Settings footer's privacy line is also being replaced by a maker credit.

## What Changes

- **No art anywhere**: the gradient placeholders on Now Playing, the mini player, the playlist cards, the playlist detail header and the add-to-playlist sheet are removed; layouts close up around the text.
- **Settings footer** reads the version and "Built by Ravi Kant", in the palette's secondary-text colour so it is legible in both themes.
- Docs and decisions stop describing album art as present or deferred (D-02 superseded by D-56).
- Version 1.0.2 (versionCode 3).

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `now-playing`: "Content" — no art; the screen shows title, artist and origin.
- `mini-player`: "Content and controls" — no art tile.
- `playlists`: "Playlists home grid" and "Playlist detail" — no art tiles.
- `app-shell`: "Settings entry point" — the footer states the version and "Built by Ravi Kant".

## Impact

- `ui/components/GradientArt.kt` and its test are deleted; five composables lose a `Box`; `SettingsScreen` footer text/colour; `versionCode`/`versionName`.
- `Build-plan/` decisions, spec and phase pages, root `README.md`.
- No dependency or permission changes.
