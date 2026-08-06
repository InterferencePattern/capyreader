**Type:** AFK

## What to build

Before committing to a Spoken Article, the user can see roughly how long it is and how much of their provider allowance it will consume.

The Listen control surfaces the character count of the article's Speakable Text and a rough spoken duration. Since Speech Providers bill per character, the character count is the honest number; duration is the number a listener actually cares about. Both are derived from text already in hand, so nothing is requested to produce them.

This matters most on providers with small allowances, where a single long article can be a meaningful fraction of a month's quota. It is deliberately an estimate presented as an estimate — not a price, which Capy cannot know without modelling every provider's pricing tiers.

## Acceptance criteria

- [ ] The Listen control exposes the Speakable Text character count and an estimated duration
- [ ] Both figures are computed locally with no request to the Speech Provider
- [ ] The figures reflect Full Content state — enabling Full Content changes them
- [ ] The estimate is presented as approximate and is not framed as a monetary price
- [ ] The figures match what will actually be synthesized, including exclusion of code blocks, tables, and captions
- [ ] The presentation does not add a step to starting playback

## Blocked by

- [02 — Full-length articles via a Passage playlist](./02-passage-playlist.md)
