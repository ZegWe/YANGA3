package com.yanga.client.data.boards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalBoardListParserTest {
  @Test
  fun parseCachedBoardListJson() {
    val json =
      """
      [
        {
          "id": "wow",
          "name": "魔兽世界",
          "groups": [
            {
              "id": "10000",
              "name": "魔兽世界",
              "boards": [
                {
                  "id": "7",
                  "name": "议事厅",
                  "iconUrl": "https://img4.nga.178.com/ngabbs/nga_classic/f/app/7.png"
                }
              ]
            }
          ]
        }
      ]
      """.trimIndent()

    val sections = BoardListJsonCodec.decode(json)

    assertEquals(1, sections.size)
    assertEquals("wow", sections.single().id)
    assertEquals("议事厅", sections.single().groups.single().boards.single().name)
    assertEquals("7", sections.single().groups.single().boards.single().boardId)
    assertTrue(sections.single().groups.single().boards.single().iconUrl!!.contains("/7.png"))
  }

  @Test
  fun encodeFillsMissingSectionAndGroupIdsFromNames() {
    val json =
      BoardListJsonCodec.encode(
        listOf(
          com.yanga.client.api.NgaBoardSection(
            id = "",
            name = "魔兽世界",
            groups =
              listOf(
                com.yanga.client.api.NgaBoardGroup(
                  id = "",
                  name = "子分组",
                  boards =
                    listOf(
                      com.yanga.client.api.NgaBoardSummary(
                        boardId = "7",
                        name = "议事厅",
                        iconUrl = "https://example.test/7.png",
                      ),
                    ),
                ),
              ),
          ),
        ),
      )

    assertTrue(json.contains("\"id\": \"魔兽世界\""))
    assertTrue(json.contains("\"id\": \"子分组\""))
  }

  @Test
  fun roundTripPreservesIconUrl() {
    val original = BoardListJsonCodec.decode(
      """
      [
        {
          "id": "game",
          "name": "游戏",
          "groups": [
            {
              "id": "g1",
              "name": "分组",
              "boards": [
                {
                  "id": "10_12345",
                  "name": "子版",
                  "iconUrl": "https://img4.nga.178.com/proxy/cache_attach/ficon/12345v.png"
                }
              ]
            }
          ]
        }
      ]
      """.trimIndent(),
    )

    val restored = BoardListJsonCodec.decode(BoardListJsonCodec.encode(original))

    assertEquals("10_12345", restored.single().groups.single().boards.single().boardId)
    assertTrue(restored.single().groups.single().boards.single().iconUrl!!.contains("12345"))
  }
}
