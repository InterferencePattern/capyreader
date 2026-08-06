**Type:** AFK

## What to build

The user chooses which Speech Provider to use, and ElevenLabs joins OpenAI as an option.

Speech Provider becomes a selectable choice in settings, with each provider owning its credential field, request shape, and Voice identifier. ElevenLabs authenticates with its own header scheme rather than a bearer token and identifies a Voice by an opaque identifier the user supplies by hand; a friendlier picker is a later slice.

Whether Passages are fetched ahead of playback becomes a property of the Speech Provider rather than a global setting. Where an unheard Passage costs approximately nothing, the next Passage is preloaded so playback is gapless. Where it costs real money — ElevenLabs, whose free allowance is roughly three long articles a month — Passages stay lazy and a short silence at Passage boundaries is accepted. This trade was made deliberately: smooth playback is not worth silently charging users for audio they never hear.

Switching Speech Provider changes the cache key, so audio synthesized by one provider is never replayed under another.

## Acceptance criteria

- [ ] Settings offer a choice of Speech Provider, with fields appropriate to the selected one
- [ ] Credentials are stored per provider, so switching back does not require re-entering them
- [ ] ElevenLabs synthesis works end to end with a hand-entered Voice identifier
- [ ] Preloading of the next Passage is enabled for OpenAI and disabled for ElevenLabs
- [ ] With preloading disabled, no request for the next Passage is issued before playback reaches it
- [ ] Switching Speech Provider causes subsequent playback to re-synthesize rather than replay cached audio
- [ ] The settings disclosure names the currently selected Speech Provider
- [ ] Error messages remain correct for both providers' status codes

## Blocked by

- [02 — Full-length articles via a Passage playlist](./02-passage-playlist.md)
