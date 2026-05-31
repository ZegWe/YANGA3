package com.yanga.client.api

import org.json.JSONArray
import org.json.JSONObject

object NgaSubBoardFilterParser {
  fun parseBlockedIds(raw: String): Set<String> {
    val root = ngaJsonRoot(raw)
    val data = root.objectValue("data") ?: root
    val payloads =
      listOfNotNull(
        data,
        data.objectValue("0"),
        data.objectValue("option"),
      )
    for (payload in payloads) {
      val blocked = parseFromPayload(payload)
      if (blocked.isNotEmpty()) return blocked
    }
    return emptySet()
  }

  private fun parseFromPayload(payload: JSONObject): Set<String> {
    val candidates =
      listOfNotNull(
        payload.opt("block_tid"),
        payload.opt("add_to_block_tids"),
        payload.opt("block_tids"),
        payload.opt("blocked"),
        payload.opt("list"),
        payload.opt("result"),
        payload.objectValue("add_to_block_tids"),
      )
    return candidates.flatMap { it.toIdList() }.filter { it.isNotBlank() }.toSet()
  }

  private fun Any?.toIdList(): List<String> =
    when (this) {
      null, JSONObject.NULL -> emptyList()
      is JSONArray ->
        buildList {
          for (index in 0 until length()) {
            opt(index)?.toString()?.takeIf { it.isNotBlank() }?.let(::add)
          }
        }
      is JSONObject -> {
        val values =
          keys().asSequence().mapNotNull { key ->
            opt(key)?.toString()?.takeIf { it.isNotBlank() }
          }.toList()
        if (values.isNotEmpty()) values else emptyList()
      }
      is Number -> listOf(toString())
      is String ->
        if (isBlank()) {
          emptyList()
        } else if (startsWith("[") || startsWith("{")) {
          runCatching { JSONArray(this).let { array -> array.toIdListFromArray() } }.getOrElse { listOf(this) }
        } else {
          split(',', ' ', ';').filter { it.isNotBlank() }
        }
      else -> listOf(toString()).filter { it.isNotBlank() }
    }

  private fun JSONArray.toIdListFromArray(): List<String> =
    buildList {
      for (index in 0 until length()) {
        opt(index)?.toString()?.takeIf { it.isNotBlank() }?.let(::add)
      }
    }
}
