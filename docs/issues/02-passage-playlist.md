**Type:** AFK

## What to build

A Spoken Article of any length plays end to end, and audio already paid for is never bought twice.

Speakable Text is split into Passages at block boundaries — never mid-sentence — each small enough for the Speech Provider to accept in one request. Passages are queued as a playlist and play back to back, with each one fetched as it starts rather than ahead of time, so abandoning an article halfway does not pay for the remainder.

Synthesized audio is cached so that pausing and resuming, or replaying an article later, costs nothing. The cache key is a hash of the Speakable Text together with the Speech Provider, Voice, and model — not the article identifier. Keying on the article would serve stale audio after the user enables Full Content, and Passage boundaries shift when the text changes, so cached Passage *n* of a teaser has no relationship to Passage *n* of the full article. Note that a `ResolvingDataSource` rewrites the URI before the cache sees it, so the cache key has to be pinned to the original synthetic URI explicitly; getting this wrong either caches nothing or caches everything under one key.

Skipping forward or back cannot cross a Passage boundary — the player only knows the duration of the Passage it is playing. This is a known and accepted limitation (ADR-0001); at roughly four minutes per Passage it is rarely reached.

## Acceptance criteria

- [ ] An article longer than one Passage plays to completion without user intervention
- [ ] Passages split at block boundaries, never mid-sentence
- [ ] A Passage is requested when playback reaches it, not before
- [ ] Abandoning playback partway issues no further requests to the Speech Provider
- [ ] Replaying a previously heard article makes no network requests
- [ ] Pausing and resuming later does not re-request the current Passage
- [ ] Enabling Full Content and replaying produces new audio rather than the cached teaser
- [ ] Changing Voice or Speech Provider produces new audio; changing back replays from cache
- [ ] Skip controls clamp at the current Passage's bounds rather than misbehaving

## Blocked by

- [01 — Speak the first passage of an article](./01-first-passage.md)
