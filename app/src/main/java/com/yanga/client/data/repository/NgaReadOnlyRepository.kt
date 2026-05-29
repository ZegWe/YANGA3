package com.yanga.client.data

import com.yanga.client.api.NgaAccountParser
import com.yanga.client.api.NgaApi
import com.yanga.client.api.NgaApiException
import com.yanga.client.api.HttpUrlConnectionNgaTransport
import com.yanga.client.api.NgaBoardCategoryParser
import com.yanga.client.api.NgaHttpResponse
import com.yanga.client.api.NgaHttpTransport
import com.yanga.client.api.NgaMessageParser
import com.yanga.client.api.NgaRequest
import com.yanga.client.api.NgaSession
import com.yanga.client.api.NgaThreadParser
import com.yanga.client.api.NgaThreadRead
import com.yanga.client.api.NgaTopicListParser
import com.yanga.client.api.NgaTopicList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoginRequiredException : IllegalStateException("Login is required for this read operation")

interface NgaReadOnlyRepository {
  suspend fun loadHome(): Result<HomeReadData>

  suspend fun loadBoards(session: LoginSessionData?): Result<BoardsReadData>

  suspend fun loadMessages(session: LoginSessionData?): Result<MessagesReadData>

  suspend fun loadProfile(session: LoginSessionData?): Result<ProfileReadData>

  suspend fun loadBoardTopics(session: LoginSessionData?, fid: String, page: Int = 1): Result<NgaTopicList>

  suspend fun loadThread(session: LoginSessionData?, tid: String, page: Int = 1): Result<NgaThreadRead>

  suspend fun listLocalFavoriteBoards(): Result<List<LocalFavoriteBoard>>

  suspend fun addLocalFavoriteBoard(board: LocalFavoriteBoard): Result<Unit>

  suspend fun removeLocalFavoriteBoard(boardId: String): Result<Unit>
}

class DefaultNgaReadOnlyRepository(
  private val transport: NgaHttpTransport = HttpUrlConnectionNgaTransport(),
  private val userAgent: String = "Yanga Android",
  private var baseUrl: String = com.yanga.client.api.NgaDomains.BBS_NGA_CN,
  private val favoriteBoardsStore: FavoriteBoardsStore? = null,
  private val boardsCacheStore: BoardsCacheStore? = null,
) : NgaReadOnlyRepository {
  fun setBaseUrl(url: String) {
    baseUrl = url
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
    val cacheKey = boardCacheKey(session)
    boardsCacheStore?.load(cacheKey)?.let { cached ->
      return@withContext Result.success(cached)
    }

    val api = api(session)
    val subscribedBoards = if (session.requireLogin() != null) {
      execute(api.subscribedBoards(), NgaBoardCategoryParser::parseBoards).getOrDefault(emptyList())
    } else {
      emptyList()
    }

    val directoryResult = execute(api.fullForumDirectory(), NgaBoardCategoryParser::parseSections)

    var allSections = directoryResult.getOrDefault(emptyList())
    if (allSections.isEmpty()) {
      allSections = execute(api.remoteBoardCategories(), NgaBoardCategoryParser::parseSections).getOrDefault(emptyList())
    }

    val legacySubscribedSection =
      allSections.find {
        it.name.contains("收藏") || it.name.contains("订阅")
      }
    val remoteSections = allSections.filter { it != legacySubscribedSection }
    val remoteSubscribedBoards =
      subscribedBoards.ifEmpty { legacySubscribedSection?.groups.orEmpty().flatMap { it.boards } }
    val localFavorites = favoriteBoardsStore?.list().orEmpty().map { it.toBoardSummary() }
    val resolvedSubscribedBoards = (localFavorites + remoteSubscribedBoards).distinctBy { it.boardId }

    val data =
      BoardsReadData(
        subscribedBoards = resolvedSubscribedBoards,
        remoteSections = remoteSections,
      )
    boardsCacheStore?.save(cacheKey, data)
    Result.success(data)
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

    val counters = execute(api.profile(mapOf("uid" to loginSession.uid)), NgaAccountParser::parseProfileCounters)
    if (counters.isFailure) return@withContext Result.failure(counters.exceptionOrNull()!!)

    Result.success(
      ProfileReadData(
        counters = counters.getOrThrow(),
        notifications = notifications.getOrThrow(),
      ),
    )
  }

  override suspend fun loadBoardTopics(session: LoginSessionData?, fid: String, page: Int): Result<NgaTopicList> = withContext(Dispatchers.IO) {
    execute(api(session).topicList(fid = fid.toIntOrNull(), page = page), NgaTopicListParser::parse)
  }

  override suspend fun loadThread(session: LoginSessionData?, tid: String, page: Int): Result<NgaThreadRead> = withContext(Dispatchers.IO) {
    execute(api(session).articleRead(tid = tid.toIntOrNull(), page = page), NgaThreadParser::parseRead)
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

  private fun boardCacheKey(session: LoginSessionData?): String {
    val uid = session?.uid.orEmpty().ifBlank { "guest" }
    return "${baseUrl.trimEnd('/')}_$uid"
  }

  private fun <T> execute(request: NgaRequest, parser: (String) -> T): Result<T> =
    runCatching {
      val response = transport.execute(request).getOrThrow()
      if (!response.isSuccessful) {
        throw response.toException(request)
      }
      parser(response.text)
    }

  private fun NgaHttpResponse.toException(request: NgaRequest): NgaApiException =
    NgaApiException("NGA request failed with HTTP $code for ${request.url}")
}
