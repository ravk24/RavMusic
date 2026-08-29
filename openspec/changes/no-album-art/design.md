## Context

Gradient art (`ui/components/GradientArt.kt`, D-02/D-28) is drawn in five composables and described in three live specs and several Build-plan pages. The footer in `SettingsScreen` uses `outlineVariant`, which is a border tone (≈2:1 contrast in light, ≈1.4:1 in dark).

## Goals / Non-Goals

**Goals:** remove every placeholder and every doc mention of album art as present or future; a legible footer credit; nothing else changes.
**Non-Goals:** new decoration in place of the art (icons, initials); layout redesign beyond closing the gaps.

## Decisions

### D1. Delete the helper, not just its call sites
`GradientArt.kt` and `GradientArtTest.kt` go; keeping a dead helper invites the feature back.

### D2. Layouts close up, nothing replaces the art
Now Playing keeps a weighted spacer above the title so the controls stay in the lower half and shrink-first behaviour on short screens is preserved; the mini player row starts with the text; the playlist card is a bordered text tile with a minimum height; the detail header and sheet rows lose their tiles.

### D3. Footer colour is `onSurfaceVariant`
Slate `#6B7C93` on white and SlateDark `#7FA3C3` on navy — the palette's secondary-text tone, already used for every subtitle, so it is legible in both themes without a new colour.

## Risks / Trade-offs

- [Grid looks plainer] → accepted; the user asked for the placeholders to go and chose not to add substitutes.
- [Tests referencing `np_art`] → the short-screen layout test drops those assertions.

## Migration Plan

None; rollback is `git revert`.
