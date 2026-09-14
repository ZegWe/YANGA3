package com.yanga.client.api

enum class NgaPersonalTopicKind(val label: String) { Topics("我的主题"), Replies("我的回复"), Favorites("默认收藏夹") }

data class NgaPersonalTopic(val tid: String, val pid: String?, val title: String, val excerpt: String)
data class NgaPersonalTopicPage(val items: List<NgaPersonalTopic>, val hasNextPage: Boolean)

object NgaPersonalTopicParser {
  fun parse(raw: String): NgaPersonalTopicPage {
    val root = ngaJsonRoot(raw)
    root.opt("error")?.let { throw NgaApiException(it.toString()) }
    val data = root.objectValue("data") ?: root
    if (!data.has("__T") && !data.has("topics")) throw NgaApiException("无法读取列表，请重新登录或重试")
    val items = data.objectListValue("__T", "topics").mapNotNull { item ->
      val tid = item.stringValue("tid")
      if (tid.toLongOrNull()?.let { it > 0 } != true) return@mapNotNull null
      NgaPersonalTopic(tid, item.nullableStringValue("pid")?.takeUnless { it == "0" },
        NgaDisplayText.singleLine(item.stringValue("subject", "title")).ifBlank { "主题 $tid" },
        NgaDisplayText.singleLine(item.stringValue("content", "postcontent")))
    }
    // Some NGA list responses have no reliable page count. Allow requesting the
    // next page until an empty page confirms the end, rather than inventing totals.
    val next = if (data.has("has_next_page")) data.booleanValue("has_next_page") else items.isNotEmpty()
    return NgaPersonalTopicPage(items, next)
  }
}
