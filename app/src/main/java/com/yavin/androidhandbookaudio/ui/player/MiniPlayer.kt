package com.yavin.androidhandbookaudio.ui.player

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yavin.androidhandbookaudio.ui.theme.AndroidHandbookAudioTheme

@Composable
fun MiniPlayer(
    state: MiniPlayerUiState,
    onOpenPlayer: () -> Unit,
    onPlayOrPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onOpenPlayer),
    ) {
        Column {
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
                if (state.isBuffering) {
                    CircularProgressIndicator(modifier = Modifier.padding(10.dp))
                }
                TextButton(onClick = onPlayOrPause) {
                    Text(if (state.isPlaying) "Pause" else "Play")
                }
            }
            state.progress?.let { progress ->
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
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
                    progress = 0.42f,
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
                    progress = null,
                ),
                onOpenPlayer = {},
                onPlayOrPause = {},
            )
        }
    }
}
