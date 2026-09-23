package com.yanga.client.api

class NgaApi(private val session: NgaSession = NgaSession()) {
  fun authenticated(block: NgaApi.() -> NgaRequest): Result<NgaRequest> =
    if (session.cookie.isNullOrBlank()) {
      Result.failure(MissingNgaSessionException())
    } else {
      Result.success(block())
    }

  fun topicList(
    page: Int = 1,
    fid: Int? = null,
    stid: Int? = null,
    authorId: Int? = null,
    searchPost: Int? = null,
    favor: Int? = null,
    content: Int? = null,
    author: String? = null,
    key: String? = null,
    fidRaw: String? = null,
    fidGroup: String? = null,
    recommend: Boolean = false,
  ): NgaRequest = get(
    path = "thread.php",
    query = linkedMapOf<String, String>().apply {
      authorId?.let { put("authorid", it.toString()) }
      searchPost?.let { put("searchpost", it.toString()) }
      favor?.let { put("favor", it.toString()) }
      content?.let { put("content", it.toString()) }
      if (!author.isNullOrBlank()) {
        put("author", NgaEncoding.urlEncodeGbk(author))
      } else {
        stid?.let { put("stid", it.toString()) }
          ?: fidRaw?.takeIf { it.isNotBlank() }?.let { put("fid", it) }
          ?: fid?.let { put("fid", it.toString()) }
        key?.takeIf { it.isNotBlank() }?.let { put("key", NgaEncoding.urlEncodeUtf8(it)) }
        if (recommend && stid != null && key == null) {
          put("key", "")
        }
        fidGroup?.takeIf { it.isNotBlank() }?.let { put("fidgroup", it) }
      }
      put("page", page.toString())
      put("lite", "js")
      put("noprefix", "")
      put("user", "1")
      if (recommend) {
        put("recommend", "1")
        put("order_by", "postdatedesc")
      }
    },
  )

  fun articleRead(
    page: Int = 1,
    tid: Int? = null,
    pid: Int? = null,
    authorId: Int? = null,
  ): NgaRequest = get(
    path = "read.php",
    query = linkedMapOf<String, String>().apply {
      put("page", page.toString())
      put("__output", "8")
      put("noprefix", "")
      put("v2", "")
      tid?.let { put("tid", it.toString()) }
      pid?.let { put("pid", it.toString()) }
      pid?.let { put("searchpost", "1") }
      authorId?.let { put("authorid", it.toString()) }
    },
  )

  fun boardSearch(boardName: String): NgaRequest =
    get("forum.php", linkedMapOf("__output" to "8", "key" to NgaEncoding.urlEncodeGbk(boardName)))

  fun remoteBoardCategories(): NgaRequest = get("app_api.php", linkedMapOf("__lib" to "home", "__act" to "category"))

  fun fullForumDirectory(): NgaRequest = get("nuke.php", linkedMapOf("__lib" to "forum_all", "__act" to "forum_all", "__output" to "8"))

  fun subscribedBoards(): NgaRequest = get("nuke.php", linkedMapOf("__lib" to "user_option", "__act" to "get", "type" to "1", "__output" to "8"))

  fun subBoardFilterGet(parentFid: String): NgaRequest =
    get(
      path = "nuke.php",
      query =
        linkedMapOf(
          "__lib" to "user_option",
          "__act" to "get",
          "raw" to "3",
          "type" to "1",
          "info" to "add_to_block_tids",
          "fid" to parentFid,
          "__output" to "8",
        ),
    )

  fun subBoardFilterSet(parentFid: String, blockId: String, visible: Boolean): NgaRequest {
    val action = if (visible) "del" else "add"
    return NgaRequest(
      method = NgaHttpMethod.POST,
      url = "${session.normalizedBaseUrl}/nuke.php",
      query =
        linkedMapOf(
          "__lib" to "user_option",
          "__act" to "set",
          "raw" to "3",
          "type" to "1",
          "info" to "add_to_block_tids",
          "fid" to parentFid,
          "__output" to "8",
          action to blockId,
        ),
      headers = commonHeaders(),
    )
  }

  fun loginPage(): NgaRequest = NgaRequest(
    method = NgaHttpMethod.GET,
    url = "https://bbs.nga.cn/nuke.php",
    query = linkedMapOf("__lib" to "login", "__act" to "account", "login" to ""),
    headers = emptyMap(),
  )

  fun topicPostInfo(fid: Int, action: String? = null, pid: String? = null, tid: String? = null, stid: String? = null): NgaRequest =
    post(
      path = "post.php",
      query = linkedMapOf<String, String>().apply {
        put("fid", fid.toString())
        put("lite", "js")
        action?.let { put("action", it) }
        pid?.let { put("pid", it) }
        tid?.let { put("tid", it) }
        stid?.let { put("stid", it) }
      },
    )

