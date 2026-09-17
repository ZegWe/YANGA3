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
  fun requestFullUrlIncludesEmptyQueryKeysWithoutEquals() {
    val request = NgaRequest(
      method = NgaHttpMethod.GET,
      url = "https://bbs.nga.cn/thread.php",
      query = linkedMapOf("page" to "1", "noprefix" to ""),
    )

    assertEquals("https://bbs.nga.cn/thread.php?page=1&noprefix", request.fullUrl())
  }

  @Test
  fun topicListBuildsForumQuery() {
    val request = api.topicList(fid = 7, page = 2, key = "测试")

    assertEquals(NgaHttpMethod.GET, request.method)
    assertEquals("https://bbs.nga.cn/thread.php", request.url)
    assertEquals("7", request.query["fid"])
    assertEquals("2", request.query["page"])
    assertEquals("js", request.query["lite"])
    assertTrue(request.query.containsKey("noprefix"))
    assertEquals("1", request.query["user"])
    assertEquals("%E6%B5%8B%E8%AF%95", request.query["key"])
  }

  @Test
  fun topicListBuildsTopicSearchVariants() {
    val globalSearch = api.topicList(key = "测试")
    assertEquals("%E6%B5%8B%E8%AF%95", globalSearch.query["key"])
    assertFalse(globalSearch.query.containsKey("fid"))
    assertFalse(globalSearch.query.containsKey("stid"))

    val scopedFidSearch = api.topicList(fid = 7, key = "测试", page = 3)
    assertEquals("7", scopedFidSearch.query["fid"])
    assertEquals("%E6%B5%8B%E8%AF%95", scopedFidSearch.query["key"])
    assertEquals("3", scopedFidSearch.query["page"])

    val scopedStidSearch = api.topicList(stid = 99, key = "测试")
    assertEquals("99", scopedStidSearch.query["stid"])
    assertEquals("%E6%B5%8B%E8%AF%95", scopedStidSearch.query["key"])

    val contentSearch = api.topicList(key = "测试", content = 1)
    assertEquals("1", contentSearch.query["content"])

    val recommendSearch = api.topicList(key = "测试", recommend = true)
    assertEquals("1", recommendSearch.query["recommend"])
    assertEquals("postdatedesc", recommendSearch.query["order_by"])

    val multiFidSearch = api.topicList(fidRaw = "7,8,-9", key = "测试")
    assertEquals("7,8,-9", multiFidSearch.query["fid"])
    assertEquals("%E6%B5%8B%E8%AF%95", multiFidSearch.query["key"])
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
  fun loginPageUsesWebLoginUiWithoutSessionCookie() {
    val request = api.loginPage()

    assertEquals(NgaHttpMethod.GET, request.method)
    assertEquals("https://bbs.nga.cn/nuke.php", request.url)
    assertEquals("login", request.query["__lib"])
    assertEquals("account", request.query["__act"])
    assertTrue(request.query.containsKey("login"))
    assertTrue(request.headers.isEmpty())
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
    assertEquals("https://img4.nga.cn/ngabbs/nga_classic/f/app/7.png", NgaStaticUrls.boardIcon(7))
    assertEquals("https://img4.nga.cn/proxy/cache_attach/ficon/123v.png", NgaStaticUrls.boardIconByStid(123))
    assertEquals("https://img.nga.178.com/attachments/mon_a.jpg", NgaStaticUrls.expandRelativeImage("./mon_a.jpg"))
    assertFalse(NgaStaticUrls.expandRelativeImage("https://example.com/a.jpg").contains("img6.nga"))
    assertEquals(
      "https://bbs.nga.cn/read.php?tid=46634352",
      NgaStaticUrls.threadReadUrl("https://bbs.nga.cn/", "46634352"),
    )
    assertEquals(
      "https://bbs.nga.cn/read.php?tid=46634352&page=3",
      NgaStaticUrls.threadReadUrl("https://bbs.nga.cn", "46634352", page = 3),
    )
  }

  @Test
  fun topicListBuildsSearchFavoriteAndRecommendVariants() {
    val authorSearch = api.topicList(page = 4, authorId = 42, searchPost = 1, favor = 1, content = 1, author = "张三")
    assertEquals("42", authorSearch.query["authorid"])
    assertEquals("1", authorSearch.query["searchpost"])
    assertEquals("1", authorSearch.query["favor"])
    assertEquals("1", authorSearch.query["content"])
    assertEquals("%D5%C5%C8%FD", authorSearch.query["author"])
    assertEquals("1", authorSearch.query["user"])
    assertFalse(authorSearch.query.containsKey("fid"))

    val stidSearch = api.topicList(stid = 99, fid = 7, fidGroup = "user", recommend = true)
    assertEquals("99", stidSearch.query["stid"])
    assertEquals("user", stidSearch.query["fidgroup"])
    assertEquals("1", stidSearch.query["recommend"])
    assertEquals("postdatedesc", stidSearch.query["order_by"])
    assertEquals("1", stidSearch.query["user"])
    assertEquals("", stidSearch.query["key"])

    val subBoardFilter = api.topicList(fid = 7, fidGroup = "448,-692072")
    assertEquals("7", subBoardFilter.query["fid"])
    assertEquals("448,-692072", subBoardFilter.query["fidgroup"])

    val fidRecommend = api.topicList(fid = 7, recommend = true)
    assertFalse(fidRecommend.query.containsKey("key"))
  }

  @Test
  fun articleReadBuildsPidQuery() {
    val request = api.articleRead(pid = 987, page = 5)

    assertEquals("987", request.query["pid"])
    assertEquals("1", request.query["searchpost"])
    assertEquals("5", request.query["page"])
    assertFalse(request.query.containsKey("tid"))
  }

  @Test
  fun boardAndRemoteCategoryRequestsMatchReferenceEndpoints() {
    val boardSearch = api.boardSearch("议事厅")
    assertEquals(NgaHttpMethod.GET, boardSearch.method)
    assertEquals("https://bbs.nga.cn/forum.php", boardSearch.url)
    assertEquals("8", boardSearch.query["__output"])
    assertEquals("%D2%E9%CA%C2%CC%FC", boardSearch.query["key"])
    assertEquals(session.cookie, boardSearch.headers["Cookie"])

    val categories = api.remoteBoardCategories()
    assertEquals("https://bbs.nga.cn/app_api.php", categories.url)
    assertEquals("home", categories.query["__lib"])
    assertEquals("category", categories.query["__act"])
  }

  @Test
  fun topicPostInfoBuildsPreflightQuery() {
    val request = api.topicPostInfo(fid = 7, action = "reply", pid = "11", tid = "22", stid = "33")

    assertEquals(NgaHttpMethod.POST, request.method)
    assertEquals("https://bbs.nga.cn/post.php", request.url)
    assertEquals("7", request.query["fid"])
    assertEquals("js", request.query["lite"])
    assertEquals("reply", request.query["action"])
    assertEquals("11", request.query["pid"])
    assertEquals("22", request.query["tid"])
    assertEquals("33", request.query["stid"])
  }

  @Test
  fun topicPostBuildsSubmitBodyWithOptionalFields() {
    val request = api.topicPost(content = "回复", fid = 7, subject = "主题", pid = "11", tid = "22", action = "reply")

    assertEquals(NgaHttpMethod.POST, request.method)
    assertEquals("https://bbs.nga.cn/post.php", request.url)
    assertTrue(request.body.fields.contains("post_content=%BB%D8%B8%B4"))
    assertTrue(request.body.fields.contains("post_subject=%D6%F7%CC%E2"))
    assertTrue(request.body.fields.contains("pid=11"))
    assertTrue(request.body.fields.contains("tid=22"))
  }

  @Test
  fun topicPostBodyIncludesAttachmentsAndStidWhenPresent() {
    val body = api.topicPostBody(
      content = "图",
      fid = 7,
      attachments = "\t1",
      attachmentsCheck = "\tabc",
      stid = "99",
    )

    assertTrue(body.fields.contains("attachments=\t1"))
    assertTrue(body.fields.contains("attachments_check=\tabc"))
    assertTrue(body.fields.contains("stid=99"))
  }

  @Test
  fun attachmentUploadMetadataBuildsFixedUploadEndpointAndFields() {
    val request = api.attachmentUploadMetadata(fid = 7, auth = "auth-token", fileName = "a.png")

    assertEquals(NgaHttpMethod.POST, request.method)
    assertEquals("https://img8.nga.cn/attach.php", request.url)
    assertEquals("multipart/form-data", request.headers["Content-Type"])
    assertEquals("a.png", request.bodyMap["attachment_file1_url_utf8_name"])
    assertEquals("7", request.bodyMap["fid"])
    assertEquals("auth-token", request.bodyMap["auth"])
    assertEquals("upload", request.bodyMap["func"])
    assertEquals("bbs.ngacn.cc", request.bodyMap["origin_domain"])
  }

  @Test
  fun commentPostBuildsReplyCommentBody() {
    val request = api.commentPost(fid = 7, tid = 22, pid = 33, comment = "你好", prefix = "#1 ", anonymous = true)

    assertEquals("https://bbs.nga.cn/post.php", request.url)
    assertEquals("22", request.bodyMap["tid"])
    assertEquals("33", request.bodyMap["pid"])
    assertEquals("reply", request.bodyMap["action"])
    assertEquals("1", request.bodyMap["comment"])
    assertEquals("htmljs", request.bodyMap["lite"])
    assertEquals("1", request.bodyMap["anony"])
    assertEquals("%231+%C4%E3%BA%C3", request.bodyMap["post_content"])
  }

  @Test
  fun favoriteLikeReportNotificationAndCheckInRequestsMatchReference() {
    val addFavorite = api.favoriteAdd(tid = "22", pid = "33")
    assertEquals("topic_favor", addFavorite.query["__lib"])
    assertEquals("add", addFavorite.query["action"])
    assertEquals("33", addFavorite.query["pid"])

    val removeFavorite = api.favoriteRemove(page = 2, tid = 22, pid = 33)
    assertEquals("del", removeFavorite.bodyMap["action"])
    assertEquals("22_33", removeFavorite.bodyMap["tidarray"])

    val like = api.like(tid = 22, pid = 33, support = false)
    assertEquals("topic_recommend", like.bodyMap["__lib"])
    assertEquals("-1", like.bodyMap["value"])

    val report = api.report(mapOf("tid" to "22", "reason" to "spam"))
    assertEquals("log_post", report.query["__lib"])
    assertEquals("report", report.query["__act"])
    assertEquals("spam", report.bodyMap["reason"])

    val notifications = api.notifications()
    assertEquals("noti", notifications.query["__lib"])
    assertEquals("get_all", notifications.query["__act"])

    val clearNotifications = api.clearNotifications()
    assertEquals("del", clearNotifications.query["__act"])
    assertEquals("3", clearNotifications.query["raw"])

    val checkIn = api.checkIn()
    assertEquals("check_in", checkIn.query["__lib"])
    assertEquals("check_in", checkIn.query["__act"])
  }

  @Test
  fun profileSignatureAndBlockWordRequestsMatchReference() {
    val profile = api.profile(mapOf("uid" to "42"))
    assertEquals("ucp", profile.query["__lib"])
    assertEquals("get", profile.query["__act"])
    assertEquals("https://bbs.nga.cn/nuke.php?func=ucp&lite=jsx&uid=42", profile.headers["Referer"])

    val signature = api.signature(uid = "42", sign = "签名")
    assertEquals("set_sign", signature.bodyMap["__lib"])
    assertEquals("gbk", signature.bodyMap["charset"])
    assertEquals("%C7%A9%C3%FB", signature.bodyMap["sign"])

    val blockWords = api.blockWords(uid = "42")
    assertEquals("ucp", blockWords.bodyMap["__lib"])
    assertEquals("get_block_word", blockWords.bodyMap["__act"])
    assertEquals("https://bbs.nga.cn/nuke.php?func=ucp&uid=42", blockWords.headers["Referer"])

    val updateBlockWords = api.updateBlockWords(blockedUsers = emptyList(), blockedWords = emptyList())
    assertEquals("https://bbs.nga.cn", updateBlockWords.headers["Origin"])
    assertEquals("GBK", updateBlockWords.headers["charset"])
    assertEquals("1%0D%0A%0D%0A", updateBlockWords.bodyMap["data"])
  }

  @Test
  fun messageListAndReadBuildExpectedQueries() {
    val list = api.messageList(page = 3)
    assertEquals(NgaHttpMethod.GET, list.method)
    assertEquals("message", list.query["__lib"])
    assertEquals("list", list.query["act"])
    assertEquals("3", list.query["page"])

    val read = api.messageRead(mid = "abc", page = 4)
    assertEquals("read", read.query["act"])
    assertEquals("abc", read.query["mid"])
    assertEquals("4", read.query["page"])
  }

  @Test
  fun subBoardFilterGetAndSetUseBlockListContract() {
    val get = api.subBoardFilterGet(parentFid = "-7861121")
    assertEquals(NgaHttpMethod.GET, get.method)
    assertEquals("user_option", get.query["__lib"])
    assertEquals("get", get.query["__act"])
    assertEquals("1", get.query["type"])
    assertEquals("add_to_block_tids", get.query["info"])
    assertEquals("-7861121", get.query["fid"])

    val show = api.subBoardFilterSet(parentFid = "-7861121", blockId = "4654", visible = true)
    assertEquals(NgaHttpMethod.POST, show.method)
    assertEquals("del", show.query.keys.firstOrNull { it == "del" || it == "add" })
    assertEquals("4654", show.query["del"])
    assertEquals("add_to_block_tids", show.query["info"])

    val hide = api.subBoardFilterSet(parentFid = "-7861121", blockId = "4654", visible = false)
    assertEquals("4654", hide.query["add"])
  }

  @Test
  fun subBoardOptionMapsSubscribeActionsByType() {
    val typeOneSubscribe = api.subBoardOption(type = 1, parentFid = "7", boardId = "99", subscribe = true)
    assertEquals("http://bbs.ngacn.cc/nuke.php", typeOneSubscribe.url)
    assertEquals("99", typeOneSubscribe.query["del"])
    assertFalse(typeOneSubscribe.query.containsKey("add"))

    val typeTwoSubscribe = api.subBoardOption(type = 2, parentFid = "7", boardId = "99", subscribe = true)
    assertEquals("99", typeTwoSubscribe.query["add"])

    val typeTwoUnsubscribe = api.subBoardOption(type = 2, parentFid = "7", boardId = "99", subscribe = false)
    assertEquals("99", typeTwoUnsubscribe.query["del"])
  }

  @Test
  fun voteBuildsVoteAndSettleQueries() {
    val vote = api.vote(tid = 22, voteIds = listOf(1, 2))
    assertEquals("vote", vote.query["__act"])
    assertEquals("22", vote.query["tid"])
    assertEquals("1,2", vote.query["voteid"])

    val settle = api.vote(tid = 22, voteIds = listOf(1), settle = true)
    assertEquals("settle", settle.query["__act"])
  }

  @Test
  fun avatarUploadAndChangeRequestsMatchLegacyEndpoints() {
    val upload = api.avatarUploadMetadata(fileName = "avatar.jpg")
    assertEquals("https://app.myauth.us/api/attach.php", upload.url)
    assertEquals("-7", upload.bodyMap["fid"])
    assertEquals("avatar.jpg", upload.bodyMap["attachment_file1_url_utf8_name"])
    assertEquals("upload", upload.bodyMap["func"])

    val change = api.avatarChange(iconUrl = "http://img.example/头像.jpg", checksum = "sum")
    assertEquals("https://nga.178.com/nuke.php", change.url)
    assertEquals("js", change.bodyMap["lite"])
    assertEquals("", change.bodyMap["noprefix"])
    assertEquals("avatar", change.bodyMap["func"])
    assertEquals("sum", change.bodyMap["__ngaClientChecksum"])
    assertEquals("http%3A%2F%2Fimg.example%2F%CD%B7%CF%F1.jpg", change.bodyMap["icon"])
  }

  @Test
  fun threadParserParsesReadResponsePostsAndUserMapping() {
    val raw = """
      window.script_muti_get_var_store={
        "data":{
          "__T":{"tid":46634352,"fid":7,"subject":"测试主题"},
          "__U":{"42":{"uid":42,"username":"作者A","avatar":".a/12345_67890.jpg?1234567890"}},
          "__R":{
            "0":{
              "pid":101,
              "tid":46634352,
              "fid":7,
              "authorid":42,
              "subject":"",
              "content":"正文<br/>内容",
              "lou":0,
              "postdatetimestamp":1770000000
            }
          },
          "__PAGE":1
        }
      }/*${'$'}js${'$'}*/
    """.trimIndent()

    val thread = NgaThreadParser.parseRead(raw)

    assertEquals("46634352", thread.tid)
    assertEquals("测试主题", thread.subject)
    assertEquals("7", thread.fid)
    assertEquals(1, thread.page)
    assertEquals(1, thread.posts.size)
    assertEquals("101", thread.posts.first().pid)
    assertEquals("42", thread.posts.first().authorId)
    assertEquals("作者A", thread.posts.first().author)
    assertEquals(
      "https://img4.nga.178.com/avatars/2002/039/003/000/12345_67890.jpg?1234567890",
      thread.posts.first().authorAvatarUrl,
    )
    assertEquals("正文<br/>内容", thread.posts.first().content)
    assertEquals(1770000000L, thread.posts.first().postDate)
  }

  @Test
  fun threadParserParsesPostAttachments() {
    val raw = """
      {
        "data":{
          "__T":{"tid":46634352,"fid":7,"subject":"附件主题"},
          "__U":{"42":{"uid":42,"username":"作者A"}},
          "__R":{
            "0":{
              "pid":101,
              "tid":46634352,
              "fid":7,
              "authorid":42,
              "subject":"",
              "content":"正文",
              "lou":0,
              "postdatetimestamp":1770000000,
              "attachs":{
                "0":{
                  "attachurl":"/mon_202606/01/sample.png"
                }
              }
            }
          },
          "__PAGE":1
        }
      }
    """.trimIndent()

    val thread = NgaThreadParser.parseRead(raw)

    assertEquals(
      listOf(
        NgaThreadAttachment(
          name = "sample.png",
          url = "https://img.nga.178.com/attachments/mon_202606/01/sample.png",
        ),
      ),
      thread.posts.first().attachments,
    )
  }

  @Test
  fun threadParserParsesEmbeddedCommentsHotRepliesAndCommentPostContent() {
    val raw = """
      {
        "data":{
          "__T":{"tid":46859796,"fid":321,"subject":"[公告]不让发转让购票资格"},
          "__U":{
            "33855250":{"uid":33855250,"username":"FBbZ"},
            "42988763":{"uid":42988763,"username":"评论者"},
            "34161502":{"uid":34161502,"username":"热点用户"}
          },
          "__R":{
            "0":{
              "pid":0,
              "tid":46859796,
              "fid":321,
              "authorid":33855250,
              "subject":"[公告]不让发转让购票资格",
              "content":"正文[b]闲鱼[/b]",
              "lou":0,
              "postdatetimestamp":1779786422,
              "alterinfo":"[E1779786491 0 0]\t",
              "comment":{
                "0":{
                  "pid":869599169,
                  "tid":46859796,
                  "authorid":42988763,
                  "content":"[b]Reply to [tid=46859796]Topic[/tid] Post by [uid=33855250]FBbZ[/uid] (2026-05-26 17:07)[/b]<br/><br/>不是，才10场都满足不了还有必要去现场吗",
                  "lou":10,
                  "score":5,
                  "postdatetimestamp":1779850050
                }
              },
              "hotreply":{
                "0":{
                  "pid":869524613,
                  "tid":46859796,
                  "authorid":34161502,
                  "content":"确实<br/>游戏里有变声器毛妹",
                  "lou":2,
                  "score":24,
                  "postdatetimestamp":1779786640
                }
              }
            },
            "10":{
              "pid":869599169,
              "authorid":42988763,
              "lou":10,
              "comment_to_id":-1
            }
          },
          "__PAGE":1
        }
      }
    """.trimIndent()

    val thread = NgaThreadParser.parseRead(raw)
    val op = thread.posts.first { it.lou == 0 }
    val commentPost = thread.posts.first { it.lou == 10 }

    assertEquals(1779786491L, op.editDate)
    assertEquals(1, op.embeddedComments.size)
    assertEquals("42988763", op.embeddedComments.first().authorId)
    assertEquals(1, op.hotReplies.size)
    assertEquals("869524613", op.hotReplies.first().pid)
    assertTrue(commentPost.content.contains("不是，才10场都满足不了"))
    assertEquals(1779850050L, commentPost.postDate)
  }
}
