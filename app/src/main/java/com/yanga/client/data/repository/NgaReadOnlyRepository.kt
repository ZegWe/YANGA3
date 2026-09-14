package com.yanga.client.data

import android.util.Log
import com.yanga.client.api.NgaAccountParser
import com.yanga.client.api.NgaApi
import com.yanga.client.api.NgaApiException
import com.yanga.client.api.HttpUrlConnectionNgaTransport
import com.yanga.client.api.NgaBoardCategoryParser
import com.yanga.client.api.NgaBoardSummary
import com.yanga.client.api.NgaHttpResponse
import com.yanga.client.api.NgaHttpTransport
import com.yanga.client.api.NgaMessageParser
import com.yanga.client.api.NgaRequest
import com.yanga.client.api.NgaSession
import com.yanga.client.api.NgaThreadParser
import com.yanga.client.api.NgaThreadPost
import com.yanga.client.api.NgaThreadRead
import com.yanga.client.api.NgaSubBoardFilterParser
import com.yanga.client.api.NgaTopicListParser
import com.yanga.client.api.NgaTopicList
import com.yanga.client.data.SubBoardVisibilityChange
import com.yanga.client.data.boards.BoardListIncrementalMerger
import com.yanga.client.data.boards.BoardSectionDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class LoginRequiredException : IllegalStateException("Login is required for this read operation")

interface NgaReadOnlyRepository {
  suspend fun loadNotifications(session: LoginSessionData?): Result<List<com.yanga.client.api.NgaNotificationSummary>> = Result.failure(UnsupportedOperationException())

  suspend fun loadPersonalTopics(session: LoginSessionData?, kind: com.yanga.client.api.NgaPersonalTopicKind, page: Int): Result<com.yanga.client.api.NgaPersonalTopicPage> = Result.failure(UnsupportedOperationException())

  suspend fun saveSignature(session: LoginSessionData?, signature: String): Result<Unit> = Result.failure(UnsupportedOperationException())

  suspend fun loadUser(session: LoginSessionData?, uid: String): Result<com.yanga.client.api.NgaUserProfile> = Result.failure(UnsupportedOperationException())

  suspend fun checkIn(session: LoginSessionData?): Result<String> = Result.failure(UnsupportedOperationException())

  suspend fun loadHome(): Result<HomeReadData>

  suspend fun loadBoards(session: LoginSessionData?): Result<BoardsReadData>

  suspend fun loadMessages(session: LoginSessionData?): Result<MessagesReadData>

  suspend fun loadProfile(session: LoginSessionData?): Result<ProfileReadData>

  suspend fun loadBoardTopics(
    session: LoginSessionData?,
    fid: String,
    page: Int = 1,
    fidGroup: String? = null,
    recommend: Boolean = false,
  ): Result<NgaTopicList>

  suspend fun searchBoards(session: LoginSessionData?, query: String): Result<List<NgaBoardSummary>> =
    Result.failure(UnsupportedOperationException("searchBoards is not implemented"))

  suspend fun searchTopics(
    session: LoginSessionData?,
    query: String,
    fid: String? = null,
    page: Int = 1,
    searchContent: Boolean = false,
    recommend: Boolean = false,
  ): Result<NgaTopicList> =
    Result.failure(UnsupportedOperationException("searchTopics is not implemented"))

  suspend fun loadThread(session: LoginSessionData?, tid: String, page: Int = 1): Result<NgaThreadRead>

  suspend fun loadThreadByAuthor(session: LoginSessionData?, tid: String, page: Int, authorId: String): Result<NgaThreadRead> =
    Result.failure(UnsupportedOperationException("暂不支持按作者筛选"))

  suspend fun reactToPost(session: LoginSessionData?, tid: String, pid: String, support: Boolean): Result<Unit> =
    Result.failure(UnsupportedOperationException("暂不支持赞踩"))

  suspend fun loadThreadPost(session: LoginSessionData?, pid: String): Result<NgaThreadPost>

  suspend fun submitPoll(session: LoginSessionData?, poll: com.yanga.client.api.NgaPoll, ids: List<Int>): Result<Unit> =
    Result.failure(UnsupportedOperationException("投票暂不可用"))

  suspend fun listLocalFavoriteBoards(): Result<List<LocalFavoriteBoard>>

  suspend fun addLocalFavoriteBoard(board: LocalFavoriteBoard): Result<Unit>

  suspend fun removeLocalFavoriteBoard(boardId: String): Result<Unit>

  suspend fun refreshIncrementalBoardDirectoryIfDue(): Boolean

