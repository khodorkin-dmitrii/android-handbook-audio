package com.yavin.androidhandbookaudio.ui.playlists

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.yavin.androidhandbookaudio.domain.model.AppThemeMode
import com.yavin.androidhandbookaudio.ui.components.PlayerAmbientBackground
import com.yavin.androidhandbookaudio.ui.components.ThemeModeAction
import com.yavin.androidhandbookaudio.ui.theme.AndroidHandbookAudioTheme

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PlaylistsScreen(
    state: PlaylistsUiState,
    onRetry: () -> Unit,
    onPlaylistClick: (String) -> Unit,
    playbackProgress: Float? = null,
    themeMode: AppThemeMode,
    onCycleThemeMode: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        PlayerAmbientBackground(
            progress = playbackProgress,
            modifier = Modifier.matchParentSize(),
        )
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                TopAppBar(
                    title = { Text("Playlists") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
                    actions = {
                        ThemeModeAction(themeMode, onCycleThemeMode)
                    },
                )
            },
        ) { contentPadding ->
            when (state) {
                PlaylistsUiState.Loading -> CenteredContent(contentPadding) {
                    CircularProgressIndicator()
                }

                PlaylistsUiState.Empty -> CenteredContent(contentPadding) {
                    Text("No playlists available")
                }

                PlaylistsUiState.Error -> CenteredContent(contentPadding) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Could not load playlists")
                        Button(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                }

                is PlaylistsUiState.Content -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = contentPadding.calculateTopPadding() + 16.dp,
                        end = 16.dp,
                        bottom = contentPadding.calculateBottomPadding() + 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.playlists, key = PlaylistUiModel::id) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = dropUnlessResumed { onPlaylistClick(playlist.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: PlaylistUiModel,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = playlist.languages.joinToString(separator = " / "),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CenteredContent(
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
    name = "Playlists — light",
    showBackground = true,
    widthDp = 393,
    heightDp = 852,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "Playlists — dark",
    showBackground = true,
    widthDp = 393,
    heightDp = 852,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PlaylistsScreenPreview() {
    AndroidHandbookAudioTheme(dynamicColor = false) {
        PlaylistsScreen(
            state = PlaylistsUiState.Content(
                playlists = listOf(
                    PlaylistUiModel(
                        id = "shorts",
                        title = "Short Audio Notes",
                        languages = listOf("EN", "RU"),
                    ),
                    PlaylistUiModel(
                        id = "deep-dives",
                        title = "Android Deep Dives",
                        languages = listOf("EN"),
                    ),
                ),
            ),
            onRetry = {},
            onPlaylistClick = {},
            themeMode = AppThemeMode.SYSTEM,
            onCycleThemeMode = { _ -> },
        )
    }
}

@Preview(
    name = "Playlist card",
    showBackground = true,
    widthDp = 393,
)
@Composable
private fun PlaylistCardPreview() {
    AndroidHandbookAudioTheme(dynamicColor = false) {
        PlaylistCard(
            playlist = PlaylistUiModel(
                id = "shorts",
                title = "Short Audio Notes",
                languages = listOf("EN", "RU"),
            ),
            onClick = {},
        )
    }
}
