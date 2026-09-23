package com.yanga.client.ui.content

import kotlin.math.ceil

/** Reproduces NGA's seeded dice sequence for an already published post. */
data class PostDiceContext(val authorId: String, val tid: String, val pid: String)

internal object PostDiceRolls {
  private val diceTag = Regex("\\[dice](.*?)\\[/dice]", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
  private val scopeTag = Regex("\\[(/?)(collapse|code)(?:=[^]]*)?]", RegexOption.IGNORE_CASE)
  private val expression = Regex("(?:\\d*[dD]\\d+|\\d+)(?:\\+(?:\\d*[dD]\\d+|\\d+))*")

  fun render(content: String, context: PostDiceContext?): String {
    val author = context?.authorId?.toLongOrNull()?.takeIf { it >= 0 } ?: return content
    val tid = context.tid.toLongOrNull()?.takeIf { it > 0 } ?: return content
    val pid = context.pid.toLongOrNull()?.takeIf { it >= 0 } ?: return content
    val scopes = scopeTag.findAll(content).iterator()
    var nextScope = if (scopes.hasNext()) scopes.next() else null
    val stack = mutableListOf<String>()
    val dice = buildList {
      for (match in diceTag.findAll(content)) {
        while (nextScope?.range?.first?.let { it < match.range.first } == true) {
          val tag = nextScope ?: break
          val name = tag.groupValues[2].lowercase()
          if (tag.groupValues[1].isEmpty()) stack += name
          else if (stack.lastOrNull() == name) stack.removeAt(stack.lastIndex)
          nextScope = if (scopes.hasNext()) scopes.next() else null
        }
        val raw = match.groupValues[1].trim()
        if ("code" !in stack && expression.matches(raw)) add(RollTag(match, raw, "collapse" in stack))
      }
    }
    if (dice.isEmpty()) return content
    var seed = (author + tid + pid + if (dice.all { it.collapsed }) 1 else 0).toDouble()
    val replacements = mutableMapOf<Int, String>()
    for (tag in dice.sortedWith(compareBy<RollTag> { it.collapsed }.thenBy { it.match.range.first })) {
      val parsed = tag.expression.split('+').map { term ->
        val sidesAt = term.indexOfFirst { it == 'd' || it == 'D' }
        if (sidesAt < 0) ParsedTerm(0, 0, term.toIntOrNull())
        else ParsedTerm(term.substring(0, sidesAt).ifBlank { "1" }.toIntOrNull(),
          term.substring(sidesAt + 1).toIntOrNull(), null)
      }
      if (parsed.any { (count, sides, constant) ->
          if (constant != null) constant < 0 || constant > 1_000_000
          else count == null || sides == null || count !in 1..10 || sides !in 1..100_000
        } || parsed.sumOf { it.count ?: 0 } > 10) continue
      var total = 0
      val detail = mutableListOf<String>()
      for (term in parsed) {
        if (term.constant != null) { total += term.constant; detail += term.constant.toString() }
        else repeat(term.count!!) {
          seed = (seed * 9301 + 49297) % 233280
          val rolled = ceil(seed / 233280.0 * term.sides!!).toInt().coerceAtLeast(1)
          total += rolled
          detail += "d${term.sides}($rolled)"
        }
      }
      replacements[tag.match.range.first] = "🎲 ${tag.expression} = ${detail.joinToString(" + ")} = $total"
    }
    if (replacements.isEmpty()) return content
    return buildString {
      var cursor = 0
      for (tag in dice.sortedBy { it.match.range.first }) {
        append(content, cursor, tag.match.range.first)
        append(replacements[tag.match.range.first] ?: tag.match.value)
        cursor = tag.match.range.last + 1
      }
      append(content, cursor, content.length)
    }
  }

  private data class RollTag(val match: MatchResult, val expression: String, val collapsed: Boolean)
  private data class ParsedTerm(val count: Int?, val sides: Int?, val constant: Int?)
}
