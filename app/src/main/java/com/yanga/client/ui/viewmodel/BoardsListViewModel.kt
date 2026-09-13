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
      applyLocalFavorites(boardId = board.id, isFavorite = !currentlyFavorite)
    }
  }

  private fun refreshFromRepository(session: LoginSessionData?) {
    viewModelScope.launch {
      _state.update { it.copy(subscribedBoards = LoadableUiState.Loading, sections = LoadableUiState.Loading) }
      val result = repository.loadBoards(session)
      _state.update { current ->
        result.fold(
          onSuccess = { data ->
            val subscribedBoardPreviews = data.subscribedBoards.map { it.toPreview().copy(isFavorite = true) }
            val favoriteIds = data.subscribedBoards.map { it.boardId }.toSet()
            current.copy(
              subscribedBoards = LoadableUiState.Content(subscribedBoardPreviews),
              sections = LoadableUiState.Content(data.remoteSections.map { it.toPreview() }.markFavoriteBoards(favoriteIds)),
            )
          },
          onFailure = { error ->
            current.copy(
              subscribedBoards = error.toLoadableError(),
              sections = error.toLoadableError(),
            )
          },
        )
      }
    }
  }

  private suspend fun applyLocalFavorites(boardId: String? = null, isFavorite: Boolean? = null) {
    val localFavorites = repository.listLocalFavoriteBoards().getOrDefault(emptyList())
    val localFavoriteIds = localFavorites.map { it.boardId }.toSet()
    val currentState = _state.value
    val subscribedFavorites =
      (currentState.subscribedBoards as? LoadableUiState.Content)
        ?.value
        .orEmpty()
        .filter { it.isFavorite }
        .map { it.id }
        .toSet()
    val sectionFavorites =
      (currentState.sections as? LoadableUiState.Content)
        ?.value
        .orEmpty()
        .flatMap { section -> section.groups.flatMap { group -> group.boards } }
        .filter { it.isFavorite }
        .map { it.id }
        .toSet()

    val favoriteIds = (localFavoriteIds + subscribedFavorites + sectionFavorites).toMutableSet()
    if (boardId != null && isFavorite != null) {
      if (isFavorite) favoriteIds.add(boardId) else favoriteIds.remove(boardId)
    }

    _state.update { state ->
      val sectionList = (state.sections as? LoadableUiState.Content)?.value.orEmpty()
      val existingSubscribed = (state.subscribedBoards as? LoadableUiState.Content)?.value.orEmpty()
      val existingSubscribedById = existingSubscribed.associateBy { it.id }
      val sectionBoardById =
        sectionList
          .flatMap { section -> section.groups.flatMap { group -> group.boards } }
          .associateBy { it.id }
      val localById = localFavorites.associateBy { it.boardId }

      val orderedFavoriteIds =
        buildList {
          existingSubscribed.forEach { b -> if (favoriteIds.contains(b.id)) add(b.id) }
          sectionList
            .flatMap { section -> section.groups.flatMap { group -> group.boards } }
            .forEach { b -> if (favoriteIds.contains(b.id) && b.id !in this) add(b.id) }
          localFavorites.forEach { local -> if (favoriteIds.contains(local.boardId) && local.boardId !in this) add(local.boardId) }
        }

      val subscribedBoards =
        orderedFavoriteIds.mapNotNull { id ->
          existingSubscribedById[id] ?: sectionBoardById[id] ?: localById[id]?.toBoardPreview()
        }.map { it.copy(isFavorite = true) }

      state.copy(
        subscribedBoards = LoadableUiState.Content(subscribedBoards),
        sections = state.sections.withFavoriteSections(favoriteIds),
      )
    }
  }
}
