package com.yavin.androidhandbookaudio.ui.player

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.yavin.androidhandbookaudio.R
import com.yavin.androidhandbookaudio.ui.theme.AndroidHandbookAudioTheme

@Composable
fun MiniPlayer(
    state: MiniPlayerUiState,
    onOpenPlayer: () -> Unit,
    onPlayOrPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeLabel = state.durationMs
        ?.takeIf { it > 0 }
        ?.let { durationMs ->
            state.positionMs
                ?.takeIf { it >= 0 }
                ?.let { positionMs -> "${formatPlaybackTime(positionMs)} / ${formatPlaybackTime(durationMs)}" }
                ?: formatPlaybackTime(durationMs)
        }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .clickable(onClick = dropUnlessResumed { onOpenPlayer() }),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.68f),
        ),
    ) {
        Column {
            state.progress?.let { progress ->
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    state.language?.let { language ->
                        Text(
                            text = language,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                timeLabel?.let { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                    )
                }
                FilledIconButton(
                    onClick = onPlayOrPause,
                    enabled = !state.isBuffering,
                ) {
                    if (state.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            painter = painterResource(
                                when {
                                    state.hasError -> R.drawable.ic_refresh_24
                                    state.isPlaying -> R.drawable.ic_pause_24
                                    else -> R.drawable.ic_play_arrow_24
                                },
                            ),
                            contentDescription = when {
                                state.hasError -> "Retry ${state.title}"
                                state.isPlaying -> "Pause ${state.title}"
                                else -> "Play ${state.title}"
                            },
                        )
                    }
                }
            }
        }
    }
}

@Preview(
    name = "Mini player — light",
    showBackground = true,
    widthDp = 393,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "Mini player — dark",
    showBackground = true,
    widthDp = 393,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun MiniPlayerPreview() {
    AndroidHandbookAudioTheme(dynamicColor = false) {
        Surface {
            MiniPlayer(
                state = MiniPlayerUiState(
                    trackId = "shorts.kotlin",
                    title = "Kotlin",
                    language = "EN",
                    isPlaying = true,
                    isBuffering = false,
                    hasError = false,
                    progress = 0.42f,
                    positionMs = 78_000,
                    durationMs = 184_000,
                ),
                onOpenPlayer = {},
                onPlayOrPause = {},
            )
        }
    }
}

@Preview(
    name = "Mini player — buffering",
    showBackground = true,
    widthDp = 393,
)
@Composable
private fun MiniPlayerBufferingPreview() {
    AndroidHandbookAudioTheme(dynamicColor = false) {
        Surface {
            MiniPlayer(
                state = MiniPlayerUiState(
                    trackId = "shorts.coroutines-flow",
                    title = "Coroutines & Flow",
                    language = "RU",
                    isPlaying = false,
                    isBuffering = true,
                    hasError = false,
                    progress = null,
                    durationMs = 184_000,
                ),
                onOpenPlayer = {},
                onPlayOrPause = {},
            )
        }
    }
}
