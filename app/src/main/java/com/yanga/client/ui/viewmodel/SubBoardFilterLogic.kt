package com.yanga.client.ui

import com.yanga.client.api.NgaSubBoard
import com.yanga.client.data.SubBoardVisibilityChange

internal object SubBoardFilterLogic {
  const val QUICK_CHIP_LIMIT = 3

  fun validateSelection(selectedIds: Set<String>, options: List<SubBoardOption>): Set<String> {
    if (options.isEmpty()) return emptySet()
    if (selectedIds.isEmpty()) return emptySet()
    val optionIds = options.map { it.id }.toSet()
    val valid = selectedIds.intersect(optionIds)
    return when {
      valid.isEmpty() -> emptySet()
      valid.size == options.size -> emptySet()
      else -> valid
    }
  }

  fun toggleSelection(selectedIds: Set<String>, optionId: String, options: List<SubBoardOption>): Set<String> {
    if (options.isEmpty()) return emptySet()
    val allSelected = selectedIds.isEmpty()
    val next =
      when {
        allSelected -> setOf(optionId)
        optionId in selectedIds -> {
          val reduced = selectedIds - optionId
          if (reduced.isEmpty()) emptySet() else reduced
        }
        else -> selectedIds + optionId
      }
    return validateSelection(next, options)
  }

  fun setSelectionEnabled(
    selectedIds: Set<String>,
    optionId: String,
    enabled: Boolean,
    options: List<SubBoardOption>,
  ): Set<String> {
    if (options.isEmpty()) return emptySet()
    val allIds = options.map { it.id }.toSet()
    val base = if (selectedIds.isEmpty()) allIds else selectedIds
    val next =
      if (enabled) {
        base + optionId
      } else {
        (base - optionId).ifEmpty { allIds }
      }
    return validateSelection(next, options)
  }

  fun blockedSubscribeIds(subBoards: List<NgaSubBoard>, selectedIds: Set<String>): Set<String> {
    if (selectedIds.isEmpty()) return emptySet()
    return subBoards
      .filter { it.id !in selectedIds }
      .mapNotNull { it.subscribeId?.takeIf { id -> id.isNotBlank() } }
      .toSet()
  }

  fun selectedIdsFromBlocked(subBoards: List<NgaSubBoard>, blockedSubscribeIds: Set<String>): Set<String> {
    if (subBoards.isEmpty()) return emptySet()
    if (blockedSubscribeIds.isEmpty()) return emptySet()
    val options = subBoards.map { it.toOption() }
    val selected =
      subBoards
        .filter { board -> board.subscribeId !in blockedSubscribeIds }
        .map { it.id }
        .toSet()
    return validateSelection(selected, options)
  }

  fun visibilityChanges(
    subBoards: List<NgaSubBoard>,
    previousBlockedSubscribeIds: Set<String>,
    nextBlockedSubscribeIds: Set<String>,
  ): List<SubBoardVisibilityChange> {
    val bySubscribeId = subBoards.mapNotNull { board -> board.subscribeId?.let { it to board } }.toMap()
    val toShow = previousBlockedSubscribeIds - nextBlockedSubscribeIds
    val toHide = nextBlockedSubscribeIds - previousBlockedSubscribeIds
    return buildList {
      toShow.forEach { subscribeId ->
        bySubscribeId[subscribeId]?.let { board ->
          add(SubBoardVisibilityChange(board = board, visible = true))
        }
      }
      toHide.forEach { subscribeId ->
        bySubscribeId[subscribeId]?.let { board ->
          add(SubBoardVisibilityChange(board = board, visible = false))
        }
      }
    }
  }

  fun canSyncWithServer(subBoards: List<NgaSubBoard>): Boolean =
    subBoards.isNotEmpty() && subBoards.all { !it.subscribeId.isNullOrBlank() }

  fun isSubBoardEnabled(optionId: String, selectedIds: Set<String>): Boolean =
    selectedIds.isEmpty() || optionId in selectedIds

  fun selectedFidGroup(selectedIds: Set<String>, subBoards: List<NgaSubBoard>): String? {
    if (selectedIds.isEmpty()) return null
    val selected =
      subBoards
        .filter { it.id in selectedIds }
        .map { it.id }
        .filter { it.isNotBlank() }
        .distinct()
    if (selected.isEmpty()) return null
    return selected.joinToString(",")
  }

  fun visibleQuickOptions(
    options: List<SubBoardOption>,
    selectedIds: Set<String>,
    limit: Int = QUICK_CHIP_LIMIT,
  ): List<SubBoardOption> {
    if (options.isEmpty()) return emptyList()
    if (selectedIds.isEmpty()) return options.take(limit)

    val selectedOptions = options.filter { it.id in selectedIds }
    val unselectedOptions = options.filter { it.id !in selectedIds }
    return (selectedOptions + unselectedOptions)
      .distinctBy { it.id }
      .take(maxOf(limit, selectedOptions.size))
  }
}
