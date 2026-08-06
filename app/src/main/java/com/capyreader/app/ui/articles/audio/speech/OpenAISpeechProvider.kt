package com.capyreader.app.ui.articles.audio.speech

import com.capyreader.app.common.MD5
import com.capyreader.app.common.SPOKEN_ARTICLE_SCHEME
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// ponytail: one hardcoded provider (OpenAI); slice 04 introduces the Speech Provider seam.
object OpenAISpeechProvider {
    private const val ENDPOINT = "https://api.openai.com/v1/audio/speech"
    // Steerable model: `tts-1` and `tts-1-hd` ignore `instructions` and infer prosody from the
    // text alone, which is what makes them dramatize the occasional sentence.
    private const val MODEL = "gpt-4o-mini-tts"

    // ponytail: hardcoded until someone wants to tune it; a settings field would have to join
    // the cache hash below, or editing the tone would replay the old audio.
    private const val INSTRUCTIONS =
        "Read in a calm, even, professional newscaster tone. Do not dramatize or add emotional " +
            "emphasis. Keep pace, pitch, and volume consistent throughout."

    /**
     * Registers the POST that fetches each Passage and returns the synthetic URIs to play, in
     * order. A URI hashes everything that determines the audio -- provider, model, voice,
     * instructions, and text -- so identical input hits the existing cache instead of paying
     * again, and any change to the input misses it. See ADR-0001.
     */
    fun registerPassages(passages: List<String>, voice: String, apiKey: String): List<String> {
        val uris = passages.map { text ->
            "$SPOKEN_ARTICLE_SCHEME://${MD5.from("openai|$MODEL|$voice|$INSTRUCTIONS|$text")}"
        }

        SpeechPassageRegistry.register(
            uris.zip(passages) { uri, text -> uri to request(text, voice, apiKey) }.toMap()
        )

        return uris
    }

    private fun request(text: String, voice: String, apiKey: String): SpeechPassageRequest {
        val body = buildJsonObject {
            put("model", MODEL)
            put("input", text)
            put("voice", voice)
            put("instructions", INSTRUCTIONS)
        }.toString().toByteArray(Charsets.UTF_8)

        return SpeechPassageRequest(
            url = ENDPOINT,
            headers = mapOf(
                "Authorization" to "Bearer $apiKey",
                "Content-Type" to "application/json",
            ),
            body = body,
        )
    }
}
