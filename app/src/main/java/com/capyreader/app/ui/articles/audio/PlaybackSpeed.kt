package com.capyreader.app.ui.articles.audio

/**
 * The speeds the player offers, in the order tapping cycles through them. Speed is applied by the
 * player to audio it already has, so it is free to change, instant, and works on cached audio --
 * unlike Voice, which is a property of the Speech Provider's output. It is deliberately absent
 * from the media cache key: two listens at different speeds are the same synthesized audio.
 *
 * ponytail: tap-to-cycle through a fixed list rather than a menu or a slider. The list stops at 2x
 * because pitch correction is what keeps a Voice natural and it stops sounding natural past that;
 * widen the list if anyone asks for faster.
 */
internal object PlaybackSpeed {
    const val DEFAULT = 1f

    val OPTIONS = listOf(0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

    fun next(current: Float): Float {
        val index = OPTIONS.indexOf(current)

        return OPTIONS[(index + 1) % OPTIONS.size]
    }

    /** "1", "1.25" -- the multiplication sign lives in the string resource. */
    fun label(speed: Float): String {
        return if (speed % 1f == 0f) {
            speed.toInt().toString()
        } else {
            "%.2f".format(speed).trimEnd('0')
        }
    }
}
