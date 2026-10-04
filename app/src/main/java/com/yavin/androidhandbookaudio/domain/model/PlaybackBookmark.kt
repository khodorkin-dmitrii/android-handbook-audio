package com.yavin.androidhandbookaudio.domain.model

data class PlaybackBookmark(
    val trackId: String,
    val title: String,
    val playlistId: String? = null,
    val playlistTitle: String? = null,
    val language: String,
    val audioUrl: String,
    val positionMs: Long,
    val playbackSpeed: Float,
    val timedTranscriptUrl: String? = null,
    val timedTranscriptFormat: String? = null,
)
