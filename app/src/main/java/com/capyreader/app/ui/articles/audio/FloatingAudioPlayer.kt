package com.capyreader.app.ui.articles.audio

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.capyreader.app.R
import com.capyreader.app.common.AudioEnclosure
import com.capyreader.app.common.isSpokenArticle
import com.capyreader.app.ui.theme.CapyTheme

@Composable
fun FloatingAudioPlayer(
    audio: AudioEnclosure,
    controller: AudioPlayerController,
    onDismiss: () -> Unit,
    onNavigateToListenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPlaying by controller.isPlaying.collectAsState()
    val currentPosition by controller.currentPosition.collectAsState()
    val duration by controller.duration.collectAsState()
    val error by controller.playbackError.collectAsState()
    val playbackSpeed by controller.playbackSpeed.collectAsState()

    FloatingAudioPlayer(
        audio = audio,
        isPlaying = isPlaying,
        currentPosition = currentPosition,
        duration = duration,
        error = error,
        playbackSpeed = playbackSpeed,
        onPlayPause = { if (isPlaying) controller.pause() else controller.resume() },
        onCycleSpeed = { controller.setPlaybackSpeed(PlaybackSpeed.next(playbackSpeed)) },
        onSeek = { controller.seekTo(it) },
        onRetry = { controller.resume() },
        onNavigateToListenSettings = onNavigateToListenSettings,
        onDismiss = onDismiss,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FloatingAudioPlayer(
    audio: AudioEnclosure,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    error: PlaybackError?,
    playbackSpeed: Float,
    onPlayPause: () -> Unit,
    onCycleSpeed: () -> Unit,
    onSeek: (Long) -> Unit,
    onRetry: () -> Unit,
    onNavigateToListenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(top = 12.dp)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!audio.artworkUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = audio.artworkUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(56.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Headphones,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = audio.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = audio.feedName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Tapping cycles rather than opening a menu: every speed is two or three taps
                // away, and the row has no space for a fourth control that opens something.
                val speedLabel = PlaybackSpeed.label(playbackSpeed)
                val speedDescription =
                    stringResource(R.string.audio_player_speed_description, speedLabel)
                TextButton(
                    onClick = onCycleSpeed,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .widthIn(min = 48.dp)
                        .semantics {
                            contentDescription = speedDescription
                        },
                ) {
                    Text(
                        text = stringResource(R.string.audio_player_speed, speedLabel),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(if (isPlaying) R.string.audio_player_pause else R.string.audio_player_play),
                        modifier = Modifier.size(32.dp),
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.audio_player_close),
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            // The player stays on screen after a failure so retrying is a single tap -- either
            // this button or the play button above, both of which resume the item that failed.
            if (error != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(error.message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )

                    if (error == PlaybackError.Credentials) {
                        TextButton(onClick = onNavigateToListenSettings) {
                            Text(stringResource(R.string.audio_player_error_open_settings))
                        }
                    }

                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.audio_player_error_retry))
                    }
                }
            }
            // Spoken articles have no seek bar: the player only ever knows the duration of the
            // current Passage, not the whole article. This is decided from the URI scheme, not
            // from missing duration metadata -- a podcast enclosure with no duration is still
            // scrubbable. See ADR-0001.
            else if (!audio.isSpokenArticle) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatDuration(currentPosition),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    val interactionSource = remember { MutableInteractionSource() }
                    Slider(
                        value = if (duration > 0) currentPosition / duration.toFloat() else 0f,
                        onValueChange = { fraction ->
                            onSeek((fraction * duration).toLong())
                        },
                        interactionSource = interactionSource,
                        thumb = {
                            SliderDefaults.Thumb(
                                interactionSource = interactionSource,
                                thumbSize = DpSize(width = 4.dp, height = 24.dp),
                            )
                        },
                        track = { sliderState ->
                            SliderDefaults.Track(
                                sliderState = sliderState,
                                drawStopIndicator = null,
                                thumbTrackGapSize = 4.dp,
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                    )

                    Text(
                        text = formatDuration(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

@Preview
@Composable
private fun FloatingAudioPlayerPreview() {
    CapyTheme {
        FloatingAudioPlayer(
            audio = AudioEnclosure(
                url = "https://example.com/episode.mp3",
                title = "Episode 42: The Answer to Everything",
                feedName = "The Podcast Show",
                durationSeconds = 3600,
                artworkUrl = null,
            ),
            isPlaying = false,
            currentPosition = 120_000,
            duration = 3600_000,
            error = null,
            playbackSpeed = 1f,
            onPlayPause = {},
            onCycleSpeed = {},
            onSeek = {},
            onRetry = {},
            onNavigateToListenSettings = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun FloatingAudioPlayerPreview_Playing() {
    CapyTheme {
        FloatingAudioPlayer(
            audio = AudioEnclosure(
                url = "https://example.com/episode.mp3",
                title = "Episode 42: The Answer to Everything",
                feedName = "The Podcast Show",
                durationSeconds = 3600,
                artworkUrl = null,
            ),
            isPlaying = true,
            currentPosition = 1800_000,
            duration = 3600_000,
            error = null,
            playbackSpeed = 1f,
            onPlayPause = {},
            onCycleSpeed = {},
            onSeek = {},
            onRetry = {},
            onNavigateToListenSettings = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun FloatingAudioPlayerPreview_SpokenArticle() {
    CapyTheme {
        FloatingAudioPlayer(
            audio = AudioEnclosure(
                url = "capytts://abc123",
                title = "How Capy Reader Handles Speech",
                feedName = "The Verge",
                durationSeconds = null,
                artworkUrl = null,
            ),
            isPlaying = true,
            currentPosition = 0,
            duration = 0,
            error = null,
            playbackSpeed = 1.75f,
            onPlayPause = {},
            onCycleSpeed = {},
            onSeek = {},
            onRetry = {},
            onNavigateToListenSettings = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun FloatingAudioPlayerPreview_CredentialsError() {
    CapyTheme {
        FloatingAudioPlayer(
            audio = AudioEnclosure(
                url = "capytts://abc123",
                title = "How Capy Reader Handles Speech",
                feedName = "The Verge",
                durationSeconds = null,
                artworkUrl = null,
            ),
            isPlaying = false,
            currentPosition = 0,
            duration = 0,
            error = PlaybackError.Credentials,
            playbackSpeed = 1f,
            onPlayPause = {},
            onCycleSpeed = {},
            onSeek = {},
            onRetry = {},
            onNavigateToListenSettings = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun FloatingAudioPlayerPreview_QuotaError() {
    CapyTheme {
        FloatingAudioPlayer(
            audio = AudioEnclosure(
                url = "capytts://abc123",
                title = "How Capy Reader Handles Speech",
                feedName = "The Verge",
                durationSeconds = null,
                artworkUrl = null,
            ),
            isPlaying = false,
            currentPosition = 0,
            duration = 0,
            error = PlaybackError.Quota,
            playbackSpeed = 1f,
            onPlayPause = {},
            onCycleSpeed = {},
            onSeek = {},
            onRetry = {},
            onNavigateToListenSettings = {},
            onDismiss = {},
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FloatingAudioPlayerPreview_DarkMode() {
    CapyTheme {
        FloatingAudioPlayer(
            audio = AudioEnclosure(
                url = "https://example.com/episode.mp3",
                title = "A Very Long Episode Title That Should Definitely Be Truncated",
                feedName = "The Extremely Long Podcast Name",
                durationSeconds = 7200,
                artworkUrl = null,
            ),
            isPlaying = false,
            currentPosition = 0,
            duration = 7200_000,
            error = null,
            playbackSpeed = 1f,
            onPlayPause = {},
            onCycleSpeed = {},
            onSeek = {},
            onRetry = {},
            onNavigateToListenSettings = {},
            onDismiss = {},
        )
    }
}
