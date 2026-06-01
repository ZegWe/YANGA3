package com.yanga.client.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yanga.client.api.NgaSubBoard
import com.yanga.client.api.NgaTopicList
import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.NgaReadOnlyRepository
import com.yanga.client.data.SubBoardFilterStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BoardContentViewModel(
  private val repository: NgaReadOnlyRepository,
  private val subBoardFilterStore: SubBoardFilterStore? = null,
) : ViewModel() {
  private val logTag = "YangaSubBoardUi"
  private val _state = MutableStateFlow<BoardTopicListUiState?>(null)
  val state: StateFlow<BoardTopicListUiState?> = _state.asStateFlow()
  private var loadingBoardId: String? = null
  private var reloadJob: Job? = null
  private var reloadGeneration = 0
  private var activeSession: LoginSessionData? = null
  private var blockedSubscribeIds: Set<String> = emptySet()

  fun openBoard(session: LoginSessionData?, board: BoardPreview) {
    openBoard(
      session = session,
      destination =
        BoardDestination(
          id = board.id,
          name = board.name,
          iconUrl = board.iconUrl,
          category = board.category,
          isFavorite = board.isFavorite,
        ),
    )
  }

  fun openBoard(session: LoginSessionData?, destination: BoardDestination) {
    activeSession = session
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
      refreshSubBoardSelectionFromServer(boardId)
      return
    }

    loadingBoardId = boardId
    blockedSubscribeIds = emptySet()
    val initialSelection =
      if (session?.cookie.isNullOrBlank()) {
        subBoardFilterStore?.load(boardId).orEmpty()
      } else {
        emptySet()
      }
    _state.value =
      BoardTopicListUiState(
        boardName = destination.name,
        fid = boardId,
        iconUrl = destination.iconUrl,
        category = destination.category,
        isFavorite = destination.isFavorite,
        selectedSubBoardIds = initialSelection,
      )
    reloadTopics(page = 1, syncSelectionToServer = false)
  }

  fun selectAllSubBoards() {
    updateSelection(emptySet())
  }

  fun toggleSubBoard(optionId: String) {
    val current = _state.value ?: return
    val options = current.subBoardOptions() ?: return
    val next = SubBoardFilterLogic.toggleSelection(current.selectedSubBoardIds, optionId, options)
    updateSelection(next)
  }

  fun setSubBoardEnabled(optionId: String, enabled: Boolean) {
    val current = _state.value ?: return
    val options = current.subBoardOptions() ?: return
    val next =
      SubBoardFilterLogic.setSelectionEnabled(
        selectedIds = current.selectedSubBoardIds,
        optionId = optionId,
        enabled = enabled,
        options = options,
      )
    updateSelection(next)
  }

  fun applySubBoardSelection(selectedIds: Set<String>) {
    val current = _state.value ?: return
    val options = current.subBoardOptions() ?: return
    updateSelection(SubBoardFilterLogic.validateSelection(selectedIds, options))
  }

  fun setFavorite(isFavorite: Boolean) {
    _state.update { it?.copy(isFavorite = isFavorite) }
  }

  fun setTopicFilter(filter: BoardTopicFilter) {
    val current = _state.value ?: return
    if (current.selectedTopicFilter == filter) return
    _state.update { it?.copy(selectedTopicFilter = filter) }
    scheduleReload(syncSelectionToServer = false)
  }

  fun refresh() {
    reloadJob?.cancel()
    val hasContent = _state.value?.topics is LoadableUiState.Content
    reloadTopics(page = 1, syncSelectionToServer = false, preserveCurrentTopics = hasContent)
  }

  fun loadNextPage() {
    val boardId = loadingBoardId ?: return
    val current = _state.value ?: return
    if (!current.hasNextTopicPage || current.isLoadingNextTopicPage) return
    val currentTopics = (current.topics as? LoadableUiState.Content)?.value ?: return
    val page = current.currentTopicPage + 1
    val generation = reloadGeneration
    val selectedIds = current.selectedSubBoardIds
    val knownSubBoards = current.knownSubBoardModels()
    val fidGroup =
      if (activeSession?.cookie.isNullOrBlank()) {
        SubBoardFilterLogic.selectedFidGroup(selectedIds, knownSubBoards)
      } else {
        null
      }

    _state.update { state ->
      if (loadingBoardId != boardId || state == null || generation != reloadGeneration) {
        state
      } else {
        state.copy(isLoadingNextTopicPage = true)
      }
    }

    viewModelScope.launch {
      val result =
        repository.loadBoardTopics(
          session = activeSession,
          fid = boardId,
          page = page,
          fidGroup = fidGroup,
          recommend = current.selectedTopicFilter == BoardTopicFilter.Recommend,
        )

      _state.update { state ->
        if (loadingBoardId != boardId || state == null || generation != reloadGeneration) {
          return@update state
        }
        result.fold(
          onSuccess = { data ->
            val nextTopics = data.topics.map { it.toPreview() }
            state.copy(
              topics = LoadableUiState.Content(currentTopics + nextTopics),
              currentTopicPage = data.page,
              hasNextTopicPage = data.hasNextPage,
              isLoadingNextTopicPage = false,
            )
          },
          onFailure = {
            state.copy(isLoadingNextTopicPage = false)
          },
        )
      }
    }
  }

  fun backFromBoard() {
    reloadJob?.cancel()
    loadingBoardId = null
    reloadGeneration += 1
    blockedSubscribeIds = emptySet()
    _state.value = null
  }

  private fun updateSelection(selectedIds: Set<String>) {
    val current = _state.value ?: return
    if (current.selectedSubBoardIds == selectedIds) return
    Log.d(logTag, "updateSelection fid=${current.fid} selectedIds=$selectedIds")
    _state.update { it?.copy(selectedSubBoardIds = selectedIds) }
    subBoardFilterStore?.save(current.fid, selectedIds)
    scheduleReload(syncSelectionToServer = true)
  }

  private fun scheduleReload(syncSelectionToServer: Boolean) {
    reloadJob?.cancel()
    reloadJob =
      viewModelScope.launch {
        delay(RELOAD_DEBOUNCE_MS)
        reloadTopics(page = 1, syncSelectionToServer = syncSelectionToServer)
      }
  }

  private fun refreshSubBoardSelectionFromServer(boardId: String) {
    if (activeSession?.cookie.isNullOrBlank()) return
    viewModelScope.launch {
      val subBoardModels = _state.value?.knownSubBoardModels().orEmpty()
      if (subBoardModels.isEmpty()) return@launch
      applyBlockedSelection(boardId, subBoardModels, repository.loadBlockedSubBoards(activeSession, boardId).getOrNull().orEmpty())
    }
  }

  private fun applyBlockedSelection(
    boardId: String,
    subBoardModels: List<NgaSubBoard>,
    blocked: Set<String>,
  ) {
    blockedSubscribeIds = blocked
    val selectedIds = SubBoardFilterLogic.selectedIdsFromBlocked(subBoardModels, blocked)
    subBoardFilterStore?.save(boardId, selectedIds)
    _state.update { state ->
      if (loadingBoardId != boardId || state == null) return@update state
      val validatedSelection =
        SubBoardFilterLogic.validateSelection(selectedIds, subBoardModels.map { it.toOption() })
      state.copy(selectedSubBoardIds = validatedSelection)
    }
  }

  private fun reloadTopics(
    page: Int,
    syncSelectionToServer: Boolean,
    preserveCurrentTopics: Boolean = false,
  ) {
    val boardId = loadingBoardId ?: return
    val generation = ++reloadGeneration
    _state.update { current ->
      current?.copy(
        topics = if (preserveCurrentTopics) current.topics else LoadableUiState.Loading,
        isRefreshing = preserveCurrentTopics,
        currentTopicPage = 1,
        hasNextTopicPage = false,
        isLoadingNextTopicPage = false,
      )
    }
    viewModelScope.launch {
      val current = _state.value ?: return@launch
      var selectedIds = current.selectedSubBoardIds
      val knownSubBoards = current.knownSubBoardModels()
      val shouldLoadBlockedFromServer =
        !syncSelectionToServer && activeSession?.cookie?.isNotBlank() == true

      if (syncSelectionToServer && SubBoardFilterLogic.canSyncWithServer(knownSubBoards)) {
        val nextBlocked = SubBoardFilterLogic.blockedSubscribeIds(knownSubBoards, selectedIds)
        Log.d(logTag, "syncSelection fid=$boardId selectedIds=$selectedIds nextBlocked=$nextBlocked prevBlocked=$blockedSubscribeIds")
        val changes =
          SubBoardFilterLogic.visibilityChanges(
            subBoards = knownSubBoards,
            previousBlockedSubscribeIds = blockedSubscribeIds,
            nextBlockedSubscribeIds = nextBlocked,
          )
        if (changes.isNotEmpty()) {
          repository
            .applySubBoardVisibilityChanges(activeSession, boardId, changes)
            .onSuccess {
              val refreshedBlocked =
                repository.loadBlockedSubBoards(activeSession, boardId).getOrNull()
              blockedSubscribeIds = refreshedBlocked ?: nextBlocked
            }
        } else {
          blockedSubscribeIds = nextBlocked
        }
      }

      val result =
        coroutineScope {
          val blockedDeferred =
            if (shouldLoadBlockedFromServer) {
              async { repository.loadBlockedSubBoards(activeSession, boardId).getOrNull() }
            } else {
              null
            }
          val fidGroup =
            if (activeSession?.cookie.isNullOrBlank()) {
              SubBoardFilterLogic.selectedFidGroup(selectedIds, knownSubBoards)
            } else {
              null
            }
          val topicsResult = repository.loadBoardTopics(
            session = activeSession,
            fid = boardId,
            page = page,
            fidGroup = fidGroup,
            recommend = current.selectedTopicFilter == BoardTopicFilter.Recommend,
          )
          topicsResult to blockedDeferred?.await()
        }
      if (generation != reloadGeneration) return@launch

      result.first.fold(
        onSuccess = { data ->
          val subBoardModels = data.subBoards.ifEmpty { knownSubBoards }
          if (shouldLoadBlockedFromServer && subBoardModels.isNotEmpty()) {
            result.second?.let { blocked ->
              blockedSubscribeIds = blocked
              selectedIds = SubBoardFilterLogic.selectedIdsFromBlocked(subBoardModels, blocked)
              subBoardFilterStore?.save(boardId, selectedIds)
            }
          }
          publishTopicResult(
            boardId = boardId,
            generation = generation,
            data = data,
            subBoardModels = subBoardModels,
            selectedIds = selectedIds,
          )
        },
        onFailure = { error ->
          _state.update { state ->
            if (loadingBoardId != boardId || state == null || generation != reloadGeneration) return@update state
            state.copy(
              subBoards = error.toLoadableError(),
              topics = error.toLoadableError(),
              isRefreshing = false,
            )
          }
        },
      )
    }
  }

  private fun publishTopicResult(
    boardId: String,
    generation: Int,
    data: NgaTopicList,
    subBoardModels: List<NgaSubBoard>,
    selectedIds: Set<String>,
  ) {
    _state.update { state ->
      if (loadingBoardId != boardId || state == null || generation != reloadGeneration) return@update state
      val subBoardOptions = subBoardModels.map { it.toOption() }
      val validatedSelection =
        SubBoardFilterLogic.validateSelection(selectedIds, subBoardOptions)
      if (validatedSelection != state.selectedSubBoardIds) {
        subBoardFilterStore?.save(boardId, validatedSelection)
      }
      val topics = data.topics.map { it.toPreview() }
      state.copy(
        selectedSubBoardIds = validatedSelection,
        subBoards =
          if (subBoardOptions.isEmpty()) {
            LoadableUiState.Empty("No sub-boards")
          } else {
            LoadableUiState.Content(subBoardOptions)
          },
        topics =
          if (topics.isEmpty()) {
            LoadableUiState.Empty("No topics")
          } else {
            LoadableUiState.Content(topics)
          },
        isRefreshing = false,
        currentTopicPage = data.page,
        hasNextTopicPage = data.hasNextPage,
        isLoadingNextTopicPage = false,
      )
    }
  }

  private fun BoardTopicListUiState.subBoardOptions(): List<SubBoardOption>? =
    when (val boards = subBoards) {
      is LoadableUiState.Content -> boards.value
      LoadableUiState.Loading -> null
      is LoadableUiState.Empty -> emptyList()
      is LoadableUiState.Error -> null
      LoadableUiState.LoginRequired -> emptyList()
    }

  private fun BoardTopicListUiState.knownSubBoardModels(): List<NgaSubBoard> =
    when (val boards = subBoards) {
      is LoadableUiState.Content ->
        boards.value.map {
          NgaSubBoard(
            id = it.id,
            name = it.name,
            valueId = it.valueId,
            subscribeId = it.subscribeId,
          )
        }
      else -> emptyList()
    }

  private companion object {
    const val RELOAD_DEBOUNCE_MS = 300L
  }
}
