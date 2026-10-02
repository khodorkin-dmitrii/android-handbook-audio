package com.yavin.androidhandbookaudio.ui.tracks

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yavin.androidhandbookaudio.ui.theme.AndroidHandbookAudioTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackListScreen(
    state: TrackListUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlayOrPause: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = (state as? TrackListUiState.Content)?.playlistTitle ?: "Tracks"
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                },
            )
        },
    ) { contentPadding ->
        when (state) {
            TrackListUiState.Loading -> CenteredTrackContent(contentPadding) {
                CircularProgressIndicator()
            }

            TrackListUiState.Empty -> CenteredTrackContent(contentPadding) {
                Text("No tracks available")
            }

            TrackListUiState.Error -> CenteredTrackContent(contentPadding) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Could not load tracks")
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }

            is TrackListUiState.Content -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = contentPadding.calculateTopPadding() + 16.dp,
                    end = 16.dp,
                    bottom = contentPadding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.tracks, key = TrackUiModel::id) { track ->
                    TrackCard(
                        track = track,
                        onPlayOrPause = onPlayOrPause,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackCard(
    track: TrackUiModel,
    onPlayOrPause: (String) -> Unit,
) {
    Card(
        onClick = { onPlayOrPause(track.id) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "${track.order}. ${track.title}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (track.isCurrent) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                if (track.languages.isNotEmpty()) {
                    Text(
                        text = track.languages.joinToString(" / ") { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                track.playbackStatus?.let { status ->
                    Text(
                        text = status.displayName(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Button(onClick = { onPlayOrPause(track.id) }) {
                Text(if (track.isPlaying) "Pause" else "Play")
            }
        }
    }
}

private fun TrackPlaybackStatus.displayName(): String = when (this) {
    TrackPlaybackStatus.BUFFERING -> "Buffering"
    TrackPlaybackStatus.PLAYING -> "Playing"
    TrackPlaybackStatus.PAUSED -> "Paused"
    TrackPlaybackStatus.ERROR -> "Playback error"
}

@Composable
private fun CenteredTrackContent(
    contentPadding: PaddingValues,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Preview(
    name = "Track list — light",
    showBackground = true,
    widthDp = 393,
    heightDp = 852,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "Track list — dark",
    showBackground = true,
    widthDp = 393,
    heightDp = 852,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun TrackListScreenPreview() {
    AndroidHandbookAudioTheme(dynamicColor = false) {
        TrackListScreen(
            state = previewTrackListState,
            onBack = {},
            onRetry = {},
            onPlayOrPause = {},
        )
    }
}

@Preview(
    name = "Track card — playing",
    showBackground = true,
    widthDp = 393,
)
@Composable
private fun TrackCardPreview() {
    AndroidHandbookAudioTheme(dynamicColor = false) {
        TrackCard(
            track = previewTrackListState.tracks.first { it.isCurrent },
            onPlayOrPause = {},
        )
    }
}

private val previewTrackListState = TrackListUiState.Content(
    playlistTitle = "Short Audio Notes",
    tracks = listOf(
        TrackUiModel(
            id = "shorts.computer-science",
            order = 1,
            title = "Computer Science",
            languages = listOf("en", "ru"),
            isCurrent = false,
            isPlaying = false,
            playbackStatus = null,
        ),
        TrackUiModel(
            id = "shorts.kotlin",
            order = 2,
            title = "Kotlin",
            languages = listOf("en", "ru"),
            isCurrent = true,
            isPlaying = true,
            playbackStatus = TrackPlaybackStatus.PLAYING,
        ),
        TrackUiModel(
            id = "shorts.jetpack-compose",
            order = 3,
            title = "Jetpack Compose",
            languages = listOf("en", "ru"),
            isCurrent = false,
            isPlaying = false,
            playbackStatus = null,
        ),
        TrackUiModel(
            id = "shorts.coroutines-flow",
            order = 4,
            title = "Coroutines & Flow",
            languages = listOf("en", "ru"),
            isCurrent = false,
            isPlaying = false,
            playbackStatus = null,
        ),
    ),
)