  suspend fun loadBlockedSubBoards(session: LoginSessionData?, parentFid: String): Result<Set<String>>

  suspend fun applySubBoardVisibilityChanges(
    session: LoginSessionData?,
    parentFid: String,
    changes: List<SubBoardVisibilityChange>,
  ): Result<Unit>
}

class DefaultNgaReadOnlyRepository(
  private val transport: NgaHttpTransport = HttpUrlConnectionNgaTransport(),
  private val userAgent: String = "Yanga Android",
  private var baseUrl: String = com.yanga.client.api.NgaDomains.BBS_NGA_CN,
  private val favoriteBoardsStore: FavoriteBoardsStore? = null,
  private val boardSectionDirectory: BoardSectionDirectory? = null,
) : NgaReadOnlyRepository {
  private val logTag = "YangaSubBoardRpc"
  fun setBaseUrl(url: String) {
    baseUrl = url
  }

  fun currentBaseUrl(): String = baseUrl

  override suspend fun loadNotifications(session: LoginSessionData?): Result<List<com.yanga.client.api.NgaNotificationSummary>> = withContext(Dispatchers.IO) {
    val login = session.requireLogin() ?: return@withContext Result.failure(LoginRequiredException())
    execute(api(login).notifications(), NgaAccountParser::parseNotifications)
  }

  override suspend fun loadPersonalTopics(session: LoginSessionData?, kind: com.yanga.client.api.NgaPersonalTopicKind, page: Int): Result<com.yanga.client.api.NgaPersonalTopicPage> = withContext(Dispatchers.IO) {
    val login = session.requireLogin() ?: return@withContext Result.failure(LoginRequiredException())
    val uid = login.uid.toIntOrNull() ?: return@withContext Result.failure(IllegalArgumentException("用户 ID 无效"))
    if (page < 1) return@withContext Result.failure(IllegalArgumentException("页码无效"))
    val favorites = kind == com.yanga.client.api.NgaPersonalTopicKind.Favorites
    execute(api(login).topicList(page = page, authorId = if (favorites) null else uid,
      searchPost = if (kind == com.yanga.client.api.NgaPersonalTopicKind.Replies) 1 else null,
      favor = if (favorites) 1 else null), com.yanga.client.api.NgaPersonalTopicParser::parse)
  }

  override suspend fun saveSignature(session: LoginSessionData?, signature: String): Result<Unit> = withContext(Dispatchers.IO) {
    val login = session.requireLogin() ?: return@withContext Result.failure(LoginRequiredException())
    execute(api(login).signature(login.uid, signature)) { raw ->
      com.yanga.client.api.NgaSignatureParser.requireSuccess(raw)
    }
  }

  override suspend fun loadUser(session: LoginSessionData?, uid: String): Result<com.yanga.client.api.NgaUserProfile> = withContext(Dispatchers.IO) {
    if (uid.toLongOrNull()?.let { it > 0 } != true) return@withContext Result.failure(IllegalArgumentException("用户 ID 无效"))
    execute(api(session).profile(mapOf("uid" to uid)), com.yanga.client.api.NgaUserProfileParser::parse)
  }

  override suspend fun checkIn(session: LoginSessionData?): Result<String> = withContext(Dispatchers.IO) {
    val login = session.requireLogin() ?: return@withContext Result.failure(LoginRequiredException())
    execute(api(login).checkIn(), com.yanga.client.api.NgaCheckInParser::parse)
  }

  override suspend fun loadHome(): Result<HomeReadData> = withContext(Dispatchers.IO) {
    val api = api()
    val categoriesResult = execute(api.remoteBoardCategories(), NgaBoardCategoryParser::parse)
    if (categoriesResult.isFailure) return@withContext Result.failure(categoriesResult.exceptionOrNull()!!)

    val allCategories = categoriesResult.getOrThrow()
    val favoriteBoards = allCategories.flatMap { it.boards }.take(6)

    val topics = execute(api.topicList(page = 1, recommend = true)) { raw ->
      NgaTopicListParser.parse(raw).topics
    }
    if (topics.isFailure) return@withContext Result.failure(topics.exceptionOrNull()!!)

    Result.success(
      HomeReadData(
        boards = favoriteBoards,
        activeTopics = topics.getOrThrow(),
      ),
    )
  }

  override suspend fun loadBoards(session: LoginSessionData?): Result<BoardsReadData> = withContext(Dispatchers.IO) {
    val cachedSections = boardSectionDirectory?.loadSections().orEmpty()
    val fetchResult = fetchRemoteSections(session)
    val fetchedSections = fetchResult.getOrNull().orEmpty().filterLegacySubscribedSection()
    val remoteSections =
      if (fetchedSections.isNotEmpty()) {
        boardSectionDirectory?.saveSections(fetchedSections)
        fetchedSections
      } else {
        cachedSections
      }

    if (remoteSections.isEmpty()) {
      return@withContext Result.failure(
        fetchResult.exceptionOrNull() ?: IllegalStateException("Board directory is empty"),
      )
    }

    val subscribedBoards = loadSubscribedBoards(session, remoteSections)
    val localFavorites = favoriteBoardsStore?.list().orEmpty().map { it.toBoardSummary() }
    val resolvedSubscribedBoards = (localFavorites + subscribedBoards).distinctBy { it.boardId }

    Result.success(
      BoardsReadData(
        subscribedBoards = resolvedSubscribedBoards,
        remoteSections = remoteSections,
      ),
    )
  }

  override suspend fun refreshIncrementalBoardDirectoryIfDue(): Boolean = withContext(Dispatchers.IO) {
    val directory = boardSectionDirectory ?: return@withContext false
    if (System.currentTimeMillis() - directory.lastIncrementalRequestAt() < INCREMENTAL_REQUEST_INTERVAL_MS) {
      return@withContext false
    }

    val categoryRaw =
      execute(api().remoteBoardCategories()) { raw -> raw }
        .getOrNull()
        .orEmpty()
    if (categoryRaw.isBlank()) return@withContext false

    val merged = BoardListIncrementalMerger.merge(directory.loadSections(), categoryRaw) ?: return@withContext false
    directory.saveSections(merged)
    directory.markIncrementalRequested(System.currentTimeMillis())
    true
  }

  private suspend fun loadSubscribedBoards(
    session: LoginSessionData?,
    remoteSections: List<com.yanga.client.api.NgaBoardSection>,
  ): List<com.yanga.client.api.NgaBoardSummary> {
    val api = api(session)
    val subscribedBoards =
      if (session.requireLogin() != null) {
        execute(api.subscribedBoards(), NgaBoardCategoryParser::parseBoards).getOrDefault(emptyList())
      } else {
        emptyList()
      }
    val legacySubscribedSection =
      remoteSections.find {
        it.name.contains("收藏") || it.name.contains("订阅")
      }
    return subscribedBoards.ifEmpty { legacySubscribedSection?.groups.orEmpty().flatMap { it.boards } }
  }

  private suspend fun fetchRemoteSections(session: LoginSessionData?): Result<List<com.yanga.client.api.NgaBoardSection>> {
    val api = api(session)
    val directoryResult = execute(api.fullForumDirectory(), NgaBoardCategoryParser::parseSections)
    if (directoryResult.isSuccess) {
      val sections = directoryResult.getOrThrow().filterLegacySubscribedSection()
      if (sections.isNotEmpty()) return Result.success(sections)
    }

    val categoryResult = execute(api.remoteBoardCategories(), NgaBoardCategoryParser::parseSections)
    if (categoryResult.isSuccess) {
      return Result.success(categoryResult.getOrThrow().filterLegacySubscribedSection())
    }

    return directoryResult.takeIf { it.isFailure } ?: categoryResult
  }

  private fun List<com.yanga.client.api.NgaBoardSection>.filterLegacySubscribedSection(): List<com.yanga.client.api.NgaBoardSection> {
    val legacySubscribedSection =
      find {
        it.name.contains("收藏") || it.name.contains("订阅")
      }
    return filter { it != legacySubscribedSection }
  }

  override suspend fun loadMessages(session: LoginSessionData?): Result<MessagesReadData> = withContext(Dispatchers.IO) {
    val loginSession = session.requireLogin()
      ?: return@withContext Result.failure(LoginRequiredException())
    val messages = execute(api(loginSession).messageList(page = 1), NgaMessageParser::parseList)
    if (messages.isFailure) return@withContext Result.failure(messages.exceptionOrNull()!!)

    Result.success(MessagesReadData(messages = messages.getOrThrow()))
  }

  override suspend fun loadProfile(session: LoginSessionData?): Result<ProfileReadData> = withContext(Dispatchers.IO) {
    val loginSession = session.requireLogin()
      ?: return@withContext Result.failure(LoginRequiredException())
    val api = api(loginSession)
    val notifications = execute(api.notifications(), NgaAccountParser::parseNotifications)
    if (notifications.isFailure) return@withContext Result.failure(notifications.exceptionOrNull()!!)

    val profile =
      execute(
        api.profile(mapOf("uid" to loginSession.uid)),
      ) { raw -> NgaAccountParser.parseProfile(raw, loginSession.uid) }
    if (profile.isFailure) return@withContext Result.failure(profile.exceptionOrNull()!!)

    val profileData = profile.getOrThrow()

    Result.success(
      ProfileReadData(
        counters = profileData.counters,
        notifications = notifications.getOrThrow(),
        avatarUrl = profileData.avatarUrl,
      ),
    )
  }

  override suspend fun loadBoardTopics(
    session: LoginSessionData?,
    fid: String,
    page: Int,
    fidGroup: String?,
    recommend: Boolean,
  ): Result<NgaTopicList> = withContext(Dispatchers.IO) {
    val stid = fid.removePrefix("t").toIntOrNull().takeIf { fid.startsWith("t") }
    val numericFid = if (fid.startsWith("t")) null else fid.toIntOrNull()
    logDebug("loadBoardTopics fid=$fid stid=$stid page=$page fidGroup=$fidGroup recommend=$recommend hasCookie=${!session?.cookie.isNullOrBlank()}")
    execute(
      api(session).topicList(fid = numericFid, stid = stid, page = page, fidGroup = fidGroup, recommend = recommend),
      NgaTopicListParser::parse,
    )
  }

  override suspend fun searchBoards(session: LoginSessionData?, query: String): Result<List<NgaBoardSummary>> =
    withContext(Dispatchers.IO) {
      execute(api(session).boardSearch(query), NgaBoardCategoryParser::parseBoards)
    }

  override suspend fun searchTopics(
    session: LoginSessionData?,
    query: String,
    fid: String?,
    page: Int,
    searchContent: Boolean,
    recommend: Boolean,
  ): Result<NgaTopicList> = withContext(Dispatchers.IO) {
    val stid = fid?.removePrefix("t")?.toIntOrNull().takeIf { fid?.startsWith("t") == true }
    val fidRaw = fid.takeUnless { it.isNullOrBlank() || it.startsWith("t") }
    execute(
      api(session).topicList(
        page = page,
        stid = stid,
        fidRaw = fidRaw,
        key = query,
        content = if (searchContent) 1 else null,
        recommend = recommend,
      ),
      NgaTopicListParser::parse,
    )
  }

  override suspend fun loadThread(session: LoginSessionData?, tid: String, page: Int): Result<NgaThreadRead> = withContext(Dispatchers.IO) {
    execute(api(session).articleRead(tid = tid.toIntOrNull(), page = page), NgaThreadParser::parseRead)
  }

  override suspend fun loadThreadByAuthor(session: LoginSessionData?, tid: String, page: Int, authorId: String): Result<NgaThreadRead> = withContext(Dispatchers.IO) {
    runCatching { require(authorId.toIntOrNull()?.let { it > 0 } == true) { "匿名作者暂不支持跨页筛选" } }.fold(
      onSuccess = { execute(api(session).articleRead(tid = tid.toIntOrNull(), page = page, authorId = authorId.toInt()), NgaThreadParser::parseRead) },
      onFailure = { Result.failure(it) },
    )
  }

  override suspend fun reactToPost(session: LoginSessionData?, tid: String, pid: String, support: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
    if (session == null || session.cookie.isBlank()) return@withContext Result.failure(IllegalStateException("请先登录后再赞踩"))
    val threadId = tid.toIntOrNull() ?: return@withContext Result.failure(IllegalArgumentException("帖子编号无效"))
    val postId = pid.toIntOrNull() ?: return@withContext Result.failure(IllegalArgumentException("楼层编号无效"))
    execute(api(session).like(threadId, postId, support), com.yanga.client.api.NgaReactionParser::requireSuccess)
  }

  override suspend fun submitPoll(session: LoginSessionData?, poll: com.yanga.client.api.NgaPoll, ids: List<Int>): Result<Unit> =
    withContext(Dispatchers.IO) {
      val login = session.requireLogin() ?: return@withContext Result.failure(LoginRequiredException())
      poll.validationError(ids)?.let { return@withContext Result.failure(IllegalArgumentException(it)) }
      val tid = poll.tid.toIntOrNull()?.takeIf { it > 0 }
        ?: return@withContext Result.failure(IllegalArgumentException("投票主题无效"))
      execute(api(login).vote(tid, ids), com.yanga.client.api.NgaPollParser::requireSuccessfulSubmission)
    }

  override suspend fun loadThreadPost(session: LoginSessionData?, pid: String): Result<NgaThreadPost> = withContext(Dispatchers.IO) {
    execute(api(session).articleRead(pid = pid.toIntOrNull())) { raw ->
      val thread = NgaThreadParser.parseRead(raw)
      thread.posts.firstOrNull { it.pid == pid }
        ?: thread.posts.firstOrNull()
        ?: throw IllegalStateException("Post $pid not found")
    }
  }

  override suspend fun loadBlockedSubBoards(session: LoginSessionData?, parentFid: String): Result<Set<String>> =
    withContext(Dispatchers.IO) {
      val loginSession = session.requireLogin() ?: return@withContext Result.success(emptySet())
      val result = execute(api(loginSession).subBoardFilterGet(parentFid), NgaSubBoardFilterParser::parseBlockedIds)
      logDebug("subBoardFilterGet fid=$parentFid result=${result.getOrNull()} error=${result.exceptionOrNull()?.message}")
      result
    }

  override suspend fun applySubBoardVisibilityChanges(
    session: LoginSessionData?,
    parentFid: String,
    changes: List<SubBoardVisibilityChange>,
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val loginSession = session.requireLogin() ?: return@withContext Result.failure(LoginRequiredException())
    if (changes.isEmpty()) return@withContext Result.success(Unit)
    runCatching {
      val ngaApi = api(loginSession)
      changes.forEach { change ->
        val blockId = change.board.subscribeId ?: return@forEach
        logDebug("subBoardFilterSet fid=$parentFid blockId=$blockId visible=${change.visible}")
        val responseText =
          executeText(
            ngaApi.subBoardFilterSet(
              parentFid = parentFid,
              blockId = blockId,
              visible = change.visible,
            ),
          ).getOrThrow()
        logDebug("subBoardFilterSetResponse fid=$parentFid blockId=$blockId body=${responseText.take(200)}")
        if (!responseText.contains("成功")) {
          throw IllegalStateException("subBoardFilterSet failed for blockId=$blockId")
        }
      }
    }
  }

  override suspend fun listLocalFavoriteBoards(): Result<List<LocalFavoriteBoard>> = withContext(Dispatchers.IO) {
    runCatching { favoriteBoardsStore?.list().orEmpty() }
  }

  override suspend fun addLocalFavoriteBoard(board: LocalFavoriteBoard): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
      favoriteBoardsStore?.upsert(board)
      Unit
    }
  }

  override suspend fun removeLocalFavoriteBoard(boardId: String): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
      favoriteBoardsStore?.remove(boardId)
      Unit
    }
  }

  private fun api(session: LoginSessionData? = null): NgaApi =
    NgaApi(NgaSession(baseUrl = baseUrl, cookie = session?.cookie, userAgent = userAgent))

  private fun LoginSessionData?.requireLogin(): LoginSessionData? =
    this?.takeIf { it.cookie.isNotBlank() }

  private fun <T> execute(request: NgaRequest, parser: (String) -> T): Result<T> =
    runCatching {
      if (request.url.contains("thread.php") || request.url.contains("nuke.php")) {
        logDebug("request ${request.method} ${requestDebugUrl(request)}")
      }
      val response = transport.execute(request).getOrThrow()
      if (!response.isSuccessful) {
        logError("http failed code=${response.code} url=${requestDebugUrl(request)}")
        throw response.toException(request)
      }
      parser(response.text)
    }

  private fun executeVoid(request: NgaRequest): Result<Unit> =
    runCatching {
      val response = transport.execute(request).getOrThrow()
      if (!response.isSuccessful) {
        throw response.toException(request)
      }
    }

  private fun executeText(request: NgaRequest): Result<String> =
    runCatching {
      if (request.url.contains("thread.php") || request.url.contains("nuke.php")) {
        logDebug("request ${request.method} ${requestDebugUrl(request)}")
      }
      val response = transport.execute(request).getOrThrow()
      if (!response.isSuccessful) {
        logError("http failed code=${response.code} url=${requestDebugUrl(request)}")
        throw response.toException(request)
      }
      response.text
    }

  private fun NgaHttpResponse.toException(request: NgaRequest): NgaApiException =
    NgaApiException("NGA request failed with HTTP $code for ${request.url}")

  private companion object {
    val INCREMENTAL_REQUEST_INTERVAL_MS = TimeUnit.DAYS.toMillis(1)
  }

  private fun requestDebugUrl(request: NgaRequest): String {
    if (request.query.isEmpty()) return request.url
    val query =
      request.query.entries.joinToString("&") { (key, value) ->
        if (value.isEmpty()) key else "$key=$value"
      }
    return "${request.url}?$query"
  }

  private fun logDebug(message: String) {
    runCatching { Log.d(logTag, message) }
  }

  private fun logError(message: String) {
    runCatching { Log.e(logTag, message) }
  }
}
