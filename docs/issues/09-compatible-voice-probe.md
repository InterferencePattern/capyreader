**Type:** AFK

## What to build

A self-hosted OpenAI-compatible endpoint that can list its Voices gets the same picker the named providers have, instead of a text field.

Slice 06 gave OpenAI and ElevenLabs a Voice list and left the OpenAI-compatible provider typing identifiers, because no listing route is part of the OpenAI shape and the servers that added one disagree about it. Kokoro-FastAPI answers `GET /v1/audio/voices` with a bare array of strings; openai-edge-tts answers `GET /v1/voices`; openedai-speech has no route at all and reads its Voices from a file on the server. Guessing wrong costs a request and a confusing error, so slice 06 guessed nothing.

The work is a probe that tolerates all of it: ask the one or two routes that exist, accept either bare strings or objects carrying an id and a name, and fall back to the text field without complaint when the server answers with a 404, HTML, or a shape nobody recognizes. A self-hosted server that cannot list its Voices is the normal case, not an error to report.

Sampling needs nothing new: a Sample is a synthesis request everywhere, and against a self-hosted endpoint its marginal cost is approximately zero, which is part of why this is worth doing at all.

## Acceptance criteria

- [ ] Voices are listed for an OpenAI-compatible endpoint that answers a known listing route
- [ ] Both the bare-string and the id-and-name object response shapes are accepted
- [ ] A server with no listing route falls back to the free-text Voice field silently, with no error surfaced
- [ ] A malformed or non-JSON response falls back the same way rather than crashing or reporting a failure
- [ ] The probe issues no request until the reader asks for the list
- [ ] Entering a Voice identifier manually remains possible regardless of what the probe finds

## Blocked by

- [06 — Voice list and sample playback](./06-voice-picker.md)
