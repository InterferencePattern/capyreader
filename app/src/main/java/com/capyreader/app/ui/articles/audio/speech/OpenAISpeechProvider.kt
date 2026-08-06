package com.capyreader.app.ui.articles.audio.speech

import com.capyreader.app.R
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object OpenAISpeechProvider : SpeechProvider {
    private const val ENDPOINT = "https://api.openai.com/v1/audio/speech"

    // Steerable model: `tts-1` and `tts-1-hd` ignore `instructions` and infer prosody from the
    // text alone, which is what makes them dramatize the occasional sentence. It bills out at
    // the same rate per character as `tts-1`.
    private const val MODEL = "gpt-4o-mini-tts"

    // ponytail: hardcoded until someone wants to tune it; a settings field would have to join
    // the audio signature below, or editing the tone would replay the old audio.
    private const val INSTRUCTIONS =
        "Read in a calm, even, professional newscaster tone. Do not dramatize or add emotional " +
            "emphasis. Keep pace, pitch, and volume consistent throughout."

    override val id = "openai"

    override val title = R.string.settings_listen_provider_openai

    override val voiceLabel = R.string.settings_listen_voice_label

    override fun audioSignature(voice: String) = "$MODEL|$voice|$INSTRUCTIONS"

    override fun request(text: String, voice: String, apiKey: String): SpeechPassageRequest {
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
