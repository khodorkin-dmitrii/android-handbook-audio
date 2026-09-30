package com.yavin.androidhandbookaudio.ui.tracks

sealed interface TrackListUiState {
    data object Loading : TrackListUiState

    data class Content(
        val playlistTitle: String,
        val preferredLanguage: String?,
        val availableLanguages: List<String>,
        val tracks: List<TrackUiModel>,
    ) : TrackListUiState

    data object Empty : TrackListUiState

    data object Error : TrackListUiState
}

data class TrackUiModel(
    val id: String,
    val order: Int,
    val title: String,
    val languages: List<String>,
    val activeLanguage: String?,
    val isCurrent: Boolean,
    val isPlaying: Boolean,
    val playbackStatus: TrackPlaybackStatus?,
)

enum class TrackPlaybackStatus {
    BUFFERING,
    PLAYING,
    PAUSED,
    ERROR,
}
