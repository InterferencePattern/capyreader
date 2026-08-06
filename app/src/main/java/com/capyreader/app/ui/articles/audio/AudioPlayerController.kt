package com.capyreader.app.ui.articles.audio

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.capyreader.app.common.AudioEnclosure
import com.capyreader.app.preferences.AppPreferences
import com.google.common.util.concurrent.ListenableFuture
import com.jocmp.capy.logging.CapyLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * How far before the end of a Passage the next one is handed to the player. It covers the round
 * trip to the Speech Provider -- a second or two before the first audio arrives, after which the
 * rest streams faster than it plays -- while exposing only the last seconds of a Passage to being
 * paid for and never heard. Handing over the whole article up front would instead risk a wasted
 * Passage on every article abandoned midway. See ADR-0001.
 *
 * ponytail: a fixed lead rather than one measured from recent requests; raise it if Passage
 * boundaries audibly gap on a slow connection.
 */
private const val PRELOAD_LEAD_MS = 10_000L

class AudioPlayerController(
    private val context: Context,
    appPreferences: AppPreferences,
) {
    private val speedPreference = appPreferences.playbackSpeed

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var positionUpdateJob: Job? = null
    private val mainScope = CoroutineScope(Dispatchers.Main)

    // Passages the player does not hold yet. Each is handed over shortly before the one before
    // it ends rather than up front, so abandoning a Spoken Article halfway never pays for the
    // rest of it. See ADR-0001.
    private var queuedUrls: List<String> = emptyList()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _currentAudio = MutableStateFlow<AudioEnclosure?>(null)
    val currentAudio: StateFlow<AudioEnclosure?> = _currentAudio.asStateFlow()

    private val _playbackError = MutableStateFlow<PlaybackError?>(null)
    val playbackError: StateFlow<PlaybackError?> = _playbackError.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(speedPreference.get())
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private fun ensureController(onReady: (MediaController) -> Unit) {
        mediaController?.let {
            if (it.isConnected) {
                onReady(it)
                return
            }
        }

        val sessionToken = SessionToken(
            context,
            ComponentName(context, MediaPlaybackService::class.java)
        )

        controllerFuture = MediaController.Builder(context, sessionToken)
            .setApplicationLooper(Looper.getMainLooper())
            .buildAsync()
        controllerFuture?.addListener({
            try {
                val controller = controllerFuture?.get()
                mediaController = controller
                controller?.let {
                    setupPlayerListener(it)
                    onReady(it)
                }
            } catch (e: Exception) {
                CapyLog.error("audio_player", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun setupPlayerListener(controller: MediaController) {
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startPositionUpdates()
                } else {
                    stopPositionUpdates()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _duration.value = controller.duration
                }
                if (playbackState == Player.STATE_ENDED) {
                    // Normally empty by now -- [preloadNextPassage] hands each Passage over
                    // before the one before it ends. This is the fallback for a Passage whose
                    // length never became known, where the seam is audible but playback continues.
                    val nextUrl = queuedUrls.firstOrNull()

                    if (nextUrl == null) {
                        _isPlaying.value = false
                        controller.pause()
                    } else {
                        queuedUrls = queuedUrls.drop(1)
                        load(controller, nextUrl)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                CapyLog.error("audio_player", error)

                // Not retried automatically: against an exhausted quota that is another paid
                // request which fails the same way, and it delays an honest message by seconds.
                // The remaining Passages are kept so a manual retry picks the article back up.
                _isPlaying.value = false
                _playbackError.value = PlaybackError.from(error)
            }
        })
    }

    /**
     * Plays [audio]. A Spoken Article passes the URIs of its remaining Passages as [queuedUrls];
     * each is loaded only once the one before it finishes.
     */
    @OptIn(UnstableApi::class)
    fun play(audio: AudioEnclosure, queuedUrls: List<String> = emptyList()) {
        mainScope.launch {
            val currentUrl = _currentAudio.value?.url

            if (currentUrl == audio.url && mediaController?.isConnected == true) {
                resume()
                return@launch
            }

            _playbackError.value = null
            this@AudioPlayerController.queuedUrls = queuedUrls

            ensureController { controller ->
                _currentAudio.value = audio
                audio.durationSeconds?.let {
                    _duration.value = it * 1000
                }

                load(controller, audio.url)
            }
        }
    }

    /** Queues the next Passage when the current one is nearly over. No-op until its length is known. */
    private fun preloadNextPassage(controller: MediaController) {
        val nextUrl = queuedUrls.firstOrNull() ?: return
        val duration = controller.duration

        if (duration <= 0 || duration - controller.currentPosition > PRELOAD_LEAD_MS) {
            return
        }

        queuedUrls = queuedUrls.drop(1)
        controller.addMediaItem(mediaItem(url = nextUrl))
    }

    private fun load(controller: MediaController, url: String) {
        controller.setMediaItem(mediaItem(url = url))
        // Re-applied per item so a session the service rebuilt still starts at the saved speed.
        controller.setPlaybackSpeed(_playbackSpeed.value)
        controller.prepare()
        controller.playWhenReady = true
    }

    private fun mediaItem(url: String): MediaItem {
        val audio = _currentAudio.value

        return MediaItem.Builder()
            .setUri(url)
            // Pinned so a Spoken Article caches under its synthetic URI: the ResolvingDataSource
            // rewrites the URI upstream, and without this the key would follow it.
            .setCustomCacheKey(url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(audio?.title)
                    .setArtist(audio?.feedName)
                    .setArtworkUri(audio?.artworkUrl?.let { Uri.parse(it) })
                    .build()
            )
            .build()
    }

    fun pause() {
        mainScope.launch {
            mediaController?.pause()
        }
    }

    /** Doubles as the retry: after an error the failed item is still loaded. */
    fun resume() {
        mainScope.launch {
            if (_playbackError.value != null) {
                _playbackError.value = null
                // Resumes at the Passage that failed. Everything before it is already in the
                // media cache under its content hash, so nothing played is paid for twice.
                mediaController?.prepare()
            }

            mediaController?.play()
        }
    }

    /**
     * Takes effect on the audio the player already holds: nothing is re-synthesized, no request
     * reaches the Speech Provider, and the media cache key is untouched. Pitch is preserved by the
     * player's own time-stretching.
     */
    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        speedPreference.set(speed)

        mainScope.launch {
            mediaController?.setPlaybackSpeed(speed)
        }
    }

    fun seekTo(positionMs: Long) {
        mainScope.launch {
            mediaController?.seekTo(positionMs)
            _currentPosition.value = positionMs
        }
    }

    fun skipBack() {
        mainScope.launch {
            mediaController?.let { controller ->
                val newPosition = SkipCalculator.skipBack(controller.currentPosition)
                controller.seekTo(newPosition)
                _currentPosition.value = newPosition
            }
        }
    }

    fun skipForward() {
        mainScope.launch {
            mediaController?.let { controller ->
                val newPosition = SkipCalculator.skipForward(controller.currentPosition, controller.duration)
                controller.seekTo(newPosition)
                _currentPosition.value = newPosition
            }
        }
    }

    fun dismiss() {
        mainScope.launch {
            mediaController?.let { controller ->
                controller.stop()
                controller.clearMediaItems()
            }
            queuedUrls = emptyList()
            _currentAudio.value = null
            _playbackError.value = null
            _isPlaying.value = false
            _currentPosition.value = 0L
            _duration.value = 0L
        }
    }

    fun release() {
        stopPositionUpdates()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        mediaController = null
        controllerFuture = null
    }

    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionUpdateJob = mainScope.launch {
            while (isActive) {
                mediaController?.let {
                    _currentPosition.value = it.currentPosition
                    if (_duration.value == 0L && it.duration > 0) {
                        _duration.value = it.duration
                    }
                    // Only runs while playing, which is exactly when a Passage is worth fetching.
                    preloadNextPassage(it)
                }
                delay(500)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }
}
