package com.yavin.androidhandbookaudio.domain.model

data class Playlist(
    val id: String,
    val titles: Map<String, String>,
    val availableLanguages: List<String>,
    val manifestUrl: String,
)
