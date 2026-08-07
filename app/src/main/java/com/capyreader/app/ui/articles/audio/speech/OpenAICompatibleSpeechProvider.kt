package com.capyreader.app.ui.articles.audio.speech

import com.capyreader.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

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

    override val listsVoices = true

    // Not the API key, which is optional here: the address is the only thing there is to ask.
    override fun canListVoices(settings: SpeechSettings) =
        settings.baseUrl.toHttpUrlOrNull() != null

    override val voicesRequirementLabel = R.string.settings_listen_voices_need_url

    override val voicesEmptyLabel = R.string.settings_listen_voices_unlisted

    /**
     * No listing route is part of the OpenAI shape, and the servers that added one disagree:
     * Kokoro-FastAPI answers `/audio/voices`, openai-edge-tts answers `/voices`, and
     * openedai-speech answers neither, reading its Voices from a file on the server. So ask both
     * and take the first that comes back with something recognizable.
     */
    private val VOICE_ROUTES = listOf("audio/voices", "voices")

    /**
     * Never throws. A 404, a page of HTML from a reverse proxy, a shape nobody recognizes, and an
     * address that does not answer are all the same thing to the reader: this server does not list
     * its Voices, which is the normal case for a self-hosted one rather than a failure to report.
     * A wrong address is found out on the first Listen, where it was always going to be found out.
     */
    override suspend fun voices(settings: SpeechSettings, client: OkHttpClient) =
        withContext(Dispatchers.IO) {
            VOICE_ROUTES.firstNotNullOfOrNull { route ->
                fetchVoices(route, settings, client).ifEmpty { null }
            }.orEmpty()
        }

    private fun fetchVoices(
        route: String,
        settings: SpeechSettings,
        client: OkHttpClient,
    ): List<SpeechVoice> {
        val request = Request.Builder()
            .url("${settings.baseUrl.trimEnd('/')}/$route")
            .apply {
                if (settings.apiKey.isNotBlank()) {
                    header("Authorization", "Bearer ${settings.apiKey}")
                }
            }
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) parseVoices(response.body.string()) else emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Tolerant by design. The array is the response itself or sits under `voices` or `data`; an
     * entry is a bare identifier or an object carrying one alongside a display name. Anything else
     * is no voices, which the caller reads as "this server does not list them".
     */
    internal fun parseVoices(body: String): List<SpeechVoice> {
        val root = runCatching { Json.parseToJsonElement(body) }.getOrNull()

        val array = root as? JsonArray
            ?: (root as? JsonObject)?.let { it["voices"] ?: it["data"] } as? JsonArray
            ?: return emptyList()

        return array.mapNotNull(::parseVoice)
    }

    private fun parseVoice(element: JsonElement): SpeechVoice? {
        (element as? JsonPrimitive)?.let { primitive ->
            val name = primitive.contentOrNull.takeIf { primitive.isString && !it.isNullOrBlank() }

            return name?.let { SpeechVoice(id = it, name = it) }
        }

        val voice = element as? JsonObject ?: return null
        val name = voice.string("name")
        val id = voice.string("id") ?: voice.string("voice_id") ?: name ?: return null

        return SpeechVoice(id = id, name = name ?: id)
    }

    private fun JsonObject.string(key: String) =
        (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

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
