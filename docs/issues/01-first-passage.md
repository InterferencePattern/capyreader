**Type:** AFK

## What to build

Tapping a Listen control in the article bottom bar synthesizes the beginning of the current article and plays it through the existing media player.

Introduces the whole vertical path in its thinnest form: a settings screen holding an API key and a Voice name for a single hardcoded Speech Provider (OpenAI); extraction of Speakable Text from the article the reader is currently showing; a request to the Speech Provider; and playback through the existing `MediaSessionService`, floating player, and media notification.

Speakable Text is produced by walking the article's markup into blocks — headings, paragraphs, list items — inserting pauses between them and skipping content that cannot be listened to (code blocks, tables, image captions). It is derived from whatever the reader currently displays, so an article without Full Content yields only its teaser. No fetching happens on the user's behalf.

This slice deliberately speaks only the first Passage — roughly the first 4,000 characters. Splitting a long article across multiple Passages is the next slice.

Per ADR-0001, a Spoken Article enters the player as a playlist item whose URI is synthetic (`capytts://…`) and is rewritten by a `ResolvingDataSource` into a POST carrying the request body and credentials. It reuses the existing player, session, notification, and cache rather than introducing a second playback path. A Spoken Article is represented using the existing `AudioEnclosure` type with a synthetic URL — an intentional shortcut recorded in `CONTEXT.md`; the type name is inaccurate and should not be "corrected".

Spoken articles have no seek bar. The floating player must decide this from the URI scheme, not from missing duration metadata — a podcast enclosure lacking duration metadata is still scrubbable.

## Acceptance criteria

- [ ] A settings screen accepts an API key and a Voice, persisted alongside existing app preferences
- [ ] The settings screen carries a disclosure stating that article text is sent to the configured Speech Provider
- [ ] The feature is inert until credentials are entered; no article text leaves the device by default
- [ ] A Listen control appears in the article bottom bar
- [ ] Speakable Text excludes code blocks, tables, and image captions, and preserves block boundaries as pauses
- [ ] Speakable Text reflects what the reader currently shows; enabling Full Content changes what is spoken
- [ ] Playback starts without waiting for the whole article, and appears in the media notification and lock screen
- [ ] Playback continues when navigating away from the article, with the floating player following
- [ ] Starting a Spoken Article stops any playing Enclosure, and vice versa
- [ ] No seek bar is shown for a Spoken Article; play/pause and skip controls remain
- [ ] An Audio Enclosure with no duration metadata still shows a seek bar

## Blocked by

None - can start immediately
