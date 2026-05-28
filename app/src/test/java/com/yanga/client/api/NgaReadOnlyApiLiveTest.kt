package com.yanga.client.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

class NgaReadOnlyApiLiveTest {
  private val cookie = System.getenv("NGA_COOKIE").orEmpty()
  private val uid = System.getenv("NGA_UID").orEmpty()
  private val fid = System.getenv("NGA_TEST_FID")?.toIntOrNull() ?: 7
  private val tid = System.getenv("NGA_TEST_TID")?.toIntOrNull()

  private val api = NgaApi(
    NgaSession(
      baseUrl = System.getenv("NGA_BASE_URL").orEmpty().ifBlank { NgaDomains.BBS_NGA_CN },
      cookie = cookie,
      userAgent = System.getenv("NGA_USER_AGENT").orEmpty().ifBlank { "Yanga Android LiveTest" },
    ),
  )

  @Test
  fun readOnlyPublicForumApisReturnHttpSuccess() {
    requireLiveCookie()

    listOf(
      "topicList" to api.topicList(fid = fid, page = 1),
      "boardSearch" to api.boardSearch("议事厅"),
      "remoteBoardCategories" to api.remoteBoardCategories(),
    ).forEach { (name, request) ->
      val response = request.executeReadOnly()
      assertTrue("$name HTTP ${response.code}: ${response.preview}", response.code in 200..399)
      assertTrue("$name returned an empty response", response.text.isNotBlank())
    }
  }

  @Test
  fun readOnlyThreadDetailReturnsHttpSuccessWhenTidIsProvided() {
    requireLiveCookie()
    assumeTrue("Set NGA_TEST_TID to run thread-detail live read test", tid != null)

    val response = api.articleRead(tid = tid, page = 1).executeReadOnly()
    val thread = NgaThreadParser.parseRead(response.text)

    assertTrue("articleRead HTTP ${response.code}: ${response.preview}", response.code in 200..399)
    assertEquals(tid.toString(), thread.tid)
    assertTrue("articleRead parsed no posts", thread.posts.isNotEmpty())
    assertTrue("articleRead parsed empty content", thread.posts.any { it.content.isNotBlank() })
  }

  @Test
  fun readOnlyAuthenticatedAccountApisReturnHttpSuccess() {
    requireLiveCookie()

    listOf(
      "notifications" to api.notifications(),
      "messageList" to api.messageList(page = 1),
    ).forEach { (name, request) ->
      val response = request.executeReadOnly()
      assertTrue("$name HTTP ${response.code}: ${response.preview}", response.code in 200..399)
      assertTrue("$name returned an empty response", response.text.isNotBlank())
    }
  }

  @Test
  fun readOnlyProfileReturnsHttpSuccessWhenUidIsProvided() {
    requireLiveCookie()
    assumeTrue("Set NGA_UID to run profile live read test", uid.isNotBlank())

    val response = api.profile(mapOf("uid" to uid)).executeReadOnly()

    assertTrue("profile HTTP ${response.code}: ${response.preview}", response.code in 200..399)
    assertTrue(response.text.isNotBlank())
  }

  private fun requireLiveCookie() {
    assumeTrue("Set NGA_COOKIE to run live read-only API tests", cookie.isNotBlank())
  }

  private fun NgaRequest.executeReadOnly(): LiveResponse {
    assertEquals("Live read-only tests must not execute mutating POST requests", NgaHttpMethod.GET, method)

    val connection = (URL(fullUrl()).openConnection() as HttpURLConnection).apply {
      requestMethod = "GET"
      connectTimeout = 15_000
      readTimeout = 15_000
      headers.forEach { (key, value) -> setRequestProperty(key, value) }
    }

    val code = connection.responseCode
    val stream = if (code in 200..399) connection.inputStream else connection.errorStream
    val text = stream?.use {
      BufferedReader(InputStreamReader(it, Charset.forName("GBK"))).readText()
    }.orEmpty()
    return LiveResponse(code = code, text = text)
  }

  private fun NgaRequest.fullUrl(): String {
    if (query.isEmpty()) return url
    return url + "?" + query.entries.joinToString("&") { (key, value) ->
      if (value.isEmpty()) key else "$key=$value"
    }
  }

  private data class LiveResponse(
    val code: Int,
    val text: String,
  ) {
    val preview: String = text.take(300)
  }
}
