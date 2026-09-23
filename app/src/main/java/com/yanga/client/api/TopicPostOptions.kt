package com.yanga.client.api

import org.json.JSONArray
import org.json.JSONObject

enum class TopicVoteType(val label: String, val wire: String) { None("无投票", ""), Poll("投票", "0"), Bet("投注铜币", "1"), Score("评分", "2") }

data class TopicPostOptions(
  val anonymous: Boolean = false,
  val hidden: Boolean = false,
  val selfReply: Boolean = false,
  val replyOnce: Boolean = false,
  val anonymousReplies: Boolean = false,
  val createCollection: Boolean = false,
  val voteType: TopicVoteType = TopicVoteType.None,
  val voteItems: String = "",
  val voteMax: String = "1",
  val voteHours: String = "",
  val voteMin: String = "1",
  val voteBetMax: String = "10",
  val voteReputation: String = "",
  val voteVisibility: Int = 0,
) : java.io.Serializable {
  fun validationError(moderator: Boolean = false): String? {
    if ((anonymousReplies || voteType == TopicVoteType.Bet || voteType == TopicVoteType.Score) && !moderator) return "此选项需要版主权限"
    if (voteType == TopicVoteType.None) return null
    val groups = voteItems.trim().split(Regex("(?m)^===.*$"))
      .map { group -> group.lines().map(String::trim).filter(String::isNotBlank) }.filter { it.isNotEmpty() }
    if (groups.isEmpty() || groups.any { it.size < if (voteType == TopicVoteType.Score) 1 else 2 }) return "每组投票请至少填写两个选项，评分至少一项"
    if (voteType != TopicVoteType.Score && (voteMax.toIntOrNull() == null || voteMax.toInt() < 0 || (voteMax.toInt() > 0 && groups.any { it.size < voteMax.toInt() }))) return "最多可选项数应为 0（不限）或不超过每组选项数量"
    if (voteHours.isNotBlank() && (voteHours.toIntOrNull()?.let { it > 0 } != true)) return "结束时间请填写正整数小时"
    if ((voteVisibility != 0 || voteType == TopicVoteType.Bet) && voteHours.isBlank()) return "当前投票类型或结果可见性需要设置结束时间"
    if (voteReputation.isNotBlank() && voteReputation.toIntOrNull()?.let { it in -21000..21000 } != true) return "声望限制应在 -21000 到 21000 之间"
    if (voteType == TopicVoteType.Bet && (voteMin.toIntOrNull()?.let { it > 0 } != true || voteBetMax.toIntOrNull()?.let { it >= (voteMin.toIntOrNull() ?: 0) } != true)) return "投注范围无效"
    return null
  }

  fun fields(): Map<String, String> = buildMap {
    if (anonymous) put("anony", "1")
    if (hidden) put("hidden", "1")
    if (selfReply) put("self_reply", "1")
    if (replyOnce) put("tpic_misc_bit1", "1073741824") // Official spelling is tpic, not topic.
    if (anonymousReplies) put("reply_anony", "1")
    if (createCollection) put("stid", "-1")
    if (voteType != TopicVoteType.None) {
      put("newvote", NgaEncoding.urlEncodeGbk(voteItems.trim()))
      put("newvote_type", voteType.wire)
      if (voteType != TopicVoteType.Score) put("newvote_max", voteMax)
      if (voteHours.isNotBlank()) put("newvote_end", voteHours)
      if (voteReputation.isNotBlank()) put("newvote_limit", voteReputation)
      if (voteType != TopicVoteType.Poll) {
        put("newvote_betmin", if (voteType == TopicVoteType.Score) "1" else voteMin)
        put("newvote_betmax", if (voteType == TopicVoteType.Score) "10" else voteBetMax)
      }
      when (voteVisibility) { 1 -> put("post_opt", "2048"); 2 -> put("post_opt", "4096") }
    }
  }
}

data class TopicUploadOptions(val compression: String = "1", val watermark: String = "", val description: String = "") : java.io.Serializable
data class TopicPostPreparation(
  val categories: List<String> = emptyList(), val categoryRequired: Boolean = false,
  val moderator: Boolean = false, val warning: String = "", val defaultSubject: String = "",
)

object TopicPostPreparationParser {
  fun parse(raw: String): TopicPostPreparation {
    val root = ngaJsonRoot(raw)
    val error = root.opt("error")
    if (error != null && error !in listOf(JSONObject.NULL, 0, false, "")) throw NgaApiException(
      (error as? JSONObject)?.optString("0")?.takeIf(String::isNotBlank) ?: error.toString())
    val data = root.optJSONObject("data") ?: throw NgaApiException("无法读取发帖权限")
    return TopicPostPreparation(
      categoryRequired = ((data.optJSONObject("__F")?.optInt("bit_data") ?: 0) and 256) != 0,
      moderator = (data.optInt("if_moderator") and 14) != 0,
      warning = data.optString("warning").replace(Regex("<[^>]+>"), " ").trim(),
      defaultSubject = data.optString("subject"),
    )
  }

  fun categories(raw: String): List<String> {
    val root = ngaJsonRoot(raw)
    if (root.has("error")) throw NgaApiException("主题分类加载失败")
    fun item(value: Any?, key: Int): Any? = when (value) { is JSONObject -> value.opt(key.toString()); is JSONArray -> value.opt(key); else -> null }
    var container = root.opt("data")
    if (item(item(container, 0), 0) is JSONObject || item(item(container, 0), 0) is JSONArray) container = item(container, 0)
    val result = mutableListOf<String>()
    for (index in 0 until 200) {
      val row = item(container, index) ?: break
      val label = (item(row, 0) as? String)?.trim() ?: continue
      if (label.isNotEmpty()) result += label
    }
    return result.distinct()
  }
}
