package com.capyreader.app.ui.articles.audio.speech

import com.capyreader.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

object ElevenLabsSpeechProvider : SpeechProvider {
    private const val API = "https://api.elevenlabs.io/v1"
    private const val ENDPOINT = "$API/text-to-speech"
    private const val MODEL = "eleven_multilingual_v2"

    override val id = "elevenlabs"

    override val title = R.string.settings_listen_provider_elevenlabs

    // A Voice here is an opaque identifier, so the picker is the only humane way in. The field
    // stays for the reader who already has an identifier, or whose list will not load.
    override val voiceLabel = R.string.settings_listen_voice_id_label

    override val listsVoices = true

    /**
     * The legacy `GET /v1/voices` rather than `/v2/voices`: one request, no paging, and a flat
     * response with everything a picker needs. v2 exists for workspaces past 500 voices, which a
     * reader choosing a narrator is not. Requires the key -- the list is per account.
     */
    override suspend fun voices(settings: SpeechSettings, client: OkHttpClient): List<SpeechVoice> {
        val request = Request.Builder()
            .url("$API/voices")
            .header("xi-api-key", settings.apiKey)
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Voice list failed: ${response.code}")
                }

                parseVoices(response.body.string())
            }
        }
    }

    /**
     * Each voice also carries a `preview_url` -- free public audio -- which is deliberately
     * ignored: every voice previews with its own stock words, so two voices would be compared on
     * the sentence rather than on the speaker. A Sample is synthesized here like anywhere else.
     */
    internal fun parseVoices(body: String): List<SpeechVoice> {
        val voices = Json.parseToJsonElement(body).jsonObject["voices"]?.jsonArray.orEmpty()

        return voices.mapNotNull { element ->
            val voice = element.jsonObject
            val voiceID = voice["voice_id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null

            SpeechVoice(
                id = voiceID,
                name = voice["name"]?.jsonPrimitive?.contentOrNull.orEmpty().ifBlank { voiceID },
            )
        }
    }

    // No equivalent of OpenAI's `instructions`: tone on this model comes from the Voice itself,
    // so the newscaster steering has nothing to hash here.
    override fun audioSignature(settings: SpeechSettings) = "$MODEL|${settings.voice}"

    override fun request(text: String, settings: SpeechSettings): SpeechPassageRequest {
        val body = buildJsonObject {
            put("text", text)
            put("model_id", MODEL)
        }.toString().toByteArray(Charsets.UTF_8)

        return SpeechPassageRequest(
            url = "$ENDPOINT/${settings.voice}",
            headers = mapOf(
                "xi-api-key" to settings.apiKey,
                "Content-Type" to "application/json",
                "Accept" to "audio/mpeg",
            ),
            body = body,
        )
    }
}
