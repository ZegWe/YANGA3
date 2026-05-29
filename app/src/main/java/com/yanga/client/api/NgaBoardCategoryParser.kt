package com.yanga.client.api

import org.json.JSONObject

object NgaBoardCategoryParser {
  fun parseSections(raw: String): List<NgaBoardSection> {
    val root = ngaJsonRoot(raw)
    if (root.has("error")) return emptyList()

    val data = root.objectValue("data") ?: root.objectValue("result") ?: root
    val resultSections = data.opt("result").toObjectList().mapNotNull { it.toBoardSection() }
    if (resultSections.isNotEmpty()) return resultSections

    val legacyCategories = parse(raw)
    if (legacyCategories.isNotEmpty()) {
      return legacyCategories.map { category ->
        NgaBoardSection(
          id = category.id,
          name = category.name,
          groups =
            listOf(
              NgaBoardGroup(
                id = category.id,
                name = category.name,
                boards = category.boards,
              ),
            ),
        )
      }
    }

    return emptyList()
  }

  fun parse(raw: String): List<NgaBoardCategory> {
    val root = ngaJsonRoot(raw)
    
    // Handle error response
    if (root.has("error")) {
      return emptyList()
    }

    val data = root.objectValue("data") ?: root.objectValue("result") ?: root

    val groupedCategories = data.opt("result").toObjectList().flatMap { it.groupCategories() }
    if (groupedCategories.isNotEmpty()) return groupedCategories
    
    // NGA forum_all often returns a root container with fid 0
    val firstObj = data.toObjectList().firstOrNull()
    if (firstObj != null && (firstObj.stringValue("fid") == "0" || firstObj.stringValue("id") == "0")) {
      val sub = mutableListOf<JSONObject>()
      listOf("sub", "subs", "children", "forums", "list").forEach { key ->
        sub.addAll(firstObj.opt(key).toObjectList())
      }
      if (sub.isNotEmpty()) {
        return sub.mapNotNull { it.toBoardCategory() }
      }
    }
    
    val categoryList = mutableListOf<JSONObject>()
    listOf("categories", "category", "list", "result", "data").forEach { key ->
      val list = data.opt(key).toObjectList()
      if (list.isNotEmpty()) categoryList.addAll(list)
    }

    if (categoryList.isNotEmpty()) {
      val result = categoryList.mapNotNull { it.toBoardCategory() }
      if (result.isNotEmpty()) return result
    }
    
    return data.toObjectList().mapNotNull { it.toBoardCategory() }
  }

  fun parseBoards(raw: String): List<NgaBoardSummary> {
    val categories = parse(raw)
    val categoryBoards = categories.flatMap { it.boards }
    if (categoryBoards.isNotEmpty()) return categoryBoards.distinctBy { it.boardId }

    val root = ngaJsonRoot(raw)
    if (root.has("error")) return emptyList()
    val data = root.objectValue("data") ?: root.objectValue("result") ?: root
    val directBoards = mutableListOf<JSONObject>()
    listOf("boards", "forums", "forum", "children", "child", "list", "sub", "subs", "data", "result").forEach { key ->
      directBoards.addAll(data.opt(key).toObjectList())
    }
    if (directBoards.isEmpty()) directBoards.addAll(data.toObjectList())

    return directBoards.mapNotNull { it.toBoardSummary() }
      .filter { it.boardId.isNotBlank() && it.boardId != "0" }
      .distinctBy { it.boardId }
  }

  private fun JSONObject.toBoardSection(): NgaBoardSection? {
    val id = stringValue("id", "cid", "category_id", "fid")
    val name = stringValue("name", "title")
    val groups =
      opt("groups").toObjectList().mapNotNull { group ->
        val groupId = group.stringValue("id", "cid", "category_id", "fid")
        val groupName = group.stringValue("name", "title")
        val boards = mutableListOf<NgaBoardSummary>()
        collectBoards(group, boards)
        if (groupId.isBlank() && groupName.isBlank() && boards.isEmpty()) {
          null
        } else {
          NgaBoardGroup(
            id = groupId,
            name = groupName,
            boards = boards.distinctBy { it.boardId },
          )
        }
      }

    if (id.isBlank() && name.isBlank() && groups.isEmpty()) return null
    return NgaBoardSection(id = id, name = name, groups = groups)
  }

  private fun JSONObject.toBoardCategory(): NgaBoardCategory? {
    val id = stringValue("id", "cid", "category_id", "fid")
    val name = stringValue("name", "title")
    
    val boards = mutableListOf<NgaBoardSummary>()
    collectBoards(this, boards)

    if (id.isBlank() && name.isBlank() && boards.isEmpty()) return null

    return NgaBoardCategory(
      id = id,
      name = name,
      boards = boards.distinctBy { it.boardId },
    )
  }

  private fun JSONObject.groupCategories(): List<NgaBoardCategory> =
    opt("groups").toObjectList().mapNotNull { it.toBoardCategory() }

  private fun collectBoards(obj: JSONObject, out: MutableList<NgaBoardSummary>) {
    val boardsList = mutableListOf<JSONObject>()
    listOf("boards", "forums", "forum", "children", "child", "list", "sub", "subs").forEach { key ->
      boardsList.addAll(obj.opt(key).toObjectList())
    }
    
    for (boardObj in boardsList) {
      val summary = boardObj.toBoardSummary()
      if (summary != null) {
        // If it has a real fid, it's a board
        if (summary.boardId.isNotBlank() && summary.boardId != "0") {
          out.add(summary)
        }
        // Also check if this board has sub-boards
        collectBoards(boardObj, out)
      }
    }
  }

  private fun JSONObject.toBoardSummary(): NgaBoardSummary? {
    val boardId = stringValue("fid", "id", "board_id", "boardId")
    val name = stringValue("name", "title")
    if (boardId.isBlank() && name.isBlank()) return null

    return NgaBoardSummary(
      boardId = boardId,
      name = name,
      description = nullableStringValue("description", "desc"),
      todayTopicCount = nullableIntValue("todayposts", "today_topic_count", "todayTopicCount"),
      unreadCount = nullableIntValue("unread", "unread_count", "unreadCount"),
      isSubscribed = booleanValue("subscribed", "is_subscribed", "isSubscribed"),
      iconUrl = boardIconUrl(boardId),
    )
  }

  private fun JSONObject.boardIconUrl(boardId: String): String? {
    val stid = intValue("stid").takeIf { it > 0 }
    if (stid != null) return NgaStaticUrls.boardIconByStid(stid)

    val explicitIcon = nullableStringValue("icon_url", "icon")
    if (explicitIcon?.startsWith("http") == true) return explicitIcon

    return boardId.toIntOrNull()?.takeIf { it != 0 }?.let(NgaStaticUrls::boardIcon)
  }
}
