package com.yanga.client.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NgaApiTest {
  private val session = NgaSession(
    baseUrl = "https://bbs.nga.cn",
    cookie = "ngaPassportUid=42; ngaPassportCid=abc",
    userAgent = "YangaTest/1.0",
  )

  private val api = NgaApi(session)

  @Test
  fun topicListBuildsForumQuery() {
    val request = api.topicList(fid = 7, page = 2, key = "测试")

    assertEquals(NgaHttpMethod.GET, request.method)
    assertEquals("https://bbs.nga.cn/thread.php", request.url)
    assertEquals("7", request.query["fid"])
    assertEquals("2", request.query["page"])
    assertEquals("js", request.query["lite"])
    assertTrue(request.query.containsKey("noprefix"))
    assertEquals("%E6%B5%8B%E8%AF%95", request.query["key"])
  }

  @Test
  fun articleReadBuildsThreadDetailQuery() {
    val request = api.articleRead(tid = 123, page = 3, authorId = 456)

    assertEquals("https://bbs.nga.cn/read.php", request.url)
    assertEquals("123", request.query["tid"])
    assertEquals("3", request.query["page"])
    assertEquals("456", request.query["authorid"])
    assertEquals("8", request.query["__output"])
    assertTrue(request.query.containsKey("noprefix"))
    assertTrue(request.query.containsKey("v2"))
  }

  @Test
  fun commonHeadersIncludeSessionCookieAndOfficialUserAgent() {
    val request = api.checkIn()

    assertEquals("ngaPassportUid=42; ngaPassportCid=abc", request.headers["Cookie"])
    assertEquals("YangaTest/1.0", request.headers["User-Agent"])
    assertEquals("Nga_Official", request.headers["X-User-Agent"])
  }

  @Test
  fun postBodyUsesGbkEncoding() {
    val body = api.topicPostBody(
      content = "中文内容",
      fid = 7,
      subject = "标题",
      tid = "123",
      action = "reply",
      anonymous = true,
    )

    assertEquals("step=2", body.fields[0])
    assertTrue(body.fields.contains("post_content=%D6%D0%CE%C4%C4%DA%C8%DD"))
    assertTrue(body.fields.contains("post_subject=%B1%EA%CC%E2"))
    assertTrue(body.fields.contains("fid=7"))
    assertTrue(body.fields.contains("tid=123"))
    assertTrue(body.fields.contains("action=reply"))
    assertTrue(body.fields.contains("anony=1"))
  }

  @Test
  fun loginCookieParserDecodesUsernameTwice() {
    val result = NgaLoginCookies.parse(
      "ngaPassportUid=42; ngaPassportCid=abc; ngaPassportUrlencodedUname=%25B2%25E2%25CA%25D4"
    )

    assertEquals(NgaLoginCookies(uid = "42", cid = "abc", username = "测试"), result)
  }

  @Test
  fun responseNormalizerStripsNgaJavaScriptWrapper() {
    val normalized = NgaResponseNormalizer.normalize(
      "window.script_muti_get_var_store={\"data\":{\"0\":\"操作成功\"}}/*\$js\$*/"
    )

    assertEquals("{\"data\":{\"0\":\"操作成功\"}}", normalized)
  }

  @Test
  fun authenticatedRequestFailsWithoutCookie() {
    val anonymousApi = NgaApi(session.copy(cookie = null))

    val result = anonymousApi.authenticated { checkIn() }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is MissingNgaSessionException)
  }

  @Test
  fun messagePostUsesGbkEncodedRecipientsSubjectAndContent() {
    val request = api.messagePost(
      action = "new",
      mid = "",
      recipient = "张三，李四",
      subject = "问候",
      content = "你好",
    )

    assertEquals("message", request.query["__lib"])
    assertEquals("message", request.query["__act"])
    assertEquals("gbk", request.query["charset"])
    assertTrue(request.body.fields.contains("to=%D5%C5%C8%FD%2C%C0%EE%CB%C4"))
    assertTrue(request.body.fields.contains("subject=%CE%CA%BA%F2"))
    assertTrue(request.body.fields.contains("content=%C4%E3%BA%C3"))
  }

  @Test
  fun blockWordUpdateBuildsExpectedPayload() {
    val request = api.updateBlockWords(
      blockedUsers = listOf("100", "200"),
      blockedWords = listOf("广告", "刷屏"),
    )

    assertEquals("ucp", request.bodyMap["__lib"])
    assertEquals("set_block_word", request.bodyMap["__act"])
    assertEquals("8", request.bodyMap["__output"])
    assertEquals("1%0D%0A%B9%E3%B8%E6+%CB%A2%C6%C1%0D%0A100+200", request.bodyMap["data"])
  }

  @Test
  fun staticImageUrlsMatchReferenceTemplates() {
    assertEquals("http://img4.nga.178.com/ngabbs/nga_classic/f/app/7.png", NgaStaticUrls.boardIcon(7))
    assertEquals("https://img4.nga.178.com/proxy/cache_attach/ficon/123v.png", NgaStaticUrls.boardIconByStid(123))
    assertEquals("http://img6.nga.178.com/attachments/mon_a.jpg", NgaStaticUrls.expandRelativeImage("./mon_a.jpg"))
    assertFalse(NgaStaticUrls.expandRelativeImage("https://example.com/a.jpg").contains("img6.nga"))
  }
}
