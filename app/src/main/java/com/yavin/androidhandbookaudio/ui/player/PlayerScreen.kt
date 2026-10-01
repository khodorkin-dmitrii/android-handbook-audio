package com.yavin.androidhandbookaudio.ui.player

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yavin.androidhandbookaudio.domain.model.TranscriptSegment
import com.yavin.androidhandbookaudio.domain.model.TranscriptStyleRange
import com.yavin.androidhandbookaudio.domain.model.TranscriptTextStyle
import com.yavin.androidhandbookaudio.ui.theme.AndroidHandbookAudioTheme

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
    onLanguageSelected: (String) -> Unit,
    onTranscriptSegmentClick: (Long) -> Unit,
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
                onLanguageSelected = onLanguageSelected,
                onTranscriptSegmentClick = onTranscriptSegmentClick,
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
    onLanguageSelected: (String) -> Unit,
    onTranscriptSegmentClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isSeeking by remember(state.trackId) { mutableStateOf(false) }
    var sliderPosition by remember(state.trackId) { mutableFloatStateOf(0f) }
    LaunchedEffect(state.positionMs, state.durationMs, isSeeking) {
        if (!isSeeking) sliderPosition = state.positionMs.toFloat()
    }

    val transcript = state.transcript
    Column(
        modifier = modifier
            .fillMaxSize(),
    ) {
        if (transcript is TranscriptUiState.Content) {
            PlayerControls(
                state = state,
                isSeeking = isSeeking,
                sliderPosition = sliderPosition,
                onSeekingChange = { seeking, position ->
                    isSeeking = seeking
                    sliderPosition = position
                },
                onPlayOrPause = onPlayOrPause,
                onSeekTo = onSeekTo,
                onSeekBackward = onSeekBackward,
                onSeekForward = onSeekForward,
                onPrevious = onPrevious,
                onNext = onNext,
                onSpeedSelected = onSpeedSelected,
                onLanguageSelected = onLanguageSelected,
                modifier = Modifier.weight(1f),
            )
            HorizontalDivider()
            TranscriptPanel(
                state = transcript,
                onSegmentClick = onTranscriptSegmentClick,
                modifier = Modifier.weight(1f),
            )
        } else {
            PlayerControls(
                state = state,
                isSeeking = isSeeking,
                sliderPosition = sliderPosition,
                onSeekingChange = { seeking, position ->
                    isSeeking = seeking
                    sliderPosition = position
                },
                onPlayOrPause = onPlayOrPause,
                onSeekTo = onSeekTo,
                onSeekBackward = onSeekBackward,
                onSeekForward = onSeekForward,
                onPrevious = onPrevious,
                onNext = onNext,
                onSpeedSelected = onSpeedSelected,
                onLanguageSelected = onLanguageSelected,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PlayerControls(
    state: PlayerUiState.Active,
    isSeeking: Boolean,
    sliderPosition: Float,
    onSeekingChange: (Boolean, Float) -> Unit,
    onPlayOrPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBackward: () -> Unit,
    onSeekForward: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = state.title,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = listOfNotNull(
                state.language?.uppercase(),
                state.status.displayName(),
                state.transcript.statusLabel(),
            ).joinToString(" • "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.errorMessage?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error)
        }

        when (state.availableLanguages.size) {
            2 -> SingleChoiceSegmentedButtonRow {
                state.availableLanguages.forEachIndexed { index, language ->
                    SegmentedButton(
                        selected = state.language == language,
                        onClick = { onLanguageSelected(language) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = state.availableLanguages.size,
                        ),
                        label = { Text(language.uppercase()) },
                    )
                }
            }

            in 3..Int.MAX_VALUE -> Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.availableLanguages.forEach { language ->
                    FilterChip(
                        selected = state.language == language,
                        onClick = { onLanguageSelected(language) },
                        label = { Text(language.uppercase()) },
                    )
                }
            }
        }

        val durationMs = state.durationMs
        AudioSeekBar(
            value = sliderPosition.coerceIn(0f, durationMs?.toFloat() ?: 0f),
            onValueChange = { value ->
                onSeekingChange(true, value)
            },
            onValueChangeFinished = {
                onSeekTo(sliderPosition.toLong())
                onSeekingChange(false, sliderPosition)
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
                Text(
                    when (state.status) {
                        PlayerStatus.PLAYING -> "Pause"
                        PlayerStatus.ERROR -> "Retry"
                        else -> "Play"
                    },
                )
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

@Composable
private fun TranscriptPanel(
    state: TranscriptUiState.Content,
    onSegmentClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.activeSegmentIndex) {
        val activeIndex = state.activeSegmentIndex ?: return@LaunchedEffect
        if (!listState.isScrollInProgress) {
            listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 24.dp,
            vertical = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        itemsIndexed(
            items = state.segments,
            key = { index, segment -> "${segment.startMs}-$index" },
        ) { index, segment ->
            val isActive = index == state.activeSegmentIndex
            val styledText = remember(segment) { segment.toAnnotatedString() }
            Text(
                text = styledText,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSegmentClick(segment.startMs) }
                    .padding(vertical = 4.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = if (isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

internal fun TranscriptSegment.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
    append(text)
    styleRanges.forEach { range ->
        val start = range.start.coerceIn(0, text.length)
        val endExclusive = range.endExclusive.coerceIn(start, text.length)
        if (start == endExclusive) return@forEach

        val spanStyle = when (range.style) {
            TranscriptTextStyle.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
            TranscriptTextStyle.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
            TranscriptTextStyle.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
        }
        addStyle(spanStyle, start, endExclusive)
    }
}

private fun PlayerStatus.displayName(): String = when (this) {
    PlayerStatus.BUFFERING -> "Buffering"
    PlayerStatus.PLAYING -> "Playing"
    PlayerStatus.PAUSED -> "Paused"
    PlayerStatus.ERROR -> "Playback error"
}

private fun TranscriptUiState.statusLabel(): String? = when (this) {
    TranscriptUiState.Loading -> "Loading transcript…"
    is TranscriptUiState.Error -> message
    is TranscriptUiState.Content, TranscriptUiState.Unavailable -> null
}

@Preview(
    name = "Player controls",
    showBackground = true,
    widthDp = 393,
    heightDp = 480,
)
@Composable
private fun PlayerControlsPreview() {
    AndroidHandbookAudioTheme(dynamicColor = false) {
        Surface {
            PlayerControls(
                state = previewPlayerState(),
                isSeeking = false,
                sliderPosition = 78_000f,
                onSeekingChange = { _, _ -> },
                onPlayOrPause = {},
                onSeekTo = {},
                onSeekBackward = {},
                onSeekForward = {},
                onPrevious = {},
                onNext = {},
                onSpeedSelected = {},
                onLanguageSelected = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(
    name = "Player controls — loading",
    showBackground = true,
    widthDp = 393,
    heightDp = 480,
)
@Composable
private fun PlayerControlsLoadingPreview() {
    AndroidHandbookAudioTheme(dynamicColor = false) {
        Surface {
            PlayerControls(
                state = previewPlayerState(
                    status = PlayerStatus.BUFFERING,
                    transcript = TranscriptUiState.Loading,
                ),
                isSeeking = false,
                sliderPosition = 78_000f,
                onSeekingChange = { _, _ -> },
                onPlayOrPause = {},
                onSeekTo = {},
                onSeekBackward = {},
                onSeekForward = {},
                onPrevious = {},
                onNext = {},
                onSpeedSelected = {},
                onLanguageSelected = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(
    name = "Player with transcript — light",
    showBackground = true,
    widthDp = 393,
    heightDp = 852,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "Player with transcript — dark",
    showBackground = true,
    widthDp = 393,
    heightDp = 852,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PlayerScreenPreview() {
    AndroidHandbookAudioTheme(dynamicColor = false) {
        PlayerScreen(
            state = previewPlayerState(
                transcript = TranscriptUiState.Content(
                    segments = previewTranscriptSegments,
                    activeSegmentIndex = 1,
                ),
            ),
            onBack = {},
            onPlayOrPause = {},
            onSeekTo = {},
            onSeekBackward = {},
            onSeekForward = {},
            onPrevious = {},
            onNext = {},
            onSpeedSelected = {},
            onLanguageSelected = {},
            onTranscriptSegmentClick = {},
        )
    }
}

private fun previewPlayerState(
    status: PlayerStatus = PlayerStatus.PLAYING,
    transcript: TranscriptUiState = TranscriptUiState.Unavailable,
) = PlayerUiState.Active(
    trackId = "shorts.libraries-build",
    title = "Libraries & Build",
    language = "en",
    availableLanguages = listOf("en", "ru"),
    status = status,
    positionMs = 78_000,
    durationMs = 184_000,
    bufferedPositionMs = 126_000,
    playbackSpeed = 1.25f,
    hasPrevious = true,
    hasNext = true,
    errorMessage = null,
    transcript = transcript,
)

private val previewTranscriptSegments = listOf(
    TranscriptSegment(
        startMs = 74_000,
        endMs = 77_000,
        text = previewQuestion,
        styleRanges = listOf(
            TranscriptStyleRange(
                start = 0,
                endExclusive = previewQuestion.length,
                style = TranscriptTextStyle.ITALIC,
            ),
        ),
    ),
    TranscriptSegment(
        startMs = 77_000,
        endMs = 81_000,
        text = "An Android library is built as an AAR and published to a Maven repository.",
    ),
    TranscriptSegment(
        startMs = 81_000,
        endMs = 85_000,
        text = "Maven Central, GitHub Packages, Nexus, and Artifactory are common options.",
    ),
)

private const val previewQuestion = "How and where are Android libraries published?"
