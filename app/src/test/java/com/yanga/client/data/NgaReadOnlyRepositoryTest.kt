package com.yanga.client.data

import com.yanga.client.api.NgaHttpResponse
import com.yanga.client.api.NgaHttpTransport
import com.yanga.client.api.NgaRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class NgaReadOnlyRepositoryTest {
  @Test
  fun loadHomeFetchesRemoteBoardCategoriesAndTopicListAndParsesBoth() = runTest {
    val transport = FakeTransport(
      "app_api.php" to fixture("remote_board_categories.json"),
      "thread.php" to fixture("topic_list_public.json"),
    )
    val repository = DefaultNgaReadOnlyRepository(transport)

    val result = repository.loadHome()

    val data = result.getOrThrow()
    assertEquals(2, data.boards.size)
    assertEquals("7", data.boards[0].boardId)
    assertEquals(2, data.activeTopics.size)
    assertEquals("1001", data.activeTopics[0].topicId)
    assertEquals(
      listOf(
        RequestKey("app_api.php", mapOf("__lib" to "home", "__act" to "category")),
        RequestKey(
          "thread.php",
          mapOf(
            "page" to "1",
            "lite" to "js",
            "noprefix" to "",
            "recommend" to "1",
            "order_by" to "postdatedesc",
            "user" to "1",
          ),
        ),
      ),
      transport.requests.map { it.key() },
    )
  }

  @Test
  fun loadBoardsFetchesRemoteCategoriesAndSubscribedBoardsForSession() = runTest {
    val transport = FakeTransport(
      responses = mapOf(
        RequestKey("nuke.php", mapOf("__lib" to "user_option", "__act" to "get", "type" to "1")) to
          fixture("subscribed_boards.json"),
      ),
      fallbackResponses = mapOf(
        "app_api.php" to fixture("remote_board_categories.json"),
      ),
    )
    val repository = DefaultNgaReadOnlyRepository(transport)

    val result = repository.loadBoards(session())

    val data = result.getOrThrow()
    assertEquals(1, data.subscribedBoards.size)
    assertEquals("310", data.subscribedBoards[0].boardId)
    assertEquals("真实关注板块", data.subscribedBoards[0].name)
    assertEquals(2, data.remoteSections.size)
    assertEquals("综合", data.remoteSections[0].name)
    assertEquals(listOf("nuke.php", "nuke.php", "app_api.php"), transport.requests.map { it.pathName() })
    assertEquals("user_option", transport.requests[0].query["__lib"])
    assertEquals("1", transport.requests[0].query["type"])
  }

  @Test
  fun loadBoardsWithoutSessionSkipsSubscribedBoardsEndpoint() = runTest {
    val transport = FakeTransport("app_api.php" to fixture("remote_board_categories.json"))
    val repository = DefaultNgaReadOnlyRepository(transport)

    val result = repository.loadBoards(null)

    val data = result.getOrThrow()
    assertTrue(data.subscribedBoards.isEmpty())
    assertEquals(2, data.remoteSections.size)
    assertEquals(listOf("nuke.php", "app_api.php"), transport.requests.map { it.pathName() })
  }

  @Test
  fun loadBoardsWithoutSessionReturnsLocalFavorites() = runTest {
    val transport = FakeTransport("app_api.php" to fixture("remote_board_categories.json"))
    val store = FakeFavoriteStore(mutableListOf(LocalFavoriteBoard(boardId = "123", name = "本地收藏", iconUrl = "i", category = "综合 / 分组")))
    val repository = DefaultNgaReadOnlyRepository(transport, favoriteBoardsStore = store)

    val result = repository.loadBoards(null)

    val data = result.getOrThrow()
    assertEquals(listOf("123"), data.subscribedBoards.map { it.boardId })
    assertEquals("本地收藏", data.subscribedBoards.first().name)
  }

  @Test
  fun loadBoardsDoesNotTreatRootSectionAsSubscribedFallback() = runTest {
    val transport = FakeTransport(
      responses = mapOf(
        RequestKey("nuke.php", mapOf("__lib" to "user_option", "__act" to "get", "type" to "1")) to
          fixture("subscribed_boards.json"),
      ),
      fallbackResponses = mapOf(
        "app_api.php" to fixture("remote_board_categories.json"),
      ),
    )
    val repository = DefaultNgaReadOnlyRepository(transport)

    val result = repository.loadBoards(session())

    val data = result.getOrThrow()
    assertEquals(listOf("310"), data.subscribedBoards.map { it.boardId })
    assertTrue(data.remoteSections.none { it.id == "0" || it.id == "-1" })
    assertEquals(2, data.remoteSections.size)
  }

  @Test
  fun loadMessagesWithoutSessionReturnsLoginRequiredFailure() = runTest {
    val repository = DefaultNgaReadOnlyRepository(FakeTransport())

    val result = repository.loadMessages(null)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is LoginRequiredException)
  }

  @Test
  fun loadMessagesWithSessionParsesMessageList() = runTest {
    val transport = FakeTransport("nuke.php" to fixture("message_list.json"))
    val repository = DefaultNgaReadOnlyRepository(transport)

    val result = repository.loadMessages(session())

    val data = result.getOrThrow()
    assertEquals(2, data.messages.size)
    assertEquals("501", data.messages[0].messageId)
    assertEquals("张三", data.messages[0].contactName)
    assertEquals("ngaPassportUid=42; ngaPassportCid=abc", transport.requests.single().headers["Cookie"])
    assertEquals("message", transport.requests.single().query["__lib"])
    assertEquals("list", transport.requests.single().query["act"])
  }

  @Test
  fun loadProfileWithoutSessionReturnsLoginRequiredFailure() = runTest {
    val repository = DefaultNgaReadOnlyRepository(FakeTransport())

    val result = repository.loadProfile(null)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is LoginRequiredException)
  }

  @Test
  fun loadProfileWithSessionCombinesNotificationsAndProfileCounters() = runTest {
    val transport = FakeTransport(
      responses = mapOf(
        RequestKey("nuke.php", mapOf("__lib" to "noti")) to fixture("notifications.json"),
        RequestKey("nuke.php", mapOf("__lib" to "ucp")) to fixture("profile.json"),
      ),
    )
    val repository = DefaultNgaReadOnlyRepository(transport)

    val result = repository.loadProfile(session())

    val data = result.getOrThrow()
    assertEquals(2, data.notifications.size)
    assertEquals("701", data.notifications[0].id)
    assertEquals(5, data.counters.favoriteTopics)
    assertEquals(3, data.counters.subscribedBoards)
    assertEquals(
      listOf("noti", "ucp"),
      transport.requests.map { it.query.getValue("__lib") },
    )
  }

  @Test
  fun transportFailurePropagatesAsResultFailure() = runTest {
    val failure = IllegalStateException("network down")
    val repository = DefaultNgaReadOnlyRepository(FakeTransport(failure = failure))

    val result = repository.loadHome()

    assertTrue(result.isFailure)
    assertSame(failure, result.exceptionOrNull())
  }

  private fun session(): LoginSessionData =
    LoginSessionData(
      username = "测试",
      uid = "42",
      cookie = "ngaPassportUid=42; ngaPassportCid=abc",
    )

  private fun fixture(name: String): String =
    checkNotNull(javaClass.classLoader?.getResource("fixtures/nga/$name")) {
      "Missing fixture $name"
    }.readText()

  private data class RequestKey(
    val path: String,
    val query: Map<String, String> = emptyMap(),
  )

  private class FakeTransport(
    private val responses: Map<RequestKey, String> = emptyMap(),
    private val fallbackResponses: Map<String, String> = emptyMap(),
    private val failure: Throwable? = null,
  ) : NgaHttpTransport {
    constructor(vararg responses: Pair<String, String>) : this(
      fallbackResponses = responses.toMap(),
    )

    val requests = mutableListOf<NgaRequest>()

    override fun execute(request: NgaRequest): Result<NgaHttpResponse> {
      requests += request
      failure?.let { return Result.failure(it) }
      val text = findResponse(request) ?: fallbackResponses[pathName(request)]
      return if (text == null) {
        Result.failure(AssertionError("Unexpected request ${request.url} ${request.query}"))
      } else {
        Result.success(NgaHttpResponse(code = 200, text = text))
      }
    }

    private fun findResponse(request: NgaRequest): String? =
      responses.entries.firstOrNull { (key, _) ->
        key.path == pathName(request) && key.query.all { (name, value) -> request.query[name] == value }
      }?.value

    private fun pathName(request: NgaRequest): String =
      request.url.substringAfterLast('/')
  }

  private class FakeFavoriteStore(
    private val boards: MutableList<LocalFavoriteBoard> = mutableListOf(),
  ) : FavoriteBoardsStore {
    override fun list(): List<LocalFavoriteBoard> = boards.toList()

    override fun upsert(board: LocalFavoriteBoard) {
      boards.removeAll { it.boardId == board.boardId }
      boards.add(board)
    }

    override fun remove(boardId: String) {
      boards.removeAll { it.boardId == boardId }
    }
  }

  private fun NgaRequest.key(): RequestKey =
    RequestKey(path = pathName(), query = query)

  private fun NgaRequest.pathName(): String =
    url.substringAfterLast('/')
}
