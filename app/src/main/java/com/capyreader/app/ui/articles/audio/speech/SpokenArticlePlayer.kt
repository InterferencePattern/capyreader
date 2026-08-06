package com.capyreader.app.ui.articles.audio.speech

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
    fun play(article: Article) {
        val apiKey = appPreferences.speechOptions.apiKey.get()
        val voice = appPreferences.speechOptions.voice.get()

        // Inert without credentials: no request is built, so no article text leaves the device.
        if (apiKey.isBlank() || voice.isBlank()) {
            return
        }

        val passages = passages(speakableText(article.content))

        if (passages.isEmpty()) {
            return
        }

        val uris = OpenAISpeechProvider.registerPassages(
            passages = passages,
            voice = voice,
            apiKey = apiKey,
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
    }
}
