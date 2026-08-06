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
 * Holds the requests for the synthetic `capytts://` URIs the player may fetch, so
 * [SpeechDataSourceResolver] can turn a playlist item into a real POST. In-memory only:
 * `MediaPlaybackService` runs in the same process as the rest of the app, so this is shared
 * without needing IPC.
 *
 * Registering a Spoken Article costs nothing and sends nothing -- only the Passages playback
 * actually reaches are ever requested.
 */
object SpeechPassageRegistry {
    @Volatile
    private var pending: Map<String, SpeechPassageRequest> = emptyMap()

    /** Replaces every pending request: only one Spoken Article is ever queued at a time. */
    fun register(requests: Map<String, SpeechPassageRequest>) {
        pending = requests
    }

    fun resolve(uri: String): SpeechPassageRequest? = pending[uri]
}
