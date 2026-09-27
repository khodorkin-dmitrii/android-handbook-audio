package com.yavin.androidhandbookaudio.ui.player

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    state: PlayerUiState,
    onBack: () -> Unit,
    onPlayOrPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBackward: () -> Unit,
    onSeekForward: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Now playing") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                },
            )
        },
    ) { contentPadding ->
        when (state) {
            is PlayerUiState.NoActiveMedia -> NoActivePlayer(
                errorMessage = state.errorMessage,
                modifier = Modifier.padding(contentPadding),
            )

            is PlayerUiState.Active -> ActivePlayer(
                state = state,
                onPlayOrPause = onPlayOrPause,
                onSeekTo = onSeekTo,
                onSeekBackward = onSeekBackward,
                onSeekForward = onSeekForward,
                onPrevious = onPrevious,
                onNext = onNext,
                onSpeedSelected = onSpeedSelected,
                modifier = Modifier.padding(contentPadding),
            )
        }
    }
}

@Composable
private fun NoActivePlayer(
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(errorMessage ?: "Select a track to start playback")
    }
}

@Composable
private fun ActivePlayer(
    state: PlayerUiState.Active,
    onPlayOrPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBackward: () -> Unit,
    onSeekForward: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isSeeking by remember(state.trackId) { mutableStateOf(false) }
    var sliderPosition by remember(state.trackId) { mutableFloatStateOf(0f) }
    LaunchedEffect(state.positionMs, state.durationMs, isSeeking) {
        if (!isSeeking) sliderPosition = state.positionMs.toFloat()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = state.title,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = listOfNotNull(state.language, state.status.displayName()).joinToString(" • "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.status == PlayerStatus.BUFFERING) {
            CircularProgressIndicator()
        }
        state.errorMessage?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error)
        }

        val durationMs = state.durationMs
        Slider(
            value = sliderPosition.coerceIn(0f, durationMs?.toFloat() ?: 0f),
            onValueChange = { value ->
                isSeeking = true
                sliderPosition = value
            },
            onValueChangeFinished = {
                onSeekTo(sliderPosition.toLong())
                isSeeking = false
            },
            valueRange = 0f..(durationMs?.toFloat()?.coerceAtLeast(1f) ?: 1f),
            enabled = durationMs != null && durationMs > 0,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatPlaybackTime(if (isSeeking) sliderPosition.toLong() else state.positionMs))
            Text(durationMs?.let(::formatPlaybackTime) ?: "--:--")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TextButton(onClick = onPrevious, enabled = state.hasPrevious) { Text("Previous") }
            TextButton(onClick = onSeekBackward) { Text("-10s") }
            Button(onClick = onPlayOrPause) {
                Text(if (state.status == PlayerStatus.PLAYING) "Pause" else "Play")
            }
            TextButton(onClick = onSeekForward) { Text("+10s") }
            TextButton(onClick = onNext, enabled = state.hasNext) { Text("Next") }
        }

        Text("Playback speed", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlaybackSpeedOptions.forEach { speed ->
                FilterChip(
                    selected = kotlin.math.abs(state.playbackSpeed - speed) < 0.01f,
                    onClick = { onSpeedSelected(speed) },
                    label = { Text("${speed}x") },
                )
            }
        }
    }
}

private fun PlayerStatus.displayName(): String = when (this) {
    PlayerStatus.BUFFERING -> "Buffering"
    PlayerStatus.PLAYING -> "Playing"
    PlayerStatus.PAUSED -> "Paused"
    PlayerStatus.ERROR -> "Playback error"
}
