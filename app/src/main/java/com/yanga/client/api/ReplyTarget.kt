package com.yanga.client.api

enum class ReplyMode(val label: String) { Reply("普通回复"), Quote("引用回复"), Comment("贴条评论") }

data class ReplyTarget(
  val tid: String, val pid: String = "0", val label: String = "回复主题",
  val quote: String = "", val mode: ReplyMode = ReplyMode.Reply,
) : java.io.Serializable {
  val action: String get() = if (mode == ReplyMode.Quote) "quote" else "reply"
}
