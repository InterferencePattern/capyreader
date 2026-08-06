package com.capyreader.app.ui.articles.audio.speech

/**
 * The resolved HTTP request for a single Passage: where to send it, and what to send.
 */
class SpeechPassageRequest(
    val url: String,
    val headers: Map<String, String>,
    val body: ByteArray,
)

/**
 * Holds the request for the synthetic `capytts://` URI the player is about to fetch, so
 * [SpeechDataSourceResolver] can turn a playlist item into a real POST. In-memory only:
 * `MediaPlaybackService` runs in the same process as the rest of the app, so this is shared
 * without needing IPC.
 */
// ponytail: one Passage in flight at a time; slice 02's playlist turns this back into a map.
object SpeechPassageRegistry {
    @Volatile
    private var pending: Pair<String, SpeechPassageRequest>? = null

    fun register(uri: String, request: SpeechPassageRequest) {
        pending = uri to request
    }

    fun resolve(uri: String): SpeechPassageRequest? =
        pending?.takeIf { it.first == uri }?.second
}
