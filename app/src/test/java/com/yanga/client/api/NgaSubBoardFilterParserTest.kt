package com.yanga.client.api

import org.junit.Assert.assertEquals
import org.junit.Test

class NgaSubBoardFilterParserTest {
  @Test
  fun parseBlockedIdsFromArrayPayload() {
    val blocked =
      NgaSubBoardFilterParser.parseBlockedIds(
        """
        {
          "data": {
            "add_to_block_tids": ["4654", "542"]
          }
        }
        """.trimIndent(),
      )

    assertEquals(setOf("4654", "542"), blocked)
  }

  @Test
  fun parseBlockedIdsFromCommaSeparatedString() {
    val blocked =
      NgaSubBoardFilterParser.parseBlockedIds(
        """
        {
          "data": {
            "add_to_block_tids": "4654,542"
          }
        }
        """.trimIndent(),
      )

    assertEquals(setOf("4654", "542"), blocked)
  }

  @Test
  fun parseBlockedIdsFromBlockTidMapPayload() {
    val blocked =
      NgaSubBoardFilterParser.parseBlockedIds(
        """
        {
          "data": {
            "0": {
              "block_tid": {
                "0": 4654,
                "1": 542
              }
            }
          }
        }
        """.trimIndent(),
      )

    assertEquals(setOf("4654", "542"), blocked)
  }
}
