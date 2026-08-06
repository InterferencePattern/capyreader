package com.capyreader.app.ui.articles.audio

import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import com.capyreader.app.R

/**
 * Why playback stopped, in terms the reader can act on.
 *
 * A Speech Provider's own error body is deliberately not surfaced: it is not readable from a
 * [PlaybackException] without wrapping the data source, and it would put untrusted vendor strings
 * on screen.
 */
enum class PlaybackError(@StringRes val message: Int) {
    Credentials(R.string.audio_player_error_credentials),
    Quota(R.string.audio_player_error_quota),
    Unknown(R.string.audio_player_error_unknown);

    companion object {
        fun from(exception: PlaybackException) = forStatus(exception.responseCode)

        internal fun forStatus(responseCode: Int?) = when (responseCode) {
            401, 403 -> Credentials
            // 429 covers both a rate limit and OpenAI's exhausted quota; 402 is what some
            // OpenAI-compatible providers send instead.
            402, 429 -> Quota
            else -> Unknown
        }
    }
}

/** media3 buries the status code in the cause chain. */
@OptIn(UnstableApi::class)
private val PlaybackException.responseCode: Int?
    get() = generateSequence(cause) { it.cause }
        .filterIsInstance<HttpDataSource.InvalidResponseCodeException>()
        .firstOrNull()
        ?.responseCode
