package com.capyreader.app.ui.articles.audio.speech

import androidx.annotation.StringRes
import com.capyreader.app.R
import com.capyreader.app.common.AudioEnclosure
import com.capyreader.app.preferences.AppPreferences
import com.capyreader.app.ui.articles.audio.AudioPlayerController
import com.jocmp.capy.Article
import com.jocmp.capy.articles.passages
import com.jocmp.capy.articles.speakableText

/**
 * Turns the article the reader is currently showing into a Spoken Article and hands it to the
 * existing [AudioPlayerController] -- the same controller Audio Enclosures play through, so
 * starting one stops the other for free (there is only ever one current playlist item).
 */
class SpokenArticlePlayer(
    private val appPreferences: AppPreferences,
    private val audioController: AudioPlayerController,
) {
    /**
     * Returns null once playback has started, or the message explaining why nothing will play.
     * Neither refusal reaches the Speech Provider, so neither shows up as a playback error.
     */
    @StringRes
    fun play(article: Article): Int? {
        val provider = SpeechProvider.from(appPreferences.speechOptions.provider.get())
        val settings = SpeechSettings(
            voice = appPreferences.speechOptions.getVoice(provider).get(),
            apiKey = appPreferences.speechOptions.getApiKey(provider).get(),
            baseUrl = appPreferences.speechOptions.getBaseUrl(provider).get(),
        )

        // Inert until configured: no request is built, so no article text leaves the device.
        provider.configurationError(settings)?.let { return it }

        val passages = passages(speakableText(article.content))

        if (passages.isEmpty()) {
            return R.string.listen_error_no_text
        }

        val uris = provider.registerPassages(
            passages = passages,
            settings = settings,
        )

        audioController.play(
            AudioEnclosure(
                url = uris.first(),
                title = article.title,
                feedName = article.feedName,
                durationSeconds = null,
                artworkUrl = null,
            ),
            queuedUrls = uris.drop(1),
        )

        return null
    }
}
