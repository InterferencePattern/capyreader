---
status: accepted
---

# Spoken articles play through the podcast media pipeline

Capy already runs a media3 `MediaSessionService` with an ExoPlayer, an OkHttp data source, and a
disk cache, built for podcast enclosures. Rather than stand up a second playback path for
text-to-speech, a spoken article is fed into that same pipeline as a playlist of passages, each
one a `MediaItem` with a synthetic `capytts://` URI that a `ResolvingDataSource` rewrites into a
POST to the user's speech provider. One player, one media session, one notification, one cache.

## Considered Options

**On-device `TextToSpeech`.** Rejected. It is a speaker, not a media source — it cannot be
handed to ExoPlayer, so supporting it means either `synthesizeToFile()` chunking or a second,
degraded playback path with no lock-screen controls. Cost: no offline listening and no free
option, which self-hosted OpenAI-compatible servers are expected to fill instead.

**Synthesize the whole article to one file, then play.** Rejected. It buys a true total duration
and a working seek bar, but costs 15–40 seconds of silence before any audio and charges the user
for an entire article they may abandon after ten seconds.

**Synthesize passage-by-passage to temp files.** Rejected as unnecessary. It is easier to debug,
but it means owning temp-file lifecycle and eviction that the existing `SimpleCache` already
handles.

## Consequences

The player only ever knows the duration of the current passage, so **spoken articles have no seek
bar** — play/pause and ±30s only, and ±30s cannot cross a passage boundary. This is why
`FloatingAudioPlayer` branches on the URI scheme rather than on missing duration metadata.

Passages are fetched a fixed ten seconds before the one before them ends, for every speech
provider. This supersedes the original plan of making preloading a per-provider property, which
assumed some providers were cheap enough that an unheard passage did not matter: measured OpenAI
billing put a passage at roughly five cents, an order of magnitude under ElevenLabs but not
free. A ten-second lead covers the round trip to the provider while exposing only the last
seconds of a passage to being paid for and never heard.

The cache key is a hash of text, provider, voice, and model rather than the article ID, so
changing any input misses the cache instead of serving audio that no longer matches the article.

A spoken article is represented by the existing `AudioEnclosure` type with a synthetic URL. This
is a deliberate shortcut to avoid a sealed-type refactor across the controller, the floating
player, and the article screen. The type name is therefore inaccurate — see `CONTEXT.md`, which
records the disagreement so it is not "fixed" by mistake.
