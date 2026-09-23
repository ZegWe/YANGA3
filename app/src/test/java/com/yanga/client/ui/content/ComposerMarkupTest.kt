package com.yanga.client.ui.content

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.*
import org.junit.Test

class ComposerMarkupTest {
  @Test fun wrapsSelectionWithoutReplacingSurroundingText() {
    val result = ComposerMarkup.wrap(TextFieldValue("前选择后", TextRange(1, 3)), "[b]", "[/b]")
    assertEquals("前[b]选择[/b]后", result.text)
    assertEquals(TextRange(4, 6), result.selection)
  }

  @Test fun emptySelectionPlacesCaretInsideTag() {
    assertEquals(TextRange(3), ComposerMarkup.wrap(TextFieldValue(""), "[b]", "[/b]").selection)
  }

  @Test fun tablePasteAndSpecialTagsMatchForumSyntax() {
    assertEquals("[table]\n[tr][td]甲[/td][td]乙[/td][/tr]\n[tr][td]丙[/td][td]丁[/td][/tr]\n[/table]",
      ComposerMarkup.build("table", listOf("甲\t乙\n丙\t丁"), ""))
    assertEquals("[customachieve][title]标题[/title]\n[txt]说明[/txt][/customachieve]", ComposerMarkup.build("customachieve", listOf("标题", "说明", ""), ""))
    assertEquals("[usarmory realm player]", ComposerMarkup.build("armory", listOf("us", "realm", "player"), ""))
    assertEquals("[dict][词条]解释[/dict]", ComposerMarkup.build("dict", listOf("词条", "解释"), ""))
    assertEquals("[collapse=提要]正文[/collapse]", ComposerMarkup.build("collapse", listOf("提要"), "正文"))
    assertTrue(runCatching { ComposerMarkup.build("url", listOf("javascript:alert(1)", ""), "") }.isFailure)
    assertTrue(runCatching { ComposerMarkup.build("dice", listOf("11d6"), "") }.isFailure)
  }

  @Test fun visualEditingMaintainsValidMonotonicOffsetsForNestedAndEmptyTags() {
    for (raw in listOf("前[b]粗体[i]斜体[/i][/b]后", "[b][/b]", "[b]正在输入", "[u]下划线[/u][del]删除[/del]")) {
      val result = ComposerVisualTransformation().filter(AnnotatedString(raw))
      val forward = (0..raw.length).map(result.offsetMapping::originalToTransformed)
      val backward = (0..result.text.length).map(result.offsetMapping::transformedToOriginal)
      assertEquals(forward.sorted(), forward)
      assertEquals(backward.sorted(), backward)
      assertTrue(forward.all { it in 0..result.text.length })
      assertTrue(backward.all { it in 0..raw.length })
    }
    val result = ComposerVisualTransformation().filter(AnnotatedString("[b]粗体[/b]"))
    assertEquals("粗体", result.text.text)
    assertFalse(result.text.spanStyles.isEmpty())
  }
}
