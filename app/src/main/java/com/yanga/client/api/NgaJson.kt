package com.yanga.client.api

import org.json.JSONArray
import org.json.JSONObject

internal fun ngaJsonRoot(raw: String): JSONObject =
  JSONObject(NgaResponseNormalizer.normalize(raw))

internal fun JSONObject.objectValue(vararg keys: String): JSONObject? =
  firstValue(*keys) as? JSONObject

internal fun JSONObject.arrayValue(vararg keys: String): JSONArray? =
  firstValue(*keys) as? JSONArray

internal fun JSONObject.stringValue(vararg keys: String): String =
  firstValue(*keys)?.takeUnless { it == JSONObject.NULL }?.toString().orEmpty()

internal fun JSONObject.nullableStringValue(vararg keys: String): String? =
  stringValue(*keys).takeIf { it.isNotBlank() }

internal fun JSONObject.intValue(vararg keys: String): Int =
  when (val value = firstValue(*keys)) {
    is Number -> value.toInt()
    is String -> value.toIntOrNull() ?: 0
    is Boolean -> if (value) 1 else 0
    else -> 0
  }

internal fun JSONObject.nullableIntValue(vararg keys: String): Int? =
  when (val value = firstValue(*keys)) {
    is Number -> value.toInt()
    is String -> value.toIntOrNull()
    is Boolean -> if (value) 1 else 0
    else -> null
  }

internal fun JSONObject.longValue(vararg keys: String): Long =
  when (val value = firstValue(*keys)) {
    is Number -> value.toLong()
    is String -> value.toLongOrNull() ?: 0L
    is Boolean -> if (value) 1L else 0L
    else -> 0L
  }

internal fun JSONObject.nullableLongValue(vararg keys: String): Long? =
  longValue(*keys).takeIf { it > 0L }

internal fun JSONObject.booleanValue(vararg keys: String): Boolean =
  when (val value = firstValue(*keys)) {
    is Boolean -> value
    is Number -> value.toInt() != 0
    is String -> value == "1" || value.equals("true", ignoreCase = true)
    else -> false
  }

internal fun JSONObject.objectListValue(vararg keys: String): List<JSONObject> =
  firstValue(*keys).toObjectList()

internal fun Any?.toObjectList(): List<JSONObject> =
  when (this) {
    is JSONArray -> (0 until length()).mapNotNull { index -> optJSONObject(index) }
    is JSONObject -> keys().asSequence()
      .toList()
      .sortedWith(compareBy<String> { it.toIntOrNull() ?: Int.MAX_VALUE }.thenBy { it })
      .mapNotNull { key -> optJSONObject(key) }
    else -> emptyList()
  }

private fun JSONObject.firstValue(vararg keys: String): Any? =
  keys.firstNotNullOfOrNull { key ->
    opt(key)?.takeUnless { it == JSONObject.NULL }
  }
