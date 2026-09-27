package com.yavin.androidhandbookaudio.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yavin.androidhandbookaudio.domain.model.Playlist
import com.yavin.androidhandbookaudio.domain.repository.CatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<PlaylistsUiState>(PlaylistsUiState.Loading)
    val uiState: StateFlow<PlaylistsUiState> = _uiState.asStateFlow()

    init {
        loadPlaylists()
    }

    fun retry() {
        loadPlaylists()
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            _uiState.value = PlaylistsUiState.Loading
            _uiState.value = runCatching { catalogRepository.getPlaylists() }
                .fold(
                    onSuccess = { playlists ->
                        if (playlists.isEmpty()) {
                            PlaylistsUiState.Empty
                        } else {
                            PlaylistsUiState.Content(playlists.map(Playlist::toUiModel))
                        }
                    },
                    onFailure = { PlaylistsUiState.Error },
                )
        }
    }
}

private fun Playlist.toUiModel(): PlaylistUiModel = PlaylistUiModel(
    id = id,
    title = titles["en"] ?: titles.values.first(),
    languages = availableLanguages.map(String::uppercase),
)
