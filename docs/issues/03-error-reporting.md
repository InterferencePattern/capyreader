**Type:** AFK

## What to build

When synthesis fails, the user is told why and can act on it.

Failures reach the app as HTTP status codes surfaced through playback errors. Two dominate in practice and both are actionable: an invalid or missing key, and an exhausted provider quota. Each gets its own message — the first offering a route to the text-to-speech settings — and everything else falls back to a generic failure message.

Failure can happen on the very first Passage, where nothing has played yet, or partway through, where earlier Passages played fine. In both cases playback stops and the floating player stays on screen so retrying is a single tap. Retrying replays already-cached Passages without re-purchasing them.

Errors are not retried automatically. A retry against an exhausted quota is another paid request that will fail again, and it delays an honest error message by several seconds.

The Speech Provider's own error body is not surfaced. It is not readable from a playback error without wrapping the data source, and it would mean rendering untrusted vendor strings directly in the UI.

## Acceptance criteria

- [ ] An unauthorized response produces a message about credentials, with a route to text-to-speech settings
- [ ] A rate-limited or quota-exhausted response produces a message naming quota as the cause
- [ ] Other failures produce a generic message rather than silence
- [ ] A failure on the first Passage is reported; the user is never left with a Listen tap that appears to do nothing
- [ ] A failure partway through stops playback and leaves the floating player visible
- [ ] Retrying after a mid-article failure replays cached Passages without new requests
- [ ] No automatic retry is issued

## Blocked by

- [01 — Speak the first passage of an article](./01-first-passage.md)
