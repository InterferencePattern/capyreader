package com.capyreader.app.ui.articles.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackSpeedTest {
    @Test
    fun next_movesToTheFollowingOption() {
        assertEquals(1.25f, PlaybackSpeed.next(1f))
    }

    @Test
    fun next_wrapsAtTheEnd() {
        assertEquals(PlaybackSpeed.OPTIONS.first(), PlaybackSpeed.next(PlaybackSpeed.OPTIONS.last()))
    }

    @Test
    fun next_fallsBackForAnUnknownSpeed() {
        assertEquals(PlaybackSpeed.OPTIONS.first(), PlaybackSpeed.next(3.3f))
    }

    @Test
    fun label_dropsTrailingZeros() {
        assertEquals("1", PlaybackSpeed.label(1f))
        assertEquals("1.5", PlaybackSpeed.label(1.5f))
        assertEquals("0.75", PlaybackSpeed.label(0.75f))
    }
}
