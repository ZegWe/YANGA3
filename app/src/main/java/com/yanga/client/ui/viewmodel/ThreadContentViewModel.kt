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

class ThreadContentViewModel(
  private val repository: NgaReadOnlyRepository,
) : ViewModel() {
  private var entryInitialized = false
  private var entrySession: LoginSessionData? = null

  // Returning to a retained entry must not reopen its original page or reset its filters.
  fun ensureThreadOpened(session: LoginSessionData?, destination: ThreadDestination) {
    if (entryInitialized && entrySession == session) return
    entryInitialized = true
    entrySession = session
    openThread(session, destination)
  }

  private val _state = MutableStateFlow<ThreadUiState?>(null)
  val state: StateFlow<ThreadUiState?> = _state.asStateFlow()
  private var loadingThreadId: String? = null
  private var loadingPage: Int? = null
  private var activeThreadId: String? = null
  private var activeDestination: ThreadDestination? = null
  private var filteredAuthorId: String? = null
  private var filteredAuthorName: String? = null
  private var requestGeneration = 0
  private var targetScrollRequestId = 0
  private val pageCache = mutableMapOf<Int, ThreadUiState>()

  fun matchesThread(threadId: String): Boolean = activeThreadId == threadId

  fun updatePollResults(poll: com.yanga.client.api.NgaPoll) {
    if (activeThreadId != poll.tid) return
    fun ThreadUiState.updated(): ThreadUiState {
      val loaded = posts as? LoadableUiState.Content ?: return this
      return copy(posts = LoadableUiState.Content(loaded.value.map { post ->
        if (post.poll?.tid == poll.tid) post.copy(poll = poll) else post
      }))
    }
    pageCache.keys.toList().forEach { page -> pageCache[page] = pageCache.getValue(page).updated() }
    _state.update { it?.updated()?.withCachedPosts() }
  }

  fun filterAuthor(session: LoginSessionData?, post: PostPreview?) {
    val destination = activeDestination ?: return
    if (post != null && post.authorId.toIntOrNull()?.let { it > 0 } != true) return
    filteredAuthorId = post?.authorId
    filteredAuthorName = post?.author
    requestGeneration++
    pageCache.clear()
    _state.value = null
    openThread(session, destination.copy(page = 1, targetPostId = null, targetFloorNumber = null))
  }

  fun updatePostScore(pid: String, score: Int) {
    fun ThreadUiState.updated(): ThreadUiState {
      val loaded = posts as? LoadableUiState.Content ?: return this
      return copy(posts = LoadableUiState.Content(loaded.value.map { if (it.pid == pid) it.copy(score = score) else it }))
    }
    pageCache.keys.toList().forEach { page -> pageCache[page] = pageCache.getValue(page).updated() }
    _state.update { it?.updated()?.withCachedPosts() }
  }

  fun openThread(session: LoginSessionData?, topic: TopicPreview) {
    openThread(session = session, destination = ThreadDestination(id = topic.id, title = topic.title))
  }

  fun openThread(session: LoginSessionData?, destination: ThreadDestination) {
    val threadId = destination.id
    val page = destination.page
    val fallbackTitle = destination.title
    val scrollRequestId =
      if (destination.hasScrollTarget()) {
        ++targetScrollRequestId
      } else {
        0
      }
    activeDestination = destination
    val cached = _state.value
    if (activeThreadId != threadId) {
      filteredAuthorId = null
      filteredAuthorName = null
      pageCache.clear()
    }
    if (
      activeThreadId == threadId &&
        cached != null &&
        cached.page.toIntOrNull() == page &&
        cached.posts is LoadableUiState.Content
    ) {
      loadingThreadId = threadId
      loadingPage = page
      _state.value =
        cached.copy(
          targetPostId = destination.targetPostId,
          targetFloorNumber = destination.targetFloorNumber,
          targetScrollRequestId = scrollRequestId,
        )
      return
    }
    pageCache[page]?.let { cachedPage ->
      loadingThreadId = null
      loadingPage = null
      activeThreadId = threadId
      _state.value =
        cachedPage
          .copy(
            targetPostId = destination.targetPostId,
            targetFloorNumber = destination.targetFloorNumber,
            targetScrollRequestId = scrollRequestId,
          )
          .withCachedPosts()
      return
    }
    loadingThreadId = threadId
    loadingPage = page
    activeThreadId = threadId
    _state.value =
      cached
        ?.copy(
          page = page.toString(),
          posts = LoadableUiState.Loading,
          targetPostId = destination.targetPostId,
          targetFloorNumber = destination.targetFloorNumber,
          targetScrollRequestId = scrollRequestId,
        )
        ?.withCachedPosts()
        ?: ThreadUiState(
          filteredAuthorId = filteredAuthorId,
          filteredAuthorName = filteredAuthorName,
          title = fallbackTitle,
          page = page.toString(),
          targetPostId = destination.targetPostId,
          targetFloorNumber = destination.targetFloorNumber,
          targetScrollRequestId = scrollRequestId,
        )
    val generation = ++requestGeneration
    val authorFilter = filteredAuthorId
    viewModelScope.launch {
      val result = if (authorFilter == null) repository.loadThread(session, threadId, page)
        else repository.loadThreadByAuthor(session, threadId, page, authorFilter)
      _state.update { current ->
        if (generation == requestGeneration && loadingThreadId == threadId && loadingPage == page && current != null) {
          result.fold(
            onSuccess = { data ->
              val loadedState =
                ThreadUiState(
                  filteredAuthorId = filteredAuthorId,
                  filteredAuthorName = filteredAuthorName,
                  title = data.subject.ifBlank { fallbackTitle },
                  page = data.page.toString(),
                  maxPage = data.maxPage.toString(),
                  replyCount = data.replyCount.toString(),
                  targetPostId = destination.targetPostId,
                  targetFloorNumber = destination.targetFloorNumber,
                  targetScrollRequestId = scrollRequestId,
                  posts = LoadableUiState.Content(data.posts.map { p -> p.toPreview() }),
                )
              pageCache[page] =
                loadedState.copy(
                  targetPostId = null,
                  targetFloorNumber = null,
                  targetScrollRequestId = 0,
                  cachedPostsByPage = emptyMap(),
                )
              loadedState.withCachedPosts()
            },
            onFailure = {
              ThreadUiState(
                filteredAuthorId = filteredAuthorId,
                filteredAuthorName = filteredAuthorName,
                title = fallbackTitle,
                page = page.toString(),
                maxPage = current.maxPage,
                replyCount = current.replyCount,
                targetPostId = destination.targetPostId,
                targetFloorNumber = destination.targetFloorNumber,
                targetScrollRequestId = scrollRequestId,
                cachedPostsByPage = cachedPostsByPage(),
                posts = it.toLoadableError(),
              )
            },
          )
        } else {
          current
        }
      }
    }
  }

  fun openPage(session: LoginSessionData?, page: Int) {
    val destination = activeDestination ?: return
    val targetPage = page.coerceAtLeast(1)
    openThread(session = session, destination = destination.copy(page = targetPage))
  }

  fun openFloor(session: LoginSessionData?, floor: Int) {
    val destination = activeDestination ?: return
    val targetFloor = floor.coerceAtLeast(0)
    if (filteredAuthorId != null) {
      filteredAuthorId = null
      filteredAuthorName = null
      pageCache.clear()
      _state.value = null
    }
    openThread(
      session = session,
      destination =
        destination.copy(
          page = targetFloor.floorPage(),
          targetPostId = null,
          targetFloorNumber = targetFloor,
        ),
    )
  }

  fun openPost(session: LoginSessionData?, postId: String) {
    val destination = activeDestination ?: return
    viewModelScope.launch {
      val result = repository.loadThreadPost(session, postId)
      result.onSuccess { post ->
        openThread(
          session = session,
          destination =
            destination.copy(
              id = post.tid.ifBlank { destination.id },
              page = post.lou.floorPage(),
              targetPostId = post.pid.ifBlank { postId },
              targetFloorNumber = post.lou,
            ),
        )
      }
    }
  }

  fun openPostOnPage(session: LoginSessionData?, postId: String, page: Int) {
    val destination = activeDestination ?: return
    openThread(
      session = session,
      destination =
        destination.copy(
          page = page.coerceAtLeast(1),
          targetPostId = postId,
          targetFloorNumber = null,
        ),
    )
  }

  fun backFromThread() {
    loadingThreadId = null
    loadingPage = null
    activeThreadId = null
    requestGeneration++
    activeDestination = null
    pageCache.clear()
    _state.value = null
  }

  private fun ThreadUiState.withCachedPosts(): ThreadUiState {
    val currentPage = page.toIntOrNull()
    val currentPosts = (posts as? LoadableUiState.Content)?.value
    val pages = cachedPostsByPage().toMutableMap()
    if (currentPage != null && currentPosts != null) {
      pages[currentPage] = currentPosts
    }
    return copy(cachedPostsByPage = pages, filteredAuthorId = filteredAuthorId, filteredAuthorName = filteredAuthorName)
  }

  private fun cachedPostsByPage(): Map<Int, List<PostPreview>> =
    pageCache.mapNotNull { (page, state) ->
      val posts = (state.posts as? LoadableUiState.Content)?.value ?: return@mapNotNull null
      page to posts
    }.toMap()
}

private fun Int.floorPage(): Int =
  (coerceAtLeast(0) / POSTS_PER_PAGE) + 1

private fun ThreadDestination.hasScrollTarget(): Boolean =
  targetPostId != null || targetFloorNumber != null

private const val POSTS_PER_PAGE = 20

