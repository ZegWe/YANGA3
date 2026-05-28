package com.yanga.client.data

import com.yanga.client.api.NgaAccountParser
import com.yanga.client.api.NgaApi
import com.yanga.client.api.NgaApiException
import com.yanga.client.api.NgaBoardCategoryParser
import com.yanga.client.api.NgaHttpResponse
import com.yanga.client.api.NgaHttpTransport
import com.yanga.client.api.NgaMessageParser
import com.yanga.client.api.NgaRequest
import com.yanga.client.api.NgaSession
import com.yanga.client.api.NgaTopicListParser

class LoginRequiredException : IllegalStateException("Login is required for this read operation")

interface NgaReadOnlyRepository {
  suspend fun loadHome(): Result<HomeReadData>

  suspend fun loadBoards(session: LoginSessionData?): Result<BoardsReadData>

  suspend fun loadMessages(session: LoginSessionData?): Result<MessagesReadData>

  suspend fun loadProfile(session: LoginSessionData?): Result<ProfileReadData>
}

class DefaultNgaReadOnlyRepository(
  private val transport: NgaHttpTransport,
  private val userAgent: String = "Yanga Android",
) : NgaReadOnlyRepository {
  override suspend fun loadHome(): Result<HomeReadData> {
    val api = api()
    val boards = execute(api.remoteBoardCategories(), NgaBoardCategoryParser::parse)
    if (boards.isFailure) return Result.failure(boards.exceptionOrNull()!!)

    val topics = execute(api.topicList(page = 1, recommend = true)) { raw ->
      NgaTopicListParser.parse(raw).topics
    }
    if (topics.isFailure) return Result.failure(topics.exceptionOrNull()!!)

    return Result.success(
      HomeReadData(
        boards = boards.getOrThrow(),
        activeTopics = topics.getOrThrow(),
      ),
    )
  }

  override suspend fun loadBoards(session: LoginSessionData?): Result<BoardsReadData> {
    val categories = execute(api(session).remoteBoardCategories(), NgaBoardCategoryParser::parse)
    if (categories.isFailure) return Result.failure(categories.exceptionOrNull()!!)

    return Result.success(
      BoardsReadData(
        subscribedBoards = emptyList(),
        remoteCategories = categories.getOrThrow(),
      ),
    )
  }

  override suspend fun loadMessages(session: LoginSessionData?): Result<MessagesReadData> {
    val loginSession = session.requireLogin()
      ?: return Result.failure(LoginRequiredException())
    val messages = execute(api(loginSession).messageList(page = 1), NgaMessageParser::parseList)
    if (messages.isFailure) return Result.failure(messages.exceptionOrNull()!!)

    return Result.success(MessagesReadData(messages = messages.getOrThrow()))
  }

  override suspend fun loadProfile(session: LoginSessionData?): Result<ProfileReadData> {
    val loginSession = session.requireLogin()
      ?: return Result.failure(LoginRequiredException())
    val api = api(loginSession)
    val notifications = execute(api.notifications(), NgaAccountParser::parseNotifications)
    if (notifications.isFailure) return Result.failure(notifications.exceptionOrNull()!!)

    val counters = execute(api.profile(mapOf("uid" to loginSession.uid)), NgaAccountParser::parseProfileCounters)
    if (counters.isFailure) return Result.failure(counters.exceptionOrNull()!!)

    return Result.success(
      ProfileReadData(
        counters = counters.getOrThrow(),
        notifications = notifications.getOrThrow(),
      ),
    )
  }

  private fun api(session: LoginSessionData? = null): NgaApi =
    NgaApi(NgaSession(cookie = session?.cookie, userAgent = userAgent))

  private fun LoginSessionData?.requireLogin(): LoginSessionData? =
    this?.takeIf { it.cookie.isNotBlank() }

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
