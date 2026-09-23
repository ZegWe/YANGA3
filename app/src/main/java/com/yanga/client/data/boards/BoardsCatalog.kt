package com.yanga.client.data.boards

import android.content.Context
import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.LocalFavoriteBoard
import com.yanga.client.data.FavoriteBoardsStore
import com.yanga.client.data.NgaReadOnlyRepository
import com.yanga.client.data.image.ImageCacheManager
import com.yanga.client.ui.BoardPreview
import com.yanga.client.ui.BoardsUiState
import com.yanga.client.ui.LoadableUiState
import com.yanga.client.ui.collectBoardIconUrls
import com.yanga.client.ui.markFavoriteBoards
import com.yanga.client.ui.toBoardPreview
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
  private val favoriteBoardsStore: FavoriteBoardsStore,
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
    _state.value = localBoardsUiState()
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
      applyLocalFavorites()
    }
  }

  private suspend fun currentFavoriteIds(): Set<String> =
    repository.listLocalFavoriteBoards().getOrDefault(emptyList()).map { it.boardId }.toSet()

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

  private suspend fun reloadInternal(session: LoginSessionData?, force: Boolean) {
    applyLocalFavorites()
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
          sections =
            if (!hasLocalSections && current.sections !is LoadableUiState.Content) {
              LoadableUiState.Loading
            } else {
              current.sections
            },
        )
      }

      val result = repository.loadBoards(session)
      _state.update { current ->
        result.fold(
          onSuccess = { data ->
            // Directory refreshes must never replace locally managed favorites.
            val favoriteIds = (current.subscribedBoards as? LoadableUiState.Content)
              ?.value.orEmpty().map { it.id }.toSet()
            current.copy(
              sections = LoadableUiState.Content(data.remoteSections.map { it.toPreview() }.markFavoriteBoards(favoriteIds)),
            )
          },
          onFailure = { error ->
            BoardsUiState(
              subscribedBoards = current.subscribedBoards,
              sections =
                if (current.sections is LoadableUiState.Content) {
                  current.sections
                } else {
                  error.toLoadableError()
                },
            )
          },
        )
      }
      val uiState = _state.value
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

  private fun localBoardsUiState(): BoardsUiState {
    val favorites = favoriteBoardsStore.list()
    val favoriteIds = favorites.map { it.boardId }.toSet()
    val sections = boardSectionDirectory?.loadSections().orEmpty()
    return BoardsUiState(
      subscribedBoards = LoadableUiState.Content(favorites.map { it.toBoardPreview() }),
      sections = if (sections.isEmpty()) LoadableUiState.Loading
        else LoadableUiState.Content(sections.map { it.toPreview() }.markFavoriteBoards(favoriteIds)),
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
