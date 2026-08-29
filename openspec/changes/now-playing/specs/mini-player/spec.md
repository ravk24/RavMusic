## MODIFIED Requirements

### Requirement: Tap reserved for Now Playing
Tapping the mini player body SHALL open the Now Playing screen as specified by the `now-playing` capability. The play/pause button and the swipe gesture keep their own behaviour.

#### Scenario: Tap is inert
- **WHEN** the user taps the mini player's title area
- **THEN** the tap is no longer inert: the Now Playing screen opens

#### Scenario: Controls unaffected
- **WHEN** the user taps the mini player's play/pause button
- **THEN** playback toggles and the Now Playing screen does not open
