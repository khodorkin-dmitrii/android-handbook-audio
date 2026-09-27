package com.yavin.androidhandbookaudio.domain.model

data class PlaybackBookmark(
    val trackId: String,
    val title: String,
    val language: String,
    val audioUrl: String,
    val positionMs: Long,
    val playbackSpeed: Float,
)