  fun topicPost(content: String, fid: Int, subject: String? = null, pid: String? = null, tid: String? = null, action: String? = null): NgaRequest =
    post(path = "post.php", body = topicPostBody(content = content, fid = fid, subject = subject, pid = pid, tid = tid, action = action))

  fun topicPostBody(
    content: String,
    fid: Int,
    subject: String? = null,
    pid: String? = null,
    tid: String? = null,
    action: String? = null,
    anonymous: Boolean = false,
    attachments: String? = null,
    attachmentsCheck: String? = null,
    stid: String? = null,
  ): NgaFormBody = formBody {
    addRaw("step", "2")
    addRaw("post_content", NgaEncoding.urlEncodeGbk(content))
    pid?.let { addRaw("pid", it) }
    tid?.let { addRaw("tid", it) }
    action?.let { addRaw("action", it) }
    subject?.let { addRaw("post_subject", NgaEncoding.urlEncodeGbk(it)) }
    addRaw("fid", fid.toString())
    if (anonymous) addRaw("anony", "1")
    if (!attachments.isNullOrBlank() && !attachmentsCheck.isNullOrBlank()) {
      addRaw("attachments", attachments)
      addRaw("attachments_check", attachmentsCheck)
    }
    stid?.let { addRaw("stid", it) }
  }

  fun newTopic(fid: Int, subject: String, content: String, attachments: List<TopicAttachment>, options: TopicPostOptions = TopicPostOptions()): NgaRequest =
    post("post.php", query = linkedMapOf("__output" to "8"), body = topicPostBody(
      content = content, fid = fid, subject = subject, action = "new",
      attachments = attachments.takeIf { it.isNotEmpty() }?.joinToString("", transform = { "%09" + NgaEncoding.urlEncodeGbk(it.id) }),
      attachmentsCheck = attachments.takeIf { it.isNotEmpty() }?.joinToString("", transform = { "%09" + NgaEncoding.urlEncodeGbk(it.check) }),
    ).let { body ->
      val mentions = Regex("\\[@([^]\\r\\n]{2,30})]").findAll(content)
        .map { it.groupValues[1].trim() }.filter(String::isNotBlank).distinct().take(5).joinToString("\t")
      NgaFormBody(body.fields + options.fields().map { (key, value) -> "$key=$value" } +
        if (mentions.isBlank()) emptyList() else listOf("mention=${NgaEncoding.urlEncodeGbk(mentions)}"))
    })

  fun topicCategories(fid: Int): NgaRequest = get("nuke.php", linkedMapOf("__lib" to "topic_key", "__act" to "get", "fid" to fid.toString(), "__output" to "8"))

  fun reply(tid: String, pid: String, content: String): NgaRequest =
    post("post.php", query = linkedMapOf("__output" to "8"), body = formBody {
      addRaw("action", "reply")
      addRaw("step", "2")
      addRaw("tid", tid)
      addRaw("pid", pid)
      addRaw("post_content", NgaEncoding.urlEncodeGbk(content))
    })

  fun attachmentUploadMetadata(fid: Int, auth: String, fileName: String): NgaRequest =
    NgaRequest(
      method = NgaHttpMethod.POST,
      url = "https://img8.nga.cn/attach.php",
      headers = commonHeaders() + ("Content-Type" to "multipart/form-data"),
      body = formBody {
        addRaw("attachment_file1_url_utf8_name", fileName)
        addRaw("fid", fid.toString())
        addRaw("auth", auth)
        addRaw("func", "upload")
        addRaw("v2", "1")
        addRaw("lite", "js")
        addRaw("attachment_file1_auto_size", "")
        addRaw("attachment_file1_watermark", "")
        addRaw("attachment_file1_dscp", "")
        addRaw("attachment_file1_img", "1")
        addRaw("origin_domain", "bbs.ngacn.cc")
      },
    )

  fun commentPost(fid: Int, tid: Int, pid: Int, comment: String, prefix: String? = null, anonymous: Boolean = false): NgaRequest =
    post(
      path = "post.php",
      body = formBody {
        addRaw("post_content", NgaEncoding.urlEncodeGbk((prefix.orEmpty()) + comment))
        addRaw("tid", tid.toString())
        addRaw("pid", pid.toString())
        addRaw("fid", fid.toString())
        addRaw("nojump", "1")
        addRaw("step", "2")
        addRaw("action", "reply")
        addRaw("comment", "1")
        addRaw("lite", "htmljs")
        if (anonymous) addRaw("anony", "1")
      },
    )

  fun favoriteAdd(tid: String, pid: String? = null): NgaRequest =
    post(
      path = "nuke.php",
      query = linkedMapOf("__lib" to "topic_favor", "lite" to "js", "noprefix" to "", "__act" to "topic_favor", "action" to "add", "tid" to tid)
        .apply { pid?.let { put("pid", it) } },
    )

