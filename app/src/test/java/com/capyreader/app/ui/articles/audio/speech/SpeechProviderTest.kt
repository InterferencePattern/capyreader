package com.capyreader.app.ui.articles.audio.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechProviderTest {
    @Test
    fun registerPassages_differsByProvider() {
        val openai = OpenAISpeechProvider.registerPassages(
            passages = listOf(PASSAGE),
            voice = "alloy",
            apiKey = "key",
        )
        val elevenLabs = ElevenLabsSpeechProvider.registerPassages(
            passages = listOf(PASSAGE),
            voice = "alloy",
            apiKey = "key",
        )

        assertNotEquals(openai, elevenLabs)
    }

    @Test
    fun registerPassages_repeatsUnderTheSameProvider() {
        val first = OpenAISpeechProvider.registerPassages(
            passages = listOf(PASSAGE),
            voice = "alloy",
            apiKey = "key",
        )
        val second = OpenAISpeechProvider.registerPassages(
            passages = listOf(PASSAGE),
            voice = "alloy",
            apiKey = "another key",
        )

        // The key is not part of the audio, so rotating it must not re-synthesize.
        assertEquals(first, second)
    }

    @Test
    fun registerPassages_differsByVoice() {
        val alloy = OpenAISpeechProvider.registerPassages(
            passages = listOf(PASSAGE),
            voice = "alloy",
            apiKey = "key",
        )
        val echo = OpenAISpeechProvider.registerPassages(
            passages = listOf(PASSAGE),
            voice = "echo",
            apiKey = "key",
        )

        assertNotEquals(alloy, echo)
    }

    @Test
    fun registerPassages_resolvesEveryPassage() {
        val uris = ElevenLabsSpeechProvider.registerPassages(
            passages = listOf(PASSAGE, "A second passage."),
            voice = "voice-id",
            apiKey = "key",
        )

        assertEquals(2, uris.size)
        uris.forEach {
            assertTrue(it.startsWith("capytts://"))
            assertNotEquals(null, SpeechPassageRegistry.resolve(it))
        }
    }

    @Test
    fun openAIRequest_authenticatesWithBearerToken() {
        val request = OpenAISpeechProvider.request(PASSAGE, voice = "alloy", apiKey = "sk-key")

        assertEquals("https://api.openai.com/v1/audio/speech", request.url)
        assertEquals("Bearer sk-key", request.headers["Authorization"])
        assertTrue(request.body.decodeToString().contains("\"input\":\"$PASSAGE\""))
    }

    @Test
    fun elevenLabsRequest_authenticatesWithItsOwnHeaderAndVoiceInThePath() {
        val request = ElevenLabsSpeechProvider.request(PASSAGE, voice = "abc123", apiKey = "el-key")

        assertEquals("https://api.elevenlabs.io/v1/text-to-speech/abc123", request.url)
        assertEquals("el-key", request.headers["xi-api-key"])
        assertEquals(null, request.headers["Authorization"])
        assertTrue(request.body.decodeToString().contains("\"text\":\"$PASSAGE\""))
    }

    @Test
    fun from_fallsBackToTheDefault() {
        assertEquals(OpenAISpeechProvider, SpeechProvider.from("openai"))
        assertEquals(ElevenLabsSpeechProvider, SpeechProvider.from("elevenlabs"))
        assertEquals(SpeechProvider.default, SpeechProvider.from("a provider since removed"))
    }
}

private const val PASSAGE = "The quick brown fox."
