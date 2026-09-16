package com.yanga.client.api

data class NgaPollOption(val id: Int, val label: String, val votes: Long?, val stake: Long? = null)

data class NgaPoll(
  val tid: String,
  val options: List<NgaPollOption>,
  val maxSelections: Int,
  val endsAt: Long? = null,
  val participants: Long? = null,
  val isBet: Boolean = false,
  val settled: Boolean = false,
) {
  val totalVotes: Long? get() = if (options.all { it.votes != null }) options.sumOf { it.votes!! } else null
  fun isClosed(now: Long = System.currentTimeMillis() / 1000): Boolean = settled || (endsAt?.let { it <= now } == true)
  fun validationError(ids: List<Int>, now: Long = System.currentTimeMillis() / 1000): String? = when {
    isBet -> "此类型请在网页中查看"
    isClosed(now) -> "投票已结束"
    ids.isEmpty() -> "请至少选择一项"
    ids.distinct().size != ids.size || ids.any { id -> options.none { it.id == id } } -> "投票选项无效"
    ids.size > maxSelections -> "最多选择 $maxSelections 项"
    else -> null
  }
}

object NgaPollParser {
  fun parse(tid: String, raw: String): NgaPoll? {
    if (raw.isBlank()) return null
    val fields = raw.split('~').chunked(2).filter { it.size == 2 }.associate { it[0] to it[1] }
    val options = fields.mapNotNull { (key, label) ->
      val id = key.toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
      val counts = fields["_$key"]?.split(',').orEmpty()
      NgaPollOption(id, NgaDisplayText.singleLine(label), counts.getOrNull(0)?.toLongOrNull()?.coerceAtLeast(0),
        counts.getOrNull(1)?.toLongOrNull()?.coerceAtLeast(0))
    }
    if (options.isEmpty()) return null
    return NgaPoll(
      tid = tid,
      options = options,
      maxSelections = (fields["max_select"]?.toIntOrNull() ?: 1).coerceIn(1, options.size),
      endsAt = fields["end"]?.toLongOrNull()?.takeIf { it > 0 },
      participants = options.mapNotNull { fields["_${it.id}"]?.split(',')?.getOrNull(2)?.toLongOrNull() }
        .maxOrNull()?.takeIf { it > 0 },
      isBet = fields["type"] == "1",
      settled = !fields["done"].isNullOrBlank() && fields["done"] != "0",
    )
  }

  fun requireSuccessfulSubmission(raw: String) {
    val root = ngaJsonRoot(raw)
    val error = root.opt("error")
    if (error != null && error != org.json.JSONObject.NULL) {
      throw NgaApiException((error as? org.json.JSONObject)?.optString("0")?.takeIf { it.isNotBlank() } ?: error.toString())
    }
    val message = root.optJSONObject("data")?.optString("0").orEmpty()
    if (!message.startsWith("操作成功") && !message.startsWith("投票成功") && message != "成功") {
      throw NgaApiException(message.ifBlank { "未能确认投票结果，请刷新后检查" })
    }
  }
}
