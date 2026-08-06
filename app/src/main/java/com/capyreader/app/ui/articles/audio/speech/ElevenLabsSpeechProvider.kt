package com.capyreader.app.ui.articles.audio.speech

import com.capyreader.app.R
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object ElevenLabsSpeechProvider : SpeechProvider {
    private const val ENDPOINT = "https://api.elevenlabs.io/v1/text-to-speech"
    private const val MODEL = "eleven_multilingual_v2"

    override val id = "elevenlabs"

    override val title = R.string.settings_listen_provider_elevenlabs

    // A Voice here is an opaque identifier copied out of the ElevenLabs voice library by hand.
    // Slice 06 replaces the typing with a picker.
    override val voiceLabel = R.string.settings_listen_voice_id_label

    // No equivalent of OpenAI's `instructions`: tone on this model comes from the Voice itself,
    // so the newscaster steering has nothing to hash here.
    override fun audioSignature(voice: String) = "$MODEL|$voice"

    override fun request(text: String, voice: String, apiKey: String): SpeechPassageRequest {
        val body = buildJsonObject {
            put("text", text)
            put("model_id", MODEL)
        }.toString().toByteArray(Charsets.UTF_8)

        return SpeechPassageRequest(
            url = "$ENDPOINT/$voice",
            headers = mapOf(
                "xi-api-key" to apiKey,
                "Content-Type" to "application/json",
                "Accept" to "audio/mpeg",
            ),
            body = body,
        )
    }
}
