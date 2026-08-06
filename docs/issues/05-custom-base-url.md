**Type:** AFK

## What to build

A user can point Capy at any OpenAI-compatible speech endpoint by supplying its address.

This adds a Speech Provider option that speaks the same request shape as OpenAI but sends it wherever the user says. It costs very little beyond a base URL field and buys every self-hosted speech server — which matters more than usual here, because on-device synthesis was ruled out. A self-hosted endpoint is the only way to use this feature without paying per article or sending article text to a company, and it is the escape hatch for any provider Capy does not ship natively.

Credentials are optional for this provider; self-hosted servers commonly require none. The empty-key case must not be treated as "not configured".

Preloading of the next Passage is enabled, since marginal cost against a self-hosted server is approximately zero.

## Acceptance criteria

- [ ] Settings accept a base URL for an OpenAI-compatible Speech Provider
- [ ] Synthesis works against such an endpoint with no API key set
- [ ] Synthesis works against such an endpoint with an API key set
- [ ] A Voice is entered as free text, since a self-hosted server's Voices are unknown in advance
- [ ] An unreachable or malformed base URL produces a clear failure rather than a silent no-op
- [ ] Preloading of the next Passage is enabled for this provider
- [ ] The settings disclosure reflects that text is sent to the user's configured endpoint

## Blocked by

- [04 — ElevenLabs provider and Speech Provider selection](./04-elevenlabs-provider.md)
