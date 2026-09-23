package com.yanga.client.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.LocalFavoriteBoard
import com.yanga.client.data.NgaReadOnlyRepository
import com.yanga.client.data.boards.BoardsCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BoardsListViewModel(
  private val repository: NgaReadOnlyRepository,
  private val boardsCatalog: BoardsCatalog? = null,
) : ViewModel() {
  private val _state = MutableStateFlow(BoardsUiState())
  val state: StateFlow<BoardsUiState> = _state.asStateFlow()

  init {
    boardsCatalog?.let { catalog ->
      _state.value = catalog.state.value
      viewModelScope.launch {
        catalog.state.collect { loaded -> _state.value = loaded }
      }
    }
  }

  private var initialized = false
  private var loadedSession: LoginSessionData? = null
  private var loadedEndpoint: String? = null

  fun ensureLoaded(session: LoginSessionData?, endpoint: String) {
    if (initialized && loadedSession == session && loadedEndpoint == endpoint) return
    initialized = true
    loadedSession = session
    loadedEndpoint = endpoint
    applyEndpoint(endpoint)
    refresh(session)
  }

  fun refresh(session: LoginSessionData?) {
    val catalog = boardsCatalog
    if (catalog != null) {
      catalog.reload(session)
      return
    }
    refreshFromRepository(session)
  }

  fun applyEndpoint(baseUrl: String) {
    boardsCatalog?.applyEndpoint(baseUrl)
  }

  fun onBoardOpened() {
    boardsCatalog?.onBoardOpened()
  }

  fun toggleBoardFavorite(board: BoardPreview) {
    boardsCatalog?.toggleLocalFavorite(board) ?: toggleBoardFavoriteLocally(board)
  }

  private fun toggleBoardFavoriteLocally(board: BoardPreview) {
    viewModelScope.launch {
      val currentlyFavorite = board.isFavorite
      if (currentlyFavorite) {
        repository.removeLocalFavoriteBoard(board.id)
      } else {
        repository.addLocalFavoriteBoard(
          LocalFavoriteBoard(
            boardId = board.id,
            name = board.name,
            iconUrl = board.iconUrl,
            category = board.category,
          ),
        )
      }
      applyLocalFavorites()
    }
  }

  private fun refreshFromRepository(session: LoginSessionData?) {
    viewModelScope.launch {
      applyLocalFavorites()
      _state.update { it.copy(sections = LoadableUiState.Loading) }
      val result = repository.loadBoards(session)
      _state.update { current ->
        val favoriteIds = (current.subscribedBoards as? LoadableUiState.Content)
          ?.value.orEmpty().map { it.id }.toSet()
        result.fold(
          onSuccess = { data ->
            current.copy(
              sections = LoadableUiState.Content(data.remoteSections.map { it.toPreview() }.markFavoriteBoards(favoriteIds)),
            )
          },
          onFailure = { error -> current.copy(sections = error.toLoadableError()) },
        )
      }
    }
  }

  private suspend fun applyLocalFavorites() {
    val localFavorites = repository.listLocalFavoriteBoards().getOrElse { return }
    val favoriteIds = localFavorites.map { it.boardId }.toSet()
    _state.update { current ->
      current.copy(
        subscribedBoards = LoadableUiState.Content(localFavorites.map { it.toBoardPreview() }),
        sections = current.sections.withFavoriteSections(favoriteIds),
      )
    }
  }
}
