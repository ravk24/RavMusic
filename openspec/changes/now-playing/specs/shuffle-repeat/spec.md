## Purpose

Shuffle and repeat: how they are toggled from the player, how they affect the order songs play in, and how they persist while the session lives.

## ADDED Requirements

### Requirement: Shuffle toggle
The Now Playing screen SHALL show a shuffle control that reflects the session's shuffle state and toggles it. Turning shuffle on SHALL keep the current song playing and randomise the order of the remaining songs; turning it off SHALL restore the queue's original order. Shuffle play from a playlist SHALL show shuffle as on.

#### Scenario: Toggle on
- **WHEN** a folder queue is playing in order and the user taps shuffle
- **THEN** the current song keeps playing, shuffle shows as on, and the upcoming songs in the queue sheet are in a different order

#### Scenario: Shuffle play shows on
- **WHEN** the user starts a playlist with Shuffle play and opens Now Playing
- **THEN** the shuffle control shows as on

### Requirement: Repeat cycle
The screen SHALL show a repeat control that cycles Off → All → One → Off on each tap and indicates the current mode. With repeat off, playback SHALL stop after the last song. With repeat all, the first song SHALL follow the last. With repeat one, the current song SHALL restart when it ends.

#### Scenario: Cycle
- **WHEN** the user taps repeat three times starting from off
- **THEN** the mode goes All, then One, then Off, and the control shows each state

#### Scenario: Repeat one loops
- **WHEN** repeat is set to One and the current song ends
- **THEN** the same song starts again from the beginning

#### Scenario: Repeat all wraps
- **WHEN** repeat is set to All and the last song of the queue ends
- **THEN** the first song of the queue starts

### Requirement: Session persistence
Shuffle and repeat SHALL keep their values while the session lives — across leaving and reopening Now Playing, backgrounding the app, and replacing the queue — until the user changes them. Starting a folder or playlist with Play (not Shuffle play) SHALL turn shuffle off.

#### Scenario: Survives reopening
- **WHEN** the user sets repeat to All, closes Now Playing, and reopens it later
- **THEN** repeat still shows All

#### Scenario: Plain play turns shuffle off
- **WHEN** shuffle is on and the user taps Play on a playlist
- **THEN** the playlist plays in order and shuffle shows as off

### Requirement: Reordering under shuffle
If the user reorders the queue while shuffle is on, the order shown in the queue sheet SHALL become the queue's fixed order, the reorder SHALL be applied to it, and shuffle SHALL turn off; the current song SHALL continue playing.

#### Scenario: Drag while shuffled
- **WHEN** shuffle is on and the user drags a queued song to play next
- **THEN** that song plays next, the remaining songs keep the order that was shown, and shuffle shows as off
