package com.yanga.client.ui

import com.yanga.client.api.NgaSubBoard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubBoardFilterLogicTest {
  private val options =
    listOf(
      SubBoardOption(id = "448", name = "同人作品", valueId = "448", subscribeId = "4654"),
      SubBoardOption(id = "517", name = "衍生讨论", valueId = "517", subscribeId = "40"),
      SubBoardOption(id = "t7348283", name = "招募 求职 师徒", valueId = "7348283", subscribeId = "542"),
    )

  private val subBoardModels =
    listOf(
      NgaSubBoard(id = "448", name = "同人作品", valueId = "448", subscribeId = "4654"),
      NgaSubBoard(id = "517", name = "衍生讨论", valueId = "517", subscribeId = "40"),
      NgaSubBoard(id = "t7348283", name = "招募 求职 师徒", valueId = "7348283", subscribeId = "542"),
    )

  @Test
  fun emptySelectionMeansAllBoards() {
    assertEquals(emptySet<String>(), SubBoardFilterLogic.validateSelection(emptySet(), options))
    assertEquals(emptySet<String>(), SubBoardFilterLogic.blockedSubscribeIds(subBoardModels, emptySet()))
  }

  @Test
  fun toggleFromAllSelectsSingleBoard() {
    assertEquals(
      setOf("448"),
      SubBoardFilterLogic.toggleSelection(emptySet(), "448", options),
    )
  }

  @Test
  fun blockedSubscribeIdsRepresentsHiddenSubBoards() {
    assertEquals(
      setOf("40", "542"),
      SubBoardFilterLogic.blockedSubscribeIds(subBoardModels, setOf("448")),
    )
  }

  @Test
  fun selectedIdsFromBlockedRestoresInclusiveSelection() {
    assertEquals(
      setOf("448"),
      SubBoardFilterLogic.selectedIdsFromBlocked(subBoardModels, setOf("40", "542")),
    )
  }

  @Test
  fun visibilityChangesMapsBlockedIdsToServerActions() {
    val changes =
      SubBoardFilterLogic.visibilityChanges(
        subBoards = subBoardModels,
        previousBlockedSubscribeIds = emptySet(),
        nextBlockedSubscribeIds = setOf("40", "542"),
      )

    assertEquals(2, changes.size)
    assertEquals(false, changes.single { it.board.id == "517" }.visible)
    assertEquals(false, changes.single { it.board.id == "t7348283" }.visible)
  }

  @Test
  fun isSubBoardEnabledTreatsEmptySelectionAsAllEnabled() {
    assertTrue(SubBoardFilterLogic.isSubBoardEnabled("448", emptySet()))
    assertTrue(SubBoardFilterLogic.isSubBoardEnabled("448", setOf("448", "517")))
    assertFalse(SubBoardFilterLogic.isSubBoardEnabled("517", setOf("448")))
  }

  @Test
  fun selectedFidGroupUsesSubBoardValueIds() {
    assertEquals("448", SubBoardFilterLogic.selectedFidGroup(setOf("448"), subBoardModels))
    assertEquals("t7348283", SubBoardFilterLogic.selectedFidGroup(setOf("t7348283"), subBoardModels))
    assertEquals("448,t7348283", SubBoardFilterLogic.selectedFidGroup(setOf("448", "t7348283"), subBoardModels))
    assertEquals(null, SubBoardFilterLogic.selectedFidGroup(emptySet(), subBoardModels))
  }

  @Test
  fun visibleQuickOptionsPrioritizesSelectedBoards() {
    val visible =
      SubBoardFilterLogic.visibleQuickOptions(
        options = options,
        selectedIds = setOf("t7348283"),
        limit = 2,
      )

    assertEquals(listOf("t7348283", "448"), visible.map { it.id })
  }
}
