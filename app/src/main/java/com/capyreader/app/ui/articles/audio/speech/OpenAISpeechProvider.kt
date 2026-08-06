package com.capyreader.app.ui.articles.audio.speech

import com.capyreader.app.R
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient

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

    // The roster OpenAI documents for MODEL. `tts-1` supports only nine of these, which is not our
    // problem: this provider always sends MODEL.
    private val VOICES = listOf(
        "alloy", "ash", "ballad", "cedar", "coral", "echo", "fable",
        "marin", "nova", "onyx", "sage", "shimmer", "verse",
    )

    override val listsVoices = true

    // Nothing to fetch: OpenAI publishes the list in its documentation and serves it from no
    // endpoint, so this drifts only when they ship a voice. The client is unused, and no key is
    // needed to see the names -- only to hear them.
    override suspend fun voices(settings: SpeechSettings, client: OkHttpClient) =
        VOICES.map { SpeechVoice(id = it, name = it.replaceFirstChar(Char::uppercase)) }

    override fun audioSignature(settings: SpeechSettings) =
        "$MODEL|${settings.voice}|$INSTRUCTIONS"

    override fun request(text: String, settings: SpeechSettings): SpeechPassageRequest {
        val body = buildJsonObject {
            put("model", MODEL)
            put("input", text)
            put("voice", settings.voice)
            put("instructions", INSTRUCTIONS)
        }.toString().toByteArray(Charsets.UTF_8)

        return SpeechPassageRequest(
            url = ENDPOINT,
            headers = mapOf(
                "Authorization" to "Bearer ${settings.apiKey}",
                "Content-Type" to "application/json",
            ),
            body = body,
        )
    }
}
