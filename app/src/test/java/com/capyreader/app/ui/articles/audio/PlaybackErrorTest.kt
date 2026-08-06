package com.capyreader.app.ui.articles.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackErrorTest {
    @Test
    fun forStatus_unauthorized() {
        assertEquals(PlaybackError.Credentials, PlaybackError.forStatus(401))
        assertEquals(PlaybackError.Credentials, PlaybackError.forStatus(403))
    }

    @Test
    fun forStatus_quotaExhausted() {
        assertEquals(PlaybackError.Quota, PlaybackError.forStatus(429))
        assertEquals(PlaybackError.Quota, PlaybackError.forStatus(402))
    }

    @Test
    fun forStatus_otherFailure() {
        assertEquals(PlaybackError.Unknown, PlaybackError.forStatus(500))
    }

    @Test
    fun forStatus_noResponseCode() {
        assertEquals(PlaybackError.Unknown, PlaybackError.forStatus(null))
    }
}
