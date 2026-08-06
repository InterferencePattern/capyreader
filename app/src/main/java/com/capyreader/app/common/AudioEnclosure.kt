package com.capyreader.app.common

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AudioEnclosure(
    val url: String,
    val title: String,
    val feedName: String,
    val durationSeconds: Long?,
    val artworkUrl: String?
) {
    companion object
}

// A Spoken Article is represented by this same type with a synthetic `capytts://` URL --
// see CONTEXT.md. The floating player uses this scheme, not missing duration metadata, to
// decide whether a seek bar makes sense.
const val SPOKEN_ARTICLE_SCHEME = "capytts"

val AudioEnclosure.isSpokenArticle: Boolean
    get() = url.startsWith("$SPOKEN_ARTICLE_SCHEME://")

val AudioEnclosure.Companion.Saver
    get() = Saver<MutableState<AudioEnclosure?>, String>(
        save = { state ->
            Json.encodeToString(state.value)
        },
        restore = { jsonString ->
            mutableStateOf(Json.decodeFromString(jsonString))
        }
    )
