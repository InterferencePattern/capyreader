**Type:** HITL

## What to build

The user picks a Voice from a list of real names and hears it before committing, instead of pasting an opaque identifier.

Where a Speech Provider can enumerate its Voices, settings fetch that list and present it for selection. A sample control speaks a short fixed sentence in the highlighted Voice. This is the only practical way to choose among dozens of ElevenLabs Voices, and it turns a copy-paste-from-a-website step into a normal picker.

The sample does not go through the media player. Taking over the media session — and interrupting whatever Enclosure or Spoken Article is playing — to preview a sentence in a settings screen would be wrong; the sample plays on its own simple playback path.

Voice enumeration requires valid credentials, so the picker only becomes useful after a key is entered. The screen needs to behave sensibly before that, while loading, and when the request fails, and must not trap the user: entering a Voice identifier by hand stays available as a fallback.

**HITL:** a new asynchronous settings surface with loading, empty, error, and unauthenticated states. Worth a design review before it ships.

## Acceptance criteria

- [ ] Voices are fetched from the Speech Provider and presented by name where the provider supports enumeration
- [ ] Selecting a Voice persists it and is reflected in subsequent playback
- [ ] A sample control speaks a short sentence in the highlighted Voice
- [ ] Playing a sample does not stop, pause, or interrupt a playing Enclosure or Spoken Article
- [ ] Playing a sample does not post a media notification
- [ ] The screen handles no-credentials, loading, empty-list, and request-failure states distinctly
- [ ] Entering a Voice identifier manually remains possible when enumeration is unavailable or fails
- [ ] Each sample's cost to the user is understood to be non-zero and is not triggered automatically on screen open

## Blocked by

- [04 — ElevenLabs provider and Speech Provider selection](./04-elevenlabs-provider.md)
