package com.yanga.client.data.boards

import android.content.Context
import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.LocalFavoriteBoard
import com.yanga.client.data.NgaReadOnlyRepository
import com.yanga.client.data.image.ImageCacheManager
import com.yanga.client.ui.BoardPreview
import com.yanga.client.ui.BoardsUiState
import com.yanga.client.ui.LoadableUiState
import com.yanga.client.ui.collectBoardIconUrls
import com.yanga.client.ui.markFavoriteBoards
import com.yanga.client.ui.toBoardPreview
import com.yanga.client.ui.toBoardsUiState
import com.yanga.client.ui.toLoadableError
import com.yanga.client.ui.toPreview
import com.yanga.client.ui.withFavoriteSections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BoardsCatalog(
  private val repository: NgaReadOnlyRepository,
  private val imageCacheManager: ImageCacheManager,
  private val appContext: Context,
  private val boardSectionDirectory: BoardSectionDirectory?,
  private val scope: CoroutineScope,
) {
  private val loadMutex = Mutex()
  private val _state = MutableStateFlow(BoardsUiState())
  val state: StateFlow<BoardsUiState> = _state.asStateFlow()

  private var loadedSessionKey: String? = null
  private var iconsWarmedForSessionKey: String? = null

  fun preloadAtStartup() {
    localBoardsUiState()?.let { _state.value = it }
    scope.launch { reloadInternal(session = null, force = true) }
  }

  fun reload(session: LoginSessionData?, force: Boolean = false) {
    scope.launch { reloadInternal(session, force) }
  }

  fun onBoardOpened() {
    scope.launch {
      if (!repository.refreshIncrementalBoardDirectoryIfDue()) return@launch
      val sections = boardSectionDirectory?.loadSections().orEmpty()
      if (sections.isEmpty()) return@launch
      val favoriteIds = currentFavoriteIds()
      _state.update { current ->
        current.copy(
          sections =
            LoadableUiState.Content(
              sections.map { it.toPreview() }.markFavoriteBoards(favoriteIds),
            ),
        )
      }
    }
  }

  fun toggleLocalFavorite(board: BoardPreview) {
    scope.launch {
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

  private suspend fun currentFavoriteIds(): Set<String> {
    val localFavoriteIds = repository.listLocalFavoriteBoards().getOrDefault(emptyList()).map { it.boardId }.toSet()
    val currentState = _state.value
    val subscribedFavorites =
      (currentState.subscribedBoards as? LoadableUiState.Content)
        ?.value
        .orEmpty()
        .map { it.id }
        .toSet()
    return localFavoriteIds + subscribedFavorites
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
          existingSubscribed.forEach { board -> if (favoriteIds.contains(board.id)) add(board.id) }
          sectionList
            .flatMap { section -> section.groups.flatMap { group -> group.boards } }
            .forEach { board -> if (favoriteIds.contains(board.id) && board.id !in this) add(board.id) }
          localFavorites.forEach { local ->
            if (favoriteIds.contains(local.boardId) && local.boardId !in this) add(local.boardId)
          }
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

  private suspend fun reloadInternal(session: LoginSessionData?, force: Boolean) {
    val sessionKey = sessionKey(session)
    if (!force && sessionKey == loadedSessionKey && _state.value.sections is LoadableUiState.Content) {
      return
    }

    loadMutex.withLock {
      if (!force && sessionKey == loadedSessionKey && _state.value.sections is LoadableUiState.Content) {
        return
      }

      val hasLocalSections = boardSectionDirectory?.loadSections().orEmpty().isNotEmpty()
      _state.update { current ->
        current.copy(
          subscribedBoards =
            if (session != null && current.subscribedBoards !is LoadableUiState.Content) {
              LoadableUiState.Loading
            } else {
              current.subscribedBoards
            },
          sections =
            if (!hasLocalSections && current.sections !is LoadableUiState.Content) {
              LoadableUiState.Loading
            } else {
              current.sections
            },
        )
      }

      val result = repository.loadBoards(session)
      val uiState =
        result.fold(
          onSuccess = { data -> data.toBoardsUiState() },
          onFailure = { error ->
            val current = _state.value
            BoardsUiState(
              subscribedBoards =
                if (session != null) {
                  error.toLoadableError()
                } else {
                  current.subscribedBoards
                },
              sections =
                if (current.sections is LoadableUiState.Content) {
                  current.sections
                } else {
                  error.toLoadableError()
                },
            )
          },
        )
      _state.value = uiState
      loadedSessionKey = sessionKey
      applyLocalFavorites()

      if (result.isSuccess) {
        warmDiskCachedIconsIfNeeded(sessionKey, uiState)
      } else {
        iconsWarmedForSessionKey = null
      }
    }
  }

  fun applyEndpoint(baseUrl: String) {
    boardSectionDirectory?.clear()
    loadedSessionKey = null
    iconsWarmedForSessionKey = null
  }

  private fun localBoardsUiState(): BoardsUiState? {
    val sections = boardSectionDirectory?.loadSections().orEmpty()
    if (sections.isEmpty()) return null
    return BoardsUiState(
      subscribedBoards = LoadableUiState.Content(emptyList()),
      sections = LoadableUiState.Content(sections.map { it.toPreview() }),
    )
  }

  private suspend fun warmDiskCachedIconsIfNeeded(sessionKey: String, boardsState: BoardsUiState) {
    if (sessionKey == iconsWarmedForSessionKey) return
    val iconUrls = collectBoardIconUrls(boardsState)
    if (iconUrls.isNotEmpty()) {
      imageCacheManager.warmDiskCachedIcons(appContext, iconUrls)
    }
    iconsWarmedForSessionKey = sessionKey
  }

  private fun sessionKey(session: LoginSessionData?): String {
    val uid = session?.uid.orEmpty().ifBlank { "guest" }
    return uid
  }
}
