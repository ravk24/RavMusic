# equalizer Specification (delta)

## Purpose

Lets the user shape RavMusic's sound: a switchable equalizer with device presets and per-band custom levels, plus bass boost and virtualizer, applied to the app's own playback only and remembered across restarts.

## ADDED Requirements

### Requirement: Equalizer surface and master switch
The app SHALL provide an Equalizer surface, opened from the Now Playing screen, with a master switch that turns all audio effects (equalizer, bass boost, virtualizer) on or off together. The switch SHALL default to off, so a fresh install sounds exactly as before. While the switch is off the preset, band and strength controls SHALL be visibly disabled but SHALL keep showing their stored values. Turning the switch on or off SHALL take effect on currently playing audio without interrupting playback.

#### Scenario: Fresh install is untouched
- **WHEN** the app is freshly installed and a song plays
- **THEN** no audio effect is active and the Equalizer surface shows the master switch off

#### Scenario: Toggle while playing
- **WHEN** a song is playing with a bass-heavy custom setting stored and the user turns the master switch on
- **THEN** the sound changes audibly without a pause or restart, and turning the switch off restores the unprocessed sound

### Requirement: Presets and custom band levels
With effects on, the Equalizer surface SHALL offer the device's equalizer presets by name plus a **Custom** option. Choosing a preset SHALL apply it immediately and update the band sliders to show that preset's levels. The surface SHALL show one slider per equalizer band (the device decides the count, typically five), each labelled with its centre frequency and spanning the device's supported level range. Moving any band slider SHALL switch the selection to Custom and apply the new level immediately.

#### Scenario: Preset applies and shows its shape
- **WHEN** the user chooses the "Rock" preset
- **THEN** playback changes to the preset's sound and the band sliders move to the preset's levels with "Rock" shown selected

#### Scenario: Touching a band goes Custom
- **WHEN** "Rock" is selected and the user drags the lowest band up
- **THEN** the selection changes to Custom, the new band level is audible immediately, and the other bands keep the levels Rock gave them

### Requirement: Bass boost and virtualizer
With effects on, the Equalizer surface SHALL offer a bass boost strength slider and a virtualizer strength slider, each from off (0%) to maximum (100%). Changes SHALL apply to playback immediately. Each control SHALL be independent of the equalizer preset/bands and of the other.

#### Scenario: Bass boost while playing
- **WHEN** a song is playing with effects on and the user raises bass boost from 0% to 60%
- **THEN** low frequencies are audibly boosted without interrupting playback, and the equalizer bands and virtualizer are unchanged

### Requirement: Settings persist and re-apply
All equalizer state — master switch, selected preset or Custom, custom band levels, bass boost strength, virtualizer strength — SHALL be persisted on the device as soon as it is changed. The stored state SHALL be re-applied automatically whenever the app plays audio, including after the process is killed and relaunched, without the user opening the Equalizer surface.

#### Scenario: Survives relaunch
- **WHEN** the user enables effects with a Custom curve and 40% bass boost, force-stops the app, relaunches it and plays a song
- **THEN** the song plays with the same curve and bass boost, and the Equalizer surface shows the saved values

#### Scenario: Applies without visiting the surface
- **WHEN** effects were left on and the user plays a song from a folder without ever opening the Equalizer surface
- **THEN** the stored effects are active on that playback

### Requirement: Effects are app-local
The effects SHALL apply only to RavMusic's own playback session. Audio from other apps SHALL be unaffected, whether RavMusic is playing, paused or closed.

#### Scenario: Other apps unaffected
- **WHEN** RavMusic has effects on and the user plays a video in another app
- **THEN** the other app's audio is unprocessed

### Requirement: Graceful degradation on unsupported devices
On a device where an effect cannot be created or a capability is missing, the corresponding controls SHALL be shown disabled with a short explanation (for example "Not supported on this device") instead of crashing, and the remaining supported effects SHALL keep working. A device reporting no equalizer presets SHALL still offer Custom band control if the equalizer itself works.

#### Scenario: No virtualizer
- **WHEN** the device cannot create a virtualizer effect and the user opens the Equalizer surface with effects on
- **THEN** the virtualizer slider is disabled with an unsupported note while the equalizer and bass boost work normally
