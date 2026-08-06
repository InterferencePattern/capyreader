# Text-to-speech work items

Vertical slices for spoken articles. Each is a complete path through settings, text extraction,
the Speech Provider, and playback — not a layer. Vocabulary follows [CONTEXT.md](../../CONTEXT.md);
the playback architecture is fixed by [ADR-0001](../adr/0001-speech-through-the-media-pipeline.md).

| # | Slice | Type | Blocked by |
|---|-------|------|-----------|
| [01](./01-first-passage.md) | Speak the first passage of an article | AFK | — |
| [02](./02-passage-playlist.md) | Full-length articles via a Passage playlist | AFK | 01 |
| [03](./03-error-reporting.md) | Synthesis error reporting | AFK | 01 |
| [04](./04-elevenlabs-provider.md) | ElevenLabs provider and provider selection | AFK | 02 |
| [05](./05-custom-base-url.md) | OpenAI-compatible custom base URL | AFK | 04 |
| [06](./06-voice-picker.md) | Voice list and sample playback | HITL | 04 |
| [07](./07-playback-speed.md) | Playback speed control | HITL | 01 |
| [08](./08-length-estimate.md) | Length and cost estimate | AFK | 02 |
| [09](./09-compatible-voice-probe.md) | Voice listing for OpenAI-compatible servers | AFK | 06 |

Slice 01 unblocks everything. 03 and 07 can run in parallel with 02.