  fun favoriteRemove(page: Int, tid: Int, pid: Int? = null): NgaRequest =
    post(
      path = "nuke.php",
      body = formBody {
        addRaw("__lib", "topic_favor")
        addRaw("__act", "topic_favor")
        addRaw("__output", "8")
        addRaw("action", "del")
        addRaw("page", page.toString())
        addRaw("tidarray", if (pid == null || pid == 0) tid.toString() else "${tid}_$pid")
      },
    )

  fun like(tid: Int, pid: Int = 0, support: Boolean): NgaRequest =
    post(
      path = "nuke.php",
      body = formBody {
        addRaw("__lib", "topic_recommend")
        addRaw("__act", "add")
        addRaw("raw", "3")
        addRaw("pid", pid.toString())
        addRaw("__output", "8")
        addRaw("value", if (support) "1" else "-1")
        addRaw("tid", tid.toString())
      },
    )

  fun report(fields: Map<String, String>): NgaRequest =
    post(path = "nuke.php", query = linkedMapOf("__lib" to "log_post", "__act" to "report"), body = formBody(fields))

  fun notifications(): NgaRequest = get("nuke.php", linkedMapOf("__lib" to "noti", "__output" to "8", "__act" to "get_all"))

  fun clearNotifications(): NgaRequest = post("nuke.php", linkedMapOf("__lib" to "noti", "raw" to "3", "__act" to "del"))

  fun checkIn(): NgaRequest = post("nuke.php", linkedMapOf("__lib" to "check_in", "__act" to "check_in", "lite" to "js"))

  fun checkInStatus(): NgaRequest = get("nuke.php", linkedMapOf("__lib" to "check_in", "__act" to "get_stat", "__output" to "8"))

  fun profile(params: Map<String, String>): NgaRequest =
    get(
      path = "nuke.php",
      query = linkedMapOf("__lib" to "ucp", "__act" to "get", "lite" to "js", "noprefix" to "").apply { putAll(params) },
      extraHeaders = mapOf("Referer" to "${session.normalizedBaseUrl}/nuke.php?func=ucp&lite=jsx&${params.toQueryString()}"),
    )

  fun signature(uid: String, sign: String): NgaRequest =
    post(
      path = "nuke.php",
      body = formBody {
        addRaw("__lib", "set_sign")
        addRaw("__act", "set")
        addRaw("raw", "3")
        addRaw("lite", "js")
        addRaw("charset", "gbk")
        addRaw("uid", uid)
        addRaw("sign", NgaEncoding.urlEncodeGbk(sign))
      },
    )

  fun blockWords(uid: String): NgaRequest =
    post(
      path = "nuke.php",
      body = formBody {
        addRaw("__lib", "ucp")
        addRaw("__act", "get_block_word")
        addRaw("__output", "8")
        addRaw("uid", uid)
      },
      extraHeaders = mapOf("Referer" to "${session.normalizedBaseUrl}/nuke.php?func=ucp&uid=$uid"),
    )

  fun updateBlockWords(blockedUsers: List<String>, blockedWords: List<String>): NgaRequest =
    post(
      path = "nuke.php",
      body = formBody {
        addRaw("__lib", "ucp")
        addRaw("__act", "set_block_word")
        addRaw("__output", "8")
        addRaw("data", NgaEncoding.urlEncodeGbk("1\r\n${blockedWords.joinToString(" ")}\r\n${blockedUsers.joinToString(" ")}"))
      },
      extraHeaders = mapOf(
        "Referer" to session.normalizedBaseUrl,
        "charset" to "GBK",
        "Host" to "bbs.nga.cn",
        "Origin" to session.normalizedBaseUrl,
        "content-type" to "application/x-www-form-urlencoded",
      ),
    )

  fun messageList(page: Int = 1): NgaRequest =
    get("nuke.php", linkedMapOf("__lib" to "message", "__act" to "message", "act" to "list", "lite" to "js", "page" to page.toString()))

  fun messageRead(mid: String, page: Int = 1): NgaRequest =
    get("nuke.php", linkedMapOf("__lib" to "message", "__act" to "message", "act" to "read", "lite" to "js", "mid" to mid, "page" to page.toString()))

  fun messagePost(action: String, mid: String, recipient: String, subject: String?, content: String): NgaRequest =
    post(
      path = "nuke.php",
      query = linkedMapOf("__lib" to "message", "__act" to "message", "lite" to "js", "charset" to "gbk", "act" to action),
      body = formBody {
        addRaw("mid", mid)
        addRaw("to", NgaEncoding.urlEncodeGbk(recipient.replace('，', ',')))
        subject?.takeIf { it.isNotBlank() }?.let { addRaw("subject", NgaEncoding.urlEncodeGbk(it)) }
        addRaw("content", NgaEncoding.urlEncodeGbk(content))
      },
    )

