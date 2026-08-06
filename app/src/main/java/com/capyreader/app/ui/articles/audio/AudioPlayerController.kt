package com.capyreader.app.ui.articles.audio

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.capyreader.app.common.AudioEnclosure
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

class AudioPlayerController(
    private val context: Context,
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var positionUpdateJob: Job? = null
    private val mainScope = CoroutineScope(Dispatchers.Main)

    // Passages after the one playing. Only one is ever in the player, so the next is requested
    // when playback reaches it rather than while the current one buffers -- abandoning a Spoken
    // Article halfway never pays for the rest. See ADR-0001.
    private var queuedUrls: List<String> = emptyList()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _currentAudio = MutableStateFlow<AudioEnclosure?>(null)
    val currentAudio: StateFlow<AudioEnclosure?> = _currentAudio.asStateFlow()

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
                mediaController?.play()
                return@launch
            }

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

    private fun load(controller: MediaController, url: String) {
        val audio = _currentAudio.value
        val mediaItem = MediaItem.Builder()
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

        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.playWhenReady = true
    }

    fun pause() {
        mainScope.launch {
            mediaController?.pause()
        }
    }

    fun resume() {
        mainScope.launch {
            mediaController?.play()
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
