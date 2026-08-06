package com.capyreader.app.ui.articles.audio.speech

import com.capyreader.app.R
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Any server that speaks OpenAI's `/audio/speech` shape at an address the reader supplies --
 * a self-hosted synthesizer, or a provider Capy does not ship natively. The escape hatch for
 * listening without paying per article or sending article text to a company.
 */
object OpenAICompatibleSpeechProvider : SpeechProvider {
    // ponytail: `tts-1` is what compatible servers accept when they check the field at all, and
    // most ignore it. A settings field for it would have to join the audio signature below.
    private const val MODEL = "tts-1"

    override val id = "openai_compatible"

    override val title = R.string.settings_listen_provider_openai_compatible

    // A self-hosted server's Voices are unknown in advance, so this is free text like OpenAI's.
    override val voiceLabel = R.string.settings_listen_voice_label

    override val apiKeyLabel = R.string.settings_listen_api_key_optional_label

    override val usesBaseUrl = true

    // The endpoint is part of what determines the audio: the same voice name on two servers is
    // two different voices, and cached Passages must not survive a move between them.
    override fun audioSignature(settings: SpeechSettings) =
        "${settings.baseUrl}|$MODEL|${settings.voice}"

    // No `instructions`: the newscaster steering is specific to OpenAI's steerable model, and a
    // server that rejects unknown fields would fail every request over it.
    override fun request(text: String, settings: SpeechSettings): SpeechPassageRequest {
        val body = buildJsonObject {
            put("model", MODEL)
            put("input", text)
            put("voice", settings.voice)
        }.toString().toByteArray(Charsets.UTF_8)

        return SpeechPassageRequest(
            url = "${settings.baseUrl.trimEnd('/')}/audio/speech",
            // Self-hosted servers commonly want no credentials at all, and some reject an empty
            // bearer token rather than ignoring it.
            headers = buildMap {
                put("Content-Type", "application/json")
                if (settings.apiKey.isNotBlank()) {
                    put("Authorization", "Bearer ${settings.apiKey}")
                }
            },
            body = body,
        )
    }

    /** An API key is optional here, so a blank one is configured, not missing. */
    override fun configurationError(settings: SpeechSettings) = when {
        settings.voice.isBlank() -> R.string.listen_error_missing_voice
        // Catches a blank or unparseable address before the player turns it into a generic
        // playback failure seconds later. An address that parses but does not answer stays a
        // playback error, which is the only place it can be found out.
        settings.baseUrl.toHttpUrlOrNull() == null -> R.string.listen_error_invalid_base_url
        else -> null
    }
}
