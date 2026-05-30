package com.yanga.client.data.boards

import com.yanga.client.api.NgaBoardGroup
import com.yanga.client.api.NgaBoardSection
import com.yanga.client.api.NgaBoardSummary
import org.json.JSONArray
import org.json.JSONObject

/** Normalized board-directory JSON cached under [LocalBoardListStore]. */
object BoardListJsonCodec {
  fun encode(sections: List<NgaBoardSection>): String = sections.toJsonArray().toString(2)

  fun decode(json: String): List<NgaBoardSection> = LocalBoardListParser.parse(json)

  private fun List<NgaBoardSection>.toJsonArray(): JSONArray {
    val array = JSONArray()
    forEach { section ->
      array.put(
        JSONObject()
          .put("id", section.id.ifBlank { section.name })
          .put("name", section.name)
          .put("groups", section.groups.toGroupJsonArray()),
      )
    }
    return array
  }

  private fun List<NgaBoardGroup>.toGroupJsonArray(): JSONArray {
    val array = JSONArray()
    forEach { group ->
      array.put(
        JSONObject()
          .put("id", group.id.ifBlank { group.name })
          .put("name", group.name)
          .put("boards", group.boards.toBoardJsonArray()),
      )
    }
    return array
  }

  private fun List<NgaBoardSummary>.toBoardJsonArray(): JSONArray {
    val array = JSONArray()
    forEach { board ->
      array.put(
        JSONObject()
          .put("id", board.boardId)
          .put("name", board.name)
          .put("description", board.description.orEmpty())
          .apply { board.todayTopicCount?.let { put("todayTopicCount", it) } }
          .apply { board.unreadCount?.let { put("unreadCount", it) } }
          .put("isSubscribed", board.isSubscribed)
          .put("iconUrl", board.iconUrl.orEmpty()),
      )
    }
    return array
  }
}
