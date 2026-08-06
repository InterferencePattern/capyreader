package com.capyreader.app.ui.articles.audio.speech

import com.capyreader.app.common.MD5
import com.capyreader.app.common.SPOKEN_ARTICLE_SCHEME
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// ponytail: one hardcoded provider (OpenAI); slice 04 introduces the Speech Provider seam.
object OpenAISpeechProvider {
    private const val ENDPOINT = "https://api.openai.com/v1/audio/speech"
    private const val MODEL = "tts-1"

    /**
     * Registers the POST that fetches this Passage and returns the synthetic URI to play. The
     * URI hashes provider, model, voice, and text, so identical input hits the existing cache
     * instead of paying again, and any change to the input misses it. See ADR-0001.
     */
    fun registerPassage(text: String, voice: String, apiKey: String): String {
        val uri = "$SPOKEN_ARTICLE_SCHEME://${MD5.from("openai|$MODEL|$voice|$text")}"

        val body = buildJsonObject {
            put("model", MODEL)
            put("input", text)
            put("voice", voice)
        }.toString().toByteArray(Charsets.UTF_8)

        SpeechPassageRegistry.register(
            uri = uri,
            request = SpeechPassageRequest(
                url = ENDPOINT,
                headers = mapOf(
                    "Authorization" to "Bearer $apiKey",
                    "Content-Type" to "application/json",
                ),
                body = body,
            )
        )

        return uri
    }
}
