package com.yavin.androidhandbookaudio.domain.repository

import com.yavin.androidhandbookaudio.domain.model.PlaybackBookmark
import kotlinx.coroutines.flow.Flow

interface PlaybackPreferencesRepository {
    val bookmark: Flow<PlaybackBookmark?>

    suspend fun saveBookmark(bookmark: PlaybackBookmark)
}
