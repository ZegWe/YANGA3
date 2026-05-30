package com.yanga.client.data.boards

import com.yanga.client.api.NgaBoardGroup
import com.yanga.client.api.NgaBoardSection
import com.yanga.client.api.NgaBoardSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BoardListIncrementalMergerTest {
  @Test
  fun mergeAddsOnlyNewBoardsForTargetSections() {
    val local =
      listOf(
        NgaBoardSection(
          id = "wow",
          name = "魔兽世界",
          groups =
            listOf(
              NgaBoardGroup(
                id = "10000",
                name = "魔兽世界",
                boards = listOf(NgaBoardSummary(boardId = "7", name = "议事厅")),
              ),
            ),
        ),
      )
    val remoteJson =
      """
      {
        "data": {
          "result": [
            {
              "id": "wow",
              "name": "魔兽世界",
              "groups": [
                {
                  "id": "10000",
                  "name": "魔兽世界",
                  "forums": [
                    { "id": 7, "name": "议事厅" },
                    { "id": 999, "name": "新板块" }
                  ]
                }
              ]
            }
          ]
        }
      }
      """.trimIndent()

    val merged = BoardListIncrementalMerger.merge(local, remoteJson)

    assertNotNull(merged)
    val boards = merged!!.single().groups.flatMap { it.boards }
    assertEquals(setOf("7", "999"), boards.map { it.boardId }.toSet())
  }

  @Test
  fun mergeReturnsNullWhenNothingNew() {
    val local =
      listOf(
        NgaBoardSection(
          id = "wow",
          name = "魔兽世界",
          groups =
            listOf(
              NgaBoardGroup(
                id = "10000",
                name = "魔兽世界",
                boards = listOf(NgaBoardSummary(boardId = "7", name = "议事厅")),
              ),
            ),
        ),
      )
    val remoteJson =
      """
      {
        "data": {
          "result": [
            {
              "id": "wow",
              "name": "魔兽世界",
              "groups": [
                {
                  "id": "10000",
                  "name": "魔兽世界",
                  "forums": [
                    { "id": 7, "name": "议事厅" }
                  ]
                }
              ]
            }
          ]
        }
      }
      """.trimIndent()

    assertNull(BoardListIncrementalMerger.merge(local, remoteJson))
  }
}
