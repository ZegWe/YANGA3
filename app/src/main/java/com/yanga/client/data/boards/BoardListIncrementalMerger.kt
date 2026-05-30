package com.yanga.client.data.boards

import com.yanga.client.api.NgaBoardCategoryParser
import com.yanga.client.api.NgaBoardGroup
import com.yanga.client.api.NgaBoardSection
import com.yanga.client.api.NgaBoardSummary

internal object BoardListIncrementalMerger {
  private val TARGET_SECTION_IDS = setOf("other", "wow", "company")

  fun merge(
    localSections: List<NgaBoardSection>,
    categoryApiRaw: String,
  ): List<NgaBoardSection>? {
    val remoteSections = NgaBoardCategoryParser.parseSections(categoryApiRaw)
    if (remoteSections.isEmpty()) return null

    val existingIds = localSections.allBoardIds()
    var changed = false
    val merged =
      localSections.map { section ->
        if (section.id !in TARGET_SECTION_IDS) return@map section
        val remoteSection = remoteSections.find { it.id == section.id } ?: return@map section
        val newBoards =
          remoteSection.groups
            .flatMap(NgaBoardGroup::boards)
            .filter { board -> board.boardId !in existingIds }
            .distinctBy(NgaBoardSummary::boardId)
        if (newBoards.isEmpty()) return@map section
        changed = true
        section.copy(
          groups = section.groups + NgaBoardGroup(id = "incremental", name = "", boards = newBoards),
        )
      }
    return merged.takeIf { changed }
  }

  private fun List<NgaBoardSection>.allBoardIds(): Set<String> =
    flatMap { section -> section.groups }
      .flatMap { group -> group.boards }
      .map { it.boardId }
      .toSet()
}
