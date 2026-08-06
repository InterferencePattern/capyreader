package com.capyreader.app.ui.articles.audio.speech

import androidx.annotation.StringRes
import com.capyreader.app.common.MD5
import com.capyreader.app.common.SPOKEN_ARTICLE_SCHEME

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

    /** Everything besides the text that determines the audio: model, voice, tone. */
    fun audioSignature(voice: String): String

    fun request(text: String, voice: String, apiKey: String): SpeechPassageRequest

    companion object {
        val all = listOf(OpenAISpeechProvider, ElevenLabsSpeechProvider)

        val default = OpenAISpeechProvider

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
    voice: String,
    apiKey: String,
): List<String> {
    val uris = passages.map { text ->
        "$SPOKEN_ARTICLE_SCHEME://${MD5.from("$id|${audioSignature(voice)}|$text")}"
    }

    SpeechPassageRegistry.register(
        uris.zip(passages) { uri, text -> uri to request(text, voice, apiKey) }.toMap()
    )

    return uris
}
