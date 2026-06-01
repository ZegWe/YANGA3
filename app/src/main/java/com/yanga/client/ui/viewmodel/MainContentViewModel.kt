package com.yanga.client.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yanga.client.data.DefaultNgaReadOnlyRepository
import com.yanga.client.data.NgaReadOnlyRepository
import com.yanga.client.data.LocalFavoriteBoard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainContentViewModel(
  private val repository: NgaReadOnlyRepository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(MainContentUiState.initialLoggedOut())
  val uiState: StateFlow<MainContentUiState> = _uiState.asStateFlow()
  private var loadingBoardId: String? = null
  private var loadingThreadId: String? = null

  fun onNavigationEvent(event: MainContentNavigationEvent) {
    when (event) {
      is MainContentNavigationEvent.OpenBoard -> openBoard(event.destination)
      is MainContentNavigationEvent.OpenThread -> openThread(event.destination)
      MainContentNavigationEvent.BackFromThread -> backFromThread()
      MainContentNavigationEvent.BackFromBoard -> backFromBoard()
    }
  }

  fun refresh(loginSession: LoginSessionUiState?, forumEndpoint: String? = null) {
    val session = loginSession?.toData()
    val endpoint = forumEndpoint ?: _uiState.value.profile.forumEndpoint

    if (repository is DefaultNgaReadOnlyRepository) {
      repository.setBaseUrl(endpoint)
    }

    _uiState.update { state ->
      if (session == null) {
        MainContentUiState.initialLoggedOut().copy(
          profile = state.profile.copy(session = LoadableUiState.LoginRequired, forumEndpoint = endpoint),
        )
      } else {
        MainContentUiState.initialLoggedIn(session).copy(
          profile = state.profile.copy(session = LoadableUiState.Content(session), forumEndpoint = endpoint),
        )
      }
    }

    viewModelScope.launch {
      launch {
        val homeResult = repository.loadHome()
        _uiState.update { state -> state.withHomeResult(homeResult) }
      }

      launch {
        val boardsResult = repository.loadBoards(session)
        _uiState.update { state -> state.withBoardsResult(boardsResult) }
      }

      if (session != null) {
        launch {
          val messagesResult = repository.loadMessages(session)
          _uiState.update { state -> state.withMessagesResult(messagesResult) }
        }

        launch {
          val profileResult = repository.loadProfile(session)
          _uiState.update { state -> state.withProfileResult(session, profileResult) }
        }
      }
    }
  }

  fun openBoard(board: BoardPreview) {
    openBoard(
      BoardDestination(
        id = board.id,
        name = board.name,
        iconUrl = board.iconUrl,
        category = board.category,
        isFavorite = board.isFavorite,
      ),
    )
  }

  fun openBoard(destination: BoardDestination) {
    val session = _uiState.value.profile.session.let { if (it is LoadableUiState.Content) it.value else null }
    val boardId = destination.id
    loadingBoardId = boardId
    _uiState.update {
      it.copy(
        activeBoard = BoardTopicListUiState(
          boardName = destination.name,
          fid = boardId,
          iconUrl = destination.iconUrl,
          category = destination.category,
          isFavorite = destination.isFavorite,
        ),
        activeThread = null,
      )
    }
    viewModelScope.launch {
      val currentBoard = _uiState.value.activeBoard
      val fidGroup =
        currentBoard?.selectedSubBoardIds
          ?.takeIf { it.isNotEmpty() }
          ?.joinToString(",")
      val result = repository.loadBoardTopics(session, boardId, fidGroup = fidGroup)
      _uiState.update { state ->
        if (loadingBoardId == boardId && state.activeBoard != null) {
          state.copy(
            activeBoard = state.activeBoard.copy(
              subBoards =
                result.fold(
                  onSuccess = { data ->
                    val options = data.subBoards.map { it.toOption() }
                    if (options.isEmpty()) LoadableUiState.Empty("No sub-boards") else LoadableUiState.Content(options)
                  },
                  onFailure = { it.toLoadableError() },
                ),
              topics = result.fold(
                onSuccess = { LoadableUiState.Content(it.topics.map { t -> t.toPreview() }) },
                onFailure = { it.toLoadableError() },
              ),
            ),
          )
        } else {
          state
        }
      }
    }
  }

  fun openThread(topic: TopicPreview) {
    openThread(ThreadDestination(id = topic.id, title = topic.title))
  }

  fun openThread(destination: ThreadDestination) {
    val session = _uiState.value.profile.session.let { if (it is LoadableUiState.Content) it.value else null }
    val threadId = destination.id
    val fallbackTitle = destination.title
    loadingThreadId = threadId
    _uiState.update { it.copy(activeThread = ThreadUiState(title = fallbackTitle)) }
    viewModelScope.launch {
      val result = repository.loadThread(session, threadId)
      _uiState.update { state ->
        if (loadingThreadId == threadId && state.activeThread != null) {
          state.copy(
            activeThread = result.fold(
              onSuccess = { data ->
                ThreadUiState(
                  title = data.subject.ifBlank { fallbackTitle },
                  page = data.page.toString(),
                  maxPage = data.maxPage.toString(),
                  replyCount = data.replyCount.toString(),
                  posts = LoadableUiState.Content(data.posts.map { p -> p.toPreview() }),
                )
              },
              onFailure = {
                ThreadUiState(title = fallbackTitle, posts = it.toLoadableError())
              },
            ),
          )
        } else {
          state
        }
      }
    }
  }

  fun closeDeepPage() {
    if (_uiState.value.activeThread != null) {
      backFromThread()
    } else if (_uiState.value.activeBoard != null) {
      backFromBoard()
    }
  }

  fun backFromThread() {
    loadingThreadId = null
    _uiState.update { it.copy(activeThread = null) }
  }

  fun backFromBoard() {
    loadingBoardId = null
    loadingThreadId = null
    _uiState.update { it.copy(activeBoard = null, activeThread = null) }
  }

  fun toggleBoardFavorite(board: BoardPreview) {
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

  fun toggleActiveBoardFavorite() {
    val board = _uiState.value.activeBoard ?: return
    toggleBoardFavorite(
      BoardPreview(
        id = board.fid,
        name = board.boardName,
        metadata = "fid: ${board.fid}",
        marker = board.boardName.take(1),
        iconUrl = board.iconUrl,
        category = board.category,
        isFavorite = board.isFavorite,
      ),
    )
  }

  private suspend fun applyLocalFavorites(boardId: String? = null, isFavorite: Boolean? = null) {
    val localFavorites = repository.listLocalFavoriteBoards().getOrDefault(emptyList())
    val localFavoriteIds = localFavorites.map { it.boardId }.toSet()
    val currentState = _uiState.value
    val subscribedFavorites =
      (currentState.boards.subscribedBoards as? LoadableUiState.Content)
        ?.value
        .orEmpty()
        .filter { it.isFavorite }
        .map { it.id }
        .toSet()
    val sectionFavorites =
      (currentState.boards.sections as? LoadableUiState.Content)
        ?.value
        .orEmpty()
        .flatMap { section -> section.groups.flatMap { group -> group.boards } }
        .filter { it.isFavorite }
        .map { it.id }
        .toSet()
    val activeFavorite =
      currentState.activeBoard
        ?.takeIf { it.isFavorite }
        ?.fid
        ?.let { setOf(it) }
        ?: emptySet()

    val favoriteIds = (localFavoriteIds + subscribedFavorites + sectionFavorites + activeFavorite).toMutableSet()
    if (boardId != null && isFavorite != null) {
      if (isFavorite) favoriteIds.add(boardId) else favoriteIds.remove(boardId)
    }

    _uiState.update { state ->
      val sectionList = (state.boards.sections as? LoadableUiState.Content)?.value.orEmpty()
      val existingSubscribed = (state.boards.subscribedBoards as? LoadableUiState.Content)?.value.orEmpty()
      val existingSubscribedById = existingSubscribed.associateBy { it.id }
      val sectionBoardById =
        sectionList
          .flatMap { section -> section.groups.flatMap { group -> group.boards } }
          .associateBy { it.id }
      val localById = localFavorites.associateBy { it.boardId }

      val orderedFavoriteIds =
        buildList {
          existingSubscribed.forEach { board -> if (favoriteIds.contains(board.id)) add(board.id) }
          sectionList
            .flatMap { section -> section.groups.flatMap { group -> group.boards } }
            .forEach { board -> if (favoriteIds.contains(board.id) && board.id !in this) add(board.id) }
          localFavorites.forEach { local -> if (favoriteIds.contains(local.boardId) && local.boardId !in this) add(local.boardId) }
        }

      val subscribedBoards =
        orderedFavoriteIds.mapNotNull { id ->
          existingSubscribedById[id] ?: sectionBoardById[id] ?: localById[id]?.toBoardPreview()
        }.map { it.copy(isFavorite = true) }

      state.copy(
        home = state.home.copy(boards = state.home.boards.withFavorites(favoriteIds)),
        boards = state.boards.copy(
          subscribedBoards = LoadableUiState.Content(subscribedBoards),
          sections = state.boards.sections.withFavoriteSections(favoriteIds),
        ),
        activeBoard = state.activeBoard?.copy(isFavorite = favoriteIds.contains(state.activeBoard.fid)),
      )
    }
  }
}