  fun subBoardOption(type: Int, parentFid: String, boardId: String, subscribe: Boolean): NgaRequest {
    val action = if (type == 1) {
      if (subscribe) "del" else "add"
    } else {
      if (subscribe) "add" else "del"
    }
    return NgaRequest(
      method = NgaHttpMethod.POST,
      url = "http://bbs.ngacn.cc/nuke.php",
      query = linkedMapOf("__lib" to "user_option", "__act" to "set", "raw" to "3", "type" to type.toString(), "__output" to "8", "fid" to parentFid, action to boardId),
      headers = commonHeaders(),
    )
  }

  fun vote(tid: Int, voteIds: List<Int>, settle: Boolean = false): NgaRequest =
    post(
      path = "nuke.php",
      query = linkedMapOf("__lib" to "vote", "raw" to "3", "lite" to "js", "__act" to if (settle) "settle" else "vote", "tid" to tid.toString(), "voteid" to voteIds.joinToString(",")),
    )

  fun avatarUploadMetadata(fileName: String): NgaRequest =
    NgaRequest(
      method = NgaHttpMethod.POST,
      url = "https://app.myauth.us/api/attach.php",
      headers = commonHeaders() + ("Content-Type" to "multipart/form-data"),
      body = formBody {
        addRaw("v2", "1")
        addRaw("attachment_file1_watermark", "")
        addRaw("attachment_file1_dscp", "")
        addRaw("attachment_file1_url_utf8_name", fileName)
        addRaw("fid", "-7")
        addRaw("func", "upload")
        addRaw("attachment_file1_img", "1")
        addRaw("origin_domain", "bbs.ngacn.cc")
        addRaw("lite", "js")
      },
    )

  fun avatarEdit(uid: String): NgaRequest = get("nuke.php", linkedMapOf(
    "__lib" to "set_avatar", "__act" to "get", "uid" to uid,
    "edit" to "1", "raw" to "3", "__output" to "8",
  ))

  fun saveUploadedAvatar(uid: String, avatar: String): NgaRequest = post(
    "nuke.php", body = formBody {
      addRaw("__lib", "set_avatar")
      addRaw("__act", "set")
      addRaw("uid", uid)
      addRaw("avatar", NgaEncoding.urlEncodeGbk(avatar))
      addRaw("raw", "3")
      addRaw("__output", "8")
    },
  )

  fun avatarChange(iconUrl: String, checksum: String? = null): NgaRequest =
    NgaRequest(
      method = NgaHttpMethod.POST,
      url = "https://nga.178.com/nuke.php",
      headers = commonHeaders(),
      body = formBody {
        addRaw("lite", "js")
        addRaw("noprefix", "")
        addRaw("func", "avatar")
        addRaw("icon", NgaEncoding.urlEncodeGbk(iconUrl))
        checksum?.let { addRaw("__ngaClientChecksum", it) }
      },
    )

  private fun get(path: String, query: Map<String, String> = emptyMap(), extraHeaders: Map<String, String> = emptyMap()): NgaRequest =
    NgaRequest(NgaHttpMethod.GET, "${session.normalizedBaseUrl}/$path", query, commonHeaders() + extraHeaders)

  private fun post(path: String, query: Map<String, String> = emptyMap(), body: NgaFormBody = NgaFormBody(), extraHeaders: Map<String, String> = emptyMap()): NgaRequest =
    NgaRequest(NgaHttpMethod.POST, "${session.normalizedBaseUrl}/$path", query, commonHeaders() + extraHeaders, body)

  private fun commonHeaders(includeCookie: Boolean = true): Map<String, String> =
    linkedMapOf<String, String>().apply {
      if (includeCookie) {
        session.cookie?.takeIf { it.isNotBlank() }?.let { put("Cookie", it) }
      }
      put("User-Agent", session.userAgent)
      put("X-User-Agent", "Nga_Official")
      put("Referer", session.normalizedBaseUrl)
    }

  private fun formBody(fields: Map<String, String>): NgaFormBody = formBody {
    fields.forEach { (key, value) -> addRaw(key, value) }
  }

  private fun formBody(builder: FormBodyBuilder.() -> Unit): NgaFormBody =
    FormBodyBuilder().apply(builder).build()

  private class FormBodyBuilder {
    private val fields = mutableListOf<String>()

    fun addRaw(key: String, value: String) {
      fields += "$key=$value"
    }

    fun build(): NgaFormBody = NgaFormBody(fields.toList())
  }
}

private fun Map<String, String>.toQueryString(): String =
  entries.joinToString("&") { (key, value) -> "$key=$value" }
