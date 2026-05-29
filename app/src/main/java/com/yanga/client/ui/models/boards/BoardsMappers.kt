package com.yanga.client.ui

import com.yanga.client.api.NgaBoardCategory
import com.yanga.client.api.NgaBoardGroup
import com.yanga.client.api.NgaBoardSection
import com.yanga.client.api.NgaBoardSummary

internal fun NgaBoardSummary.toPreview(): BoardPreview =
  BoardPreview(
    id = boardId,
    name = name,
    metadata = description ?: "fid: $boardId",
    marker = name.initialOrFallback(),
    badge = unreadCount.takeIf { it != null && it > 0 }?.toString(),
    iconUrl = iconUrl,
  )

internal fun NgaBoardSummary.toPreview(category: String): BoardPreview = toPreview().copy(category = category)

internal fun NgaBoardGroup.toPreview(): BoardGroupPreview =
  BoardGroupPreview(id = id, name = name, boards = boards.map { it.toPreview(category = name) })

internal fun NgaBoardGroup.toPreview(categoryPrefix: String): BoardGroupPreview =
  BoardGroupPreview(id = id, name = name, boards = boards.map { it.toPreview(category = "$categoryPrefix / $name") })

internal fun NgaBoardSection.toPreview(): BoardSectionPreview =
  BoardSectionPreview(id = id, name = name, groups = groups.map { group -> group.toPreview(categoryPrefix = name) })

internal fun NgaBoardCategory.toPreview(): BoardPreview {
  val unreadCount = boards.sumOf { it.unreadCount ?: 0 }
  return BoardPreview(
    id = id,
    name = name,
    metadata = "${boards.size} boards",
    marker = name.initialOrFallback(),
    badge = unreadCount.takeIf { it > 0 }?.toString(),
  )
}
