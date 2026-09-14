package com.yanga.client.api

/** Official js_read.postTxtColor / js_default.modInfo rules (excluding item overrides). */
object NgaPostColors {
  fun color(memberId: Int, moderator: Boolean): String? = when (memberId) {
    3, 86 -> "purple"
    4, 77, 90, 83 -> "teal"
    else -> if (moderator) "blue" else null
  }

  fun moderators(html: String, nowSeconds: Long = System.currentTimeMillis() / 1000): Set<String> {
    val args = Regex("""commonui\.postArg\.setDefault\([^\n]+""").find(html)?.value ?: return emptySet()
    // First three quoted arguments are punished users, visits and the moderator list.
    val list = Regex("\"([^\"]*)\"").findAll(args).drop(2).firstOrNull()?.groupValues?.get(1) ?: return emptySet()
    val result = mutableSetOf<String>()
    var temporary: Long? = null
    for (token in Regex("""\d+t?""").findAll(list).map { it.value }) {
      if (token.endsWith('t')) temporary = token.dropLast(1).toLongOrNull()
      else {
        val expiry = temporary
        if (expiry == null || (expiry >= nowSeconds && expiry and 1L != 0L)) result += token
        if (expiry == null || expiry >= nowSeconds) temporary = null
      }
    }
    return result
  }
}
