package com.yanga.client.ui.content

import org.junit.Assert.*
import org.junit.Test

class StructuredPostContentParserTest {
  @Test fun headingsKeepInlineFormattingAndDoNotLeakTags() {
    val parts = PostContentParser.parse("[h][b]活动备注[/b][/h][list][*]参与活动[/list]")
    val heading = parts[0] as PostContentPart.Heading
    assertEquals("活动备注", (heading.parts.single() as PostContentPart.Text).text)
    assertTrue((heading.parts.single() as PostContentPart.Text).styles.single().bold)
    assertTrue(parts[1] is PostContentPart.ListBlock)
  }

  @Test fun tablesPreserveSpansAndIgnoreInterRowWhitespace() {
    val table = PostContentParser.parse("[table]\n[tr][td colspan2]奖励[/td][/tr]\n\n[tr][td width50 rowspan=2]冠军[/td][td]75声望[/td][/tr]\n[tr][td]45声望[/td][/tr]\n[/table]").single() as PostContentPart.Table
    assertEquals(listOf(1, 2, 1), table.rows.map { it.size })
    assertEquals(PostContentPart.CellSpan(columns = 2), table.spans[0][0])
    assertEquals(PostContentPart.CellSpan(rows = 2), table.spans[1][0])
    assertEquals("45声望", (table.rows[2][0].single() as PostContentPart.Text).text)
  }

  @Test fun closingTagsInsideCodeDoNotCloseTheContainingBlock() {
    val part = PostContentParser.parse("[list][*][code][/list][*][/code][*]tail[/list]").single() as PostContentPart.ListBlock
    assertEquals(2, part.items.size)
    assertEquals("[/list][*]", (part.items[0].single() as PostContentPart.Code).text)
  }
  @Test fun listsKeepNestedFormattingImagesAndItemBoundaries() {
    val part = PostContentParser.parse("[list=1][*][b]first[/b][*]second[list][*]nested[img]./mon_a.png[/img][/list][*]third[/list]")
      .single() as PostContentPart.ListBlock
    assertEquals("1", part.marker)
    assertEquals(3, part.items.size)
    val first = part.items[0].single() as PostContentPart.Text
    assertEquals("first", first.text)
    assertTrue(first.styles.single().bold)
    assertTrue(part.items[1][1] is PostContentPart.ListBlock)
    assertEquals(1, PostContentParser.collectImageUrls(listOf(part)).size)
  }

  @Test fun explicitListClosersDoNotCreateExtraItems() {
    val list = PostContentParser.parse("[list][*]a[/*][*]b[/*][/list]").single() as PostContentPart.ListBlock
    assertEquals(2, list.items.size)
  }

  @Test fun collapseKeepsNestedListAndQuoteRatherThanPrintingTags() {
    val part = PostContentParser.parse("[collapse=剧透][quote]引文[/quote][list][*]内容[/list][/collapse]").single() as PostContentPart.Collapse
    assertEquals("剧透", part.title)
    assertTrue(part.parts[0] is PostContentPart.Quote)
    assertTrue(part.parts[1] is PostContentPart.ListBlock)
  }

  @Test fun codePreservesWhitespaceAndMarkupLiterally() {
    val code = PostContentParser.parse("[code=kotlin]\n  val x = \"[b]text[/b]\"\n    &lt;tag&gt;\n[/code]").single() as PostContentPart.Code
    assertEquals("kotlin", code.language)
    assertEquals("  val x = \"[b]text[/b]\"\n    <tag>", code.text)
  }

  @Test fun tablePreservesRowsEmptyCellsAndNestedBlocks() {
    val table = PostContentParser.parse("[table][tr][td]A[/td][td][/td][/tr][tr][td][list][*]B[/list][/td][td]C[/td][/tr][/table]")
      .single() as PostContentPart.Table
    assertEquals(2, table.rows.size)
    assertEquals(2, table.rows[0].size)
    assertTrue(table.rows[0][1].isEmpty())
    assertTrue(table.rows[1][0].single() is PostContentPart.ListBlock)
  }

  @Test fun incompleteBlocksAndUnknownTagsDoNotDiscardText() {
    val list = PostContentParser.parse("[list][*]unfinished").single() as PostContentPart.ListBlock
    assertEquals("unfinished", (list.items.single().single() as PostContentPart.Text).text)
    assertEquals("[Custom=ABC]Text[/Custom][]", (PostContentParser.parse("[Custom=ABC]Text[/Custom][]").single() as PostContentPart.Text).text)
  }

  @Test fun handlesStandaloneBulletsRuleAndNonBmpEntities() {
    val parts = PostContentParser.parse("[*]第一项<br/>[*]&#x1f600;[hr]结束")
    assertEquals("• 第一项\n\n• 😀", (parts[0] as PostContentPart.Text).text)
    assertEquals(PostContentPart.Rule, parts[1])
  }

  @Test fun capsNestingDepthAndCollectsImagesInsideTablesAndCollapses() {
    val source = "[collapse]".repeat(80) + "内容" + "[/collapse]".repeat(80)
    assertTrue(PostContentParser.parse(source).isNotEmpty())
    assertEquals(1, PostContentParser.collectImageUrls(PostContentParser.parse("[collapse][table][tr][td][img]./mon_a.jpg[/img][/td][/tr][/table][/collapse]")).size)
  }
}
