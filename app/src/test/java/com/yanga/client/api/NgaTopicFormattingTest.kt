package com.yanga.client.api

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NgaTopicFormattingTest {
  @Test fun threadHeaderAndPostSubjectDecodeExactlyOnce() {
    val thread = NgaThreadParser.parseRead("""
      {"data":{"__T":{"tid":1,"subject":"&quot;测试&quot; &amp;quot;"},
      "__R":{"0":{"pid":0,"tid":1,"content":"正文"}}}}
    """)
    assertEquals("\"测试\" &quot;", thread.subject)
    assertEquals("\"测试\" &quot;", thread.posts.single().subject)
  }

  @Test fun decodesQuotationMarksAndUnicodeWithoutLosingCategoryTags() {
    assertEquals(
      "[Steam] \"三国志 14\" & '测试' 😀 😀",
      NgaDisplayText.singleLine("[Steam] &quot;三国志 14&quot; &amp; &#39;测试&apos; &#128512; &#x1F600;"),
    )
  }

  @Test fun normalizesLineBreaksAndPreservesLiteralAndMalformedMarkup() {
    assertEquals("A B C", NgaDisplayText.singleLine(" A<br/>B<BR >\n C&nbsp; "))
    assertEquals("<b>文字</b> &quot; [PS5] [攻略]", NgaDisplayText.singleLine(
      "&lt;b&gt;文字&lt;/b&gt; &amp;quot; [PS5] [攻略]",
    ))
    val invalid = "&#xD800; &#1114112; &#999999999999; &#0; &unknown;"
    assertEquals(invalid, NgaDisplayText.singleLine(invalid))
  }

  @Test fun readsCapturedRedAnnouncementAndBoardMirrorRecords() {
    assertEquals(NgaTitleStyle(color = "red", bold = true), NgaTitleStyleParser.parse("AQAAACE"))
    // The board id record comes before the font record in this real response.
    assertEquals(NgaTitleStyle(bold = true), NgaTitleStyleParser.parse("AwAAAmgBAAAAIA"))
    assertEquals(NgaTitleStyle(), NgaTitleStyleParser.parse("AgLJcrA"))
  }

  @Test fun ignoresUnknownRecordsAndIncompleteOrMalformedData() {
    assertEquals(NgaTitleStyle(), NgaTitleStyleParser.parse("not base64!"))
    assertEquals(NgaTitleStyle(), NgaTitleStyleParser.parse("AQAA"))
    val data = byteArrayOf(9, 0, 0, 0, 1, 1, 0, 0, 0, 66)
    assertEquals(NgaTitleStyle(color = "blue", italic = true), NgaTitleStyleParser.parse(
      Base64.getEncoder().encodeToString(data),
    ))
  }

  @Test fun supportsLegacyStylesAndExpandedFontBits() {
    assertEquals(NgaTitleStyle(color = "green", bold = true, italic = true, underline = true),
      NgaTitleStyleParser.parse("~green~b~i~u"))
    assertEquals(NgaTitleStyle(bold = true, italic = true), NgaTitleStyleParser.parse("~1~~"))
    assertEquals(NgaTitleStyle(color = "silver"), NgaTitleStyleParser.parse("", titleFont = "sliver"))
    assertEquals(NgaTitleStyle(color = "orange", underline = true), NgaTitleStyleParser.parse("", 136))
    assertEquals(NgaTitleStyle(), NgaTitleStyleParser.parse("AQAAACE", 0))
  }

  @Test fun parsesRealListFormatsAndKeepsNavigationAndStatusIndependent() {
    val list = NgaTopicListParser.parse("""
      {"data":{"__F":{"fid":414},"__T":{
        "0":{"tid":47314939,"fid":414,"subject":"[Steam] 近期论坛多人被steam假客服诈骗 注意防范","topic_misc":"AQAAACE","type":0},
        "1":{"tid":47535882,"fid":510505,"subject":"&quot;三国志 14 with 威力加强传承版&quot; 今日上市！","topic_misc":"AQAAACE","type":134225920,"parent":{"0":510505,"2":"游戏业界新闻"}},
        "2":{"tid":15763999,"fid":635,"subject":"Nintendo游戏综合讨论","topic_misc":"AwAAAmgBAAAAIA","topic_misc_var":{"3":616,"1":32},"type":2097152,"parent":{"0":635,"2":"版面镜像"}},
        "3":{"tid":46756528,"fid":707,"subject":"枫树山丘(怀旧服)","type":33792,"parent":{"0":707,"2":"冒险岛"}}
      }}}
    """)
    assertEquals(NgaTitleStyle(color = "red", bold = true), list.topics[0].titleStyle)
    val quoted = list.topics[1]
    assertEquals("\"三国志 14 with 威力加强传承版\" 今日上市！", quoted.title)
    assertEquals("游戏业界新闻", quoted.boardName)
    assertTrue(quoted.hasAttachments)
    assertFalse(quoted.isLocked)
    assertEquals(NgaTopicEntryTarget("616", NgaTopicEntryType.Board), list.topics[2].entryTarget)
    assertEquals(NgaTitleStyle(bold = true), list.topics[2].titleStyle)
    assertTrue(list.topics[3].isLocked)
    assertEquals(NgaTopicEntryType.Collection, list.topics[3].entryTarget?.type)
  }
}
