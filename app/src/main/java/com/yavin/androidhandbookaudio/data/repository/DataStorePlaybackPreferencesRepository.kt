package com.yavin.androidhandbookaudio.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yavin.androidhandbookaudio.domain.model.PlaybackBookmark
import com.yavin.androidhandbookaudio.domain.repository.PlaybackPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.playbackPreferencesDataStore by preferencesDataStore(name = "playback_preferences")

@Singleton
class DataStorePlaybackPreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : PlaybackPreferencesRepository {
    override val bookmark: Flow<PlaybackBookmark?> = context.playbackPreferencesDataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map(Preferences::toPlaybackBookmark)

    override suspend fun saveBookmark(bookmark: PlaybackBookmark) {
        context.playbackPreferencesDataStore.edit { preferences ->
            preferences[PlaybackPreferenceKeys.TRACK_ID] = bookmark.trackId
            preferences[PlaybackPreferenceKeys.TITLE] = bookmark.title
            preferences[PlaybackPreferenceKeys.LANGUAGE] = bookmark.language
            preferences[PlaybackPreferenceKeys.AUDIO_URL] = bookmark.audioUrl
            preferences[PlaybackPreferenceKeys.POSITION_MS] = bookmark.positionMs.coerceAtLeast(0)
            preferences[PlaybackPreferenceKeys.PLAYBACK_SPEED] = bookmark.playbackSpeed
        }
    }
}

internal object PlaybackPreferenceKeys {
    val TRACK_ID = stringPreferencesKey("last_track_id")
    val TITLE = stringPreferencesKey("last_track_title")
    val LANGUAGE = stringPreferencesKey("last_rendition_language")
    val AUDIO_URL = stringPreferencesKey("last_audio_url")
    val POSITION_MS = longPreferencesKey("last_position_ms")
    val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
}

internal fun Preferences.toPlaybackBookmark(): PlaybackBookmark? {
    val trackId = this[PlaybackPreferenceKeys.TRACK_ID]?.takeIf(String::isNotBlank) ?: return null
    val title = this[PlaybackPreferenceKeys.TITLE]?.takeIf(String::isNotBlank) ?: return null
    val language = this[PlaybackPreferenceKeys.LANGUAGE]?.takeIf(String::isNotBlank) ?: return null
    val audioUrl = this[PlaybackPreferenceKeys.AUDIO_URL]?.takeIf(String::isNotBlank) ?: return null
    return PlaybackBookmark(
        trackId = trackId,
        title = title,
        language = language,
        audioUrl = audioUrl,
        positionMs = this[PlaybackPreferenceKeys.POSITION_MS]?.coerceAtLeast(0) ?: 0,
        playbackSpeed = this[PlaybackPreferenceKeys.PLAYBACK_SPEED]
            ?.takeIf { it in MIN_PLAYBACK_SPEED..MAX_PLAYBACK_SPEED }
            ?: DEFAULT_PLAYBACK_SPEED,
    )
}

private const val MIN_PLAYBACK_SPEED = 0.25f
private const val MAX_PLAYBACK_SPEED = 3f
private const val DEFAULT_PLAYBACK_SPEED = 1f
