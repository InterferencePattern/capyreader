**Type:** HITL

## What to build

The listener can change playback speed, and the choice is remembered.

Synthetic Voices at normal speed feel slow to habitual listeners, and speed is the single most-requested control in any spoken-word player. Speed is applied locally to audio that has already been synthesized, so changing it is instant, works on cached audio, and costs nothing — unlike Voice, which is a property of the Speech Provider's output and forces re-synthesis.

The control lands on the player shared with podcast playback, so Enclosures gain speed control at the same time. That is scope beyond text-to-speech and is the reason this needs a human decision: it changes an existing, shipped surface.

Speed must not be conflated with Voice in the cache key. Two listens at different speeds are the same synthesized audio and must both hit the cache.

## Acceptance criteria

- [ ] Playback speed can be changed during playback and takes effect immediately
- [ ] The selected speed persists across playback sessions and app restarts
- [ ] Changing speed triggers no request to the Speech Provider
- [ ] Cached audio replays at the current speed without re-synthesis
- [ ] Speed is not part of the cache key
- [ ] The control's placement in the shared player has been reviewed against the existing podcast playback UI
- [ ] Pitch remains natural at the supported speeds

## Blocked by

- [01 — Speak the first passage of an article](./01-first-passage.md)
