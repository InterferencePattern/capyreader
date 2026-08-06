package com.capyreader.app.ui.articles.audio.speech

import com.capyreader.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechProviderTest {
    @Test
    fun registerPassages_differsByProvider() {
        val openai = OpenAISpeechProvider.registerPassages(
            passages = listOf(PASSAGE),
            settings = settings(),
        )
        val elevenLabs = ElevenLabsSpeechProvider.registerPassages(
            passages = listOf(PASSAGE),
            settings = settings(),
        )

        assertNotEquals(openai, elevenLabs)
    }

    @Test
    fun registerPassages_repeatsUnderTheSameProvider() {
        val first = OpenAISpeechProvider.registerPassages(
            passages = listOf(PASSAGE),
            settings = settings(),
        )
        val second = OpenAISpeechProvider.registerPassages(
            passages = listOf(PASSAGE),
            settings = settings(apiKey = "another key"),
        )

        // The key is not part of the audio, so rotating it must not re-synthesize.
        assertEquals(first, second)
    }

    @Test
    fun registerPassages_differsByVoice() {
        val alloy = OpenAISpeechProvider.registerPassages(
            passages = listOf(PASSAGE),
            settings = settings(voice = "alloy"),
        )
        val echo = OpenAISpeechProvider.registerPassages(
            passages = listOf(PASSAGE),
            settings = settings(voice = "echo"),
        )

        assertNotEquals(alloy, echo)
    }

    @Test
    fun registerPassages_differsByBaseUrl() {
        val local = OpenAICompatibleSpeechProvider.registerPassages(
            passages = listOf(PASSAGE),
            settings = settings(baseUrl = "http://localhost:8880/v1"),
        )
        val remote = OpenAICompatibleSpeechProvider.registerPassages(
            passages = listOf(PASSAGE),
            settings = settings(baseUrl = "https://speech.example.com/v1"),
        )

        // Two servers, two different voices under the same name: neither may replay the other's.
        assertNotEquals(local, remote)
    }

    @Test
    fun registerPassages_resolvesEveryPassage() {
        val uris = ElevenLabsSpeechProvider.registerPassages(
            passages = listOf(PASSAGE, "A second passage."),
            settings = settings(voice = "voice-id"),
        )

        assertEquals(2, uris.size)
        uris.forEach {
            assertTrue(it.startsWith("capytts://"))
            assertNotEquals(null, SpeechPassageRegistry.resolve(it))
        }
    }

    @Test
    fun openAIRequest_authenticatesWithBearerToken() {
        val request = OpenAISpeechProvider.request(PASSAGE, settings(apiKey = "sk-key"))

        assertEquals("https://api.openai.com/v1/audio/speech", request.url)
        assertEquals("Bearer sk-key", request.headers["Authorization"])
        assertTrue(request.body.decodeToString().contains("\"input\":\"$PASSAGE\""))
    }

    @Test
    fun elevenLabsRequest_authenticatesWithItsOwnHeaderAndVoiceInThePath() {
        val request = ElevenLabsSpeechProvider.request(PASSAGE, settings(voice = "abc123", apiKey = "el-key"))

        assertEquals("https://api.elevenlabs.io/v1/text-to-speech/abc123", request.url)
        assertEquals("el-key", request.headers["xi-api-key"])
        assertEquals(null, request.headers["Authorization"])
        assertTrue(request.body.decodeToString().contains("\"text\":\"$PASSAGE\""))
    }

    @Test
    fun openAICompatibleRequest_postsToTheConfiguredEndpoint() {
        val request = OpenAICompatibleSpeechProvider.request(
            PASSAGE,
            settings(baseUrl = "http://localhost:8880/v1/", apiKey = "key"),
        )

        assertEquals("http://localhost:8880/v1/audio/speech", request.url)
        assertEquals("Bearer key", request.headers["Authorization"])
        assertTrue(request.body.decodeToString().contains("\"input\":\"$PASSAGE\""))
    }

    @Test
    fun openAICompatibleRequest_sendsNoAuthorizationWithoutAKey() {
        val request = OpenAICompatibleSpeechProvider.request(
            PASSAGE,
            settings(baseUrl = "http://localhost:8880/v1", apiKey = ""),
        )

        assertEquals(null, request.headers["Authorization"])
    }

    @Test
    fun openAICompatible_treatsAnEmptyKeyAsConfigured() {
        assertNull(
            OpenAICompatibleSpeechProvider.configurationError(
                settings(baseUrl = "http://localhost:8880/v1", apiKey = "")
            )
        )
    }

    @Test
    fun openAICompatible_rejectsAMissingOrMalformedBaseUrl() {
        assertEquals(
            R.string.listen_error_invalid_base_url,
            OpenAICompatibleSpeechProvider.configurationError(settings(baseUrl = ""))
        )
        assertEquals(
            R.string.listen_error_invalid_base_url,
            OpenAICompatibleSpeechProvider.configurationError(settings(baseUrl = "localhost:8880"))
        )
    }

    @Test
    fun namedProviders_stillRequireAKey() {
        assertEquals(
            R.string.listen_error_missing_credentials,
            OpenAISpeechProvider.configurationError(settings(apiKey = ""))
        )
    }

    @Test
    fun from_fallsBackToTheDefault() {
        assertEquals(OpenAISpeechProvider, SpeechProvider.from("openai"))
        assertEquals(ElevenLabsSpeechProvider, SpeechProvider.from("elevenlabs"))
        assertEquals(OpenAICompatibleSpeechProvider, SpeechProvider.from("openai_compatible"))
        assertEquals(SpeechProvider.default, SpeechProvider.from("a provider since removed"))
    }
}

private fun settings(
    voice: String = "alloy",
    apiKey: String = "key",
    baseUrl: String = "http://localhost:8880/v1",
) = SpeechSettings(voice = voice, apiKey = apiKey, baseUrl = baseUrl)

private const val PASSAGE = "The quick brown fox."
