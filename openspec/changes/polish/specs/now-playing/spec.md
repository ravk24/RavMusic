## MODIFIED Requirements

### Requirement: Opens from the mini player and closes back to where the user was
Tapping the mini player SHALL open the Now Playing screen as a full-screen destination above the current screen, sliding up over it. While it is open, neither the bottom navigation bar nor the mini player SHALL be shown. A collapse affordance and the system back action SHALL both return to exactly the screen the user came from, sliding the player down, with the mini player visible again; on devices with predictive back the player SHALL follow the back gesture. If the queue is cleared or ends while the screen is open, the screen SHALL close by itself.

#### Scenario: Open from a tab
- **WHEN** a song is playing and the user taps the mini player on the Playlists tab
- **THEN** the Now Playing screen slides up and is shown with no bottom navigation bar and no mini player

#### Scenario: Collapse
- **WHEN** the user taps the collapse affordance or presses system back on Now Playing
- **THEN** the player slides down and the previous screen is shown with the mini player docked and the bottom bar back where it was

#### Scenario: Queue ends while open
- **WHEN** the last song ends (repeat off) while Now Playing is open
- **THEN** the screen closes and the previous screen is shown without a mini player

#### Scenario: Mid-transition both screens exist
- **WHEN** the open animation is in progress
- **THEN** the previous screen is still composed underneath the incoming player until the animation finishes
