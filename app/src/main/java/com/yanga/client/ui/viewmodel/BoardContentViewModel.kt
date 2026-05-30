package com.yanga.client.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.NgaReadOnlyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BoardContentViewModel(
  private val repository: NgaReadOnlyRepository,
) : ViewModel() {
  private val _state = MutableStateFlow<BoardTopicListUiState?>(null)
  val state: StateFlow<BoardTopicListUiState?> = _state.asStateFlow()
  private var loadingBoardId: String? = null

  fun openBoard(session: LoginSessionData?, board: BoardPreview) {
    openBoard(
      session = session,
      destination = BoardDestination(
        id = board.id,
        name = board.name,
        iconUrl = board.iconUrl,
        category = board.category,
        isFavorite = board.isFavorite,
      ),
    )
  }

  fun openBoard(session: LoginSessionData?, destination: BoardDestination) {
    val boardId = destination.id
    val cached = _state.value
    if (cached?.fid == boardId && cached.topics is LoadableUiState.Content) {
      loadingBoardId = boardId
      _state.value =
        cached.copy(
          boardName = destination.name,
          iconUrl = destination.iconUrl,
          category = destination.category,
          isFavorite = destination.isFavorite,
        )
      return
    }
    loadingBoardId = boardId
    _state.value =
      BoardTopicListUiState(
        boardName = destination.name,
        fid = boardId,
        iconUrl = destination.iconUrl,
        category = destination.category,
        isFavorite = destination.isFavorite,
      )
    viewModelScope.launch {
      val result = repository.loadBoardTopics(session, boardId)
      _state.update { current ->
        if (loadingBoardId == boardId && current != null) {
          current.copy(
            topics = result.fold(
              onSuccess = { LoadableUiState.Content(it.topics.map { t -> t.toPreview() }) },
              onFailure = { it.toLoadableError() },
            ),
          )
        } else {
          current
        }
      }
    }
  }

  fun setFavorite(isFavorite: Boolean) {
    _state.update { it?.copy(isFavorite = isFavorite) }
  }

  fun backFromBoard() {
    loadingBoardId = null
    _state.value = null
  }
}

