package com.capyreader.app.ui.articles.audio.speech

import com.capyreader.app.common.AudioEnclosure
import com.capyreader.app.preferences.AppPreferences
import com.capyreader.app.ui.articles.audio.AudioPlayerController
import com.jocmp.capy.Article
import com.jocmp.capy.articles.speakableText

// ponytail: first Passage only (~4,000 chars); slice 02 splits the rest into a playlist.
private const val FIRST_PASSAGE_MAX_LENGTH = 4_000

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

        val text = speakableText(article.content).take(FIRST_PASSAGE_MAX_LENGTH)

        if (text.isBlank()) {
            return
        }

        val uri = OpenAISpeechProvider.registerPassage(
            text = text,
            voice = voice,
            apiKey = apiKey,
        )

        audioController.play(
            AudioEnclosure(
                url = uri,
                title = article.title,
                feedName = article.feedName,
                durationSeconds = null,
                artworkUrl = null,
            )
        )
    }
}
