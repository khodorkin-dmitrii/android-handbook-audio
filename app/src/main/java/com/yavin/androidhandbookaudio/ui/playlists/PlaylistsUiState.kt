package com.yavin.androidhandbookaudio.ui.playlists

sealed interface PlaylistsUiState {
    data object Loading : PlaylistsUiState

    data class Content(val playlists: List<PlaylistUiModel>) : PlaylistsUiState

    data object Empty : PlaylistsUiState

    data object Error : PlaylistsUiState
}

data class PlaylistUiModel(
    val id: String,
    val title: String,
    val languages: List<String>,
)
