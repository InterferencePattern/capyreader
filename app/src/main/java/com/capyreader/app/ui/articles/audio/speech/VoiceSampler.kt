package com.capyreader.app.ui.articles.audio.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.annotation.StringRes
import com.capyreader.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException

/**
 * Plays a sample of one Voice on its own playback path: a bare [MediaPlayer] with no MediaSession,
 * no notification, and no audio focus request, so an Enclosure or Spoken Article already playing
 * carries on untouched. ADR-0001 routes articles through the media pipeline; taking that pipeline
 * over to preview a sentence in settings would stop whatever the reader was listening to.
 *
 * Never plays on its own. Every Sample is a synthesis request the reader pays for, and nothing
 * they pay for should depend on them opening a screen.
 */
class VoiceSampler(
    private val context: Context,
    private val client: OkHttpClient,
) {
    private var player: MediaPlayer? = null

    /** Returns null once the sample is playing, or the message explaining why it will not. */
    @StringRes
    suspend fun play(
        voice: SpeechVoice,
        provider: SpeechProvider,
        settings: SpeechSettings,
    ): Int? = withContext(Dispatchers.IO) {
        stop()

        try {
            start(synthesize(voice, provider, settings))
            null
        } catch (e: IOException) {
            R.string.settings_listen_sample_failed
        } catch (e: IllegalStateException) {
            // A source MediaPlayer cannot decode lands here rather than as an IOException.
            R.string.settings_listen_sample_failed
        }
    }

    /** Safe to call on a sample that already finished, and cheap enough to call unconditionally. */
    fun stop() {
        player?.release()
        player = null
    }

    private fun start(source: String) {
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            setDataSource(source)
            setOnCompletionListener { it.release() }
            prepare()
            start()
        }
    }

    /**
     * Pays for the same sentence in this Voice, through the same request a Passage uses. Written
     * to disk because [MediaPlayer] plays a URL or a file, and a POST response body is neither.
     */
    private fun synthesize(
        voice: SpeechVoice,
        provider: SpeechProvider,
        settings: SpeechSettings,
    ): String {
        val speech = provider.request(SAMPLE_TEXT, settings.copy(voice = voice.id))

        val request = Request.Builder()
            .url(speech.url)
            .post(speech.body.toRequestBody())
            .apply {
                speech.headers.forEach { (name, value) -> header(name, value) }
            }
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Sample request failed: ${response.code}")
            }

            // ponytail: one reused file, so samples cannot pile up in the cache. Sampling two
            // voices at once is not a thing a person does with one pair of ears.
            val file = File(context.cacheDir, "voice-sample")

            response.body.byteStream().use { input ->
                file.outputStream().use(input::copyTo)
            }

            return file.path
        }
    }

    companion object {
        /** Fixed, so two Voices are compared on the voice rather than on the words. */
        private const val SAMPLE_TEXT =
            "Capy Reader will read your articles aloud in this voice."
    }
}
