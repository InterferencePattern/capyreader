package com.capyreader.app.ui.articles.audio.speech

import androidx.annotation.StringRes
import com.capyreader.app.R
import com.capyreader.app.common.MD5
import com.capyreader.app.common.SPOKEN_ARTICLE_SCHEME

/**
 * Everything the reader configures about a Speech Provider. Providers use the parts they have a
 * use for: [baseUrl] means nothing to a provider whose endpoint is fixed, and [apiKey] means
 * nothing to a self-hosted server that asks for no credentials.
 */
data class SpeechSettings(
    val voice: String,
    val apiKey: String,
    val baseUrl: String = "",
)

/**
 * The third-party service that turns Speakable Text into audio. Each one owns its endpoint, its
 * authentication scheme, and what a Voice identifier looks like.
 *
 * No provider preloads: ADR-0001 expected OpenAI to be cheap enough that an unheard Passage did
 * not matter, and measured billing put a Passage at roughly five cents. Every provider therefore
 * fetches a Passage only once playback reaches it, and pays for silence at Passage boundaries.
 */
interface SpeechProvider {
    /**
     * Stored in preferences, namespaces the credentials, and is hashed into every Passage URI so
     * audio synthesized by one provider is never replayed under another.
     */
    val id: String

    @get:StringRes
    val title: Int

    /** Providers disagree on whether a Voice has a name or an opaque identifier. */
    @get:StringRes
    val voiceLabel: Int

    @get:StringRes
    val apiKeyLabel: Int
        get() = R.string.settings_listen_api_key_label

    /** Whether the reader supplies the endpoint. Only true for a provider Capy cannot name. */
    val usesBaseUrl: Boolean
        get() = false

    /** Everything besides the text that determines the audio: model, voice, tone. */
    fun audioSignature(settings: SpeechSettings): String

    fun request(text: String, settings: SpeechSettings): SpeechPassageRequest

    /**
     * Null when the provider can be asked to speak, or the message explaining what is missing.
     * Checked before any request is built, so a misconfigured provider never sees article text.
     */
    @StringRes
    fun configurationError(settings: SpeechSettings): Int? = when {
        settings.apiKey.isBlank() -> R.string.listen_error_missing_credentials
        settings.voice.isBlank() -> R.string.listen_error_missing_voice
        else -> null
    }

    companion object {
        // Built on each read rather than held: this interface has default methods, so loading any
        // provider loads the companion first, and a stored list would capture the provider that
        // is still mid-initialization as null.
        val all: List<SpeechProvider>
            get() = listOf(
                OpenAISpeechProvider,
                ElevenLabsSpeechProvider,
                OpenAICompatibleSpeechProvider,
            )

        val default: SpeechProvider
            get() = OpenAISpeechProvider

        /** Falls back to [default] so an unknown stored id can never leave Listen unusable. */
        fun from(id: String) = all.find { it.id == id } ?: default
    }
}

/**
 * Registers the POST that fetches each Passage and returns the synthetic URIs to play, in order.
 * A URI hashes everything that determines the audio -- provider, model, voice, tone, and text --
 * so identical input hits the existing cache instead of paying again, and any change to the
 * input misses it. See ADR-0001.
 */
fun SpeechProvider.registerPassages(
    passages: List<String>,
    settings: SpeechSettings,
): List<String> {
    val uris = passages.map { text ->
        "$SPOKEN_ARTICLE_SCHEME://${MD5.from("$id|${audioSignature(settings)}|$text")}"
    }

    SpeechPassageRegistry.register(
        uris.zip(passages) { uri, text -> uri to request(text, settings) }.toMap()
    )

    return uris
}
