package com.yanga.client.ui.content

import org.junit.Assert.*
import org.junit.Test

class PostDiceRollsTest {
  private val context = PostDiceContext("1", "10", "25")

  @Test fun publishedPostShowsSeededRollsInBody() {
    val source = "前[dice]2d6+1d20[/dice]后"
    val rendered = PostDiceRolls.render(source, context)
    assertEquals("前🎲 2d6+1d20 = d6(4) + d6(5) + d20(4) = 13后", rendered)
    assertEquals(rendered, (PostContentParser.parse(source, context).single() as PostContentPart.Text).text)
    assertEquals(source, PostDiceRolls.render(source, null))
    assertEquals(source, PostDiceRolls.render(source, context.copy(authorId = "-1")))
  }

  @Test fun outsideCollapseRollsBeforeInsideAndInsideOnlyAddsSeedOffset() {
    val source = "[collapse=隐藏][dice]d6[/dice][/collapse] 外[dice]d6[/dice]"
    val rendered = PostDiceRolls.render(source, context)
    assertTrue(rendered.contains("[collapse=隐藏]🎲 d6 = d6(5) = 5[/collapse]"))
    assertTrue(rendered.contains("外🎲 d6 = d6(4) = 4"))
    assertEquals("[collapse=隐藏]🎲 d6 = d6(5) = 5[/collapse]",
      PostDiceRolls.render("[collapse=隐藏][dice]d6[/dice][/collapse]", context))
    val collapse = PostContentParser.parse(source, context).first() as PostContentPart.Collapse
    assertTrue((collapse.parts.single() as PostContentPart.Text).text.contains("d6(5)"))
  }

  @Test fun invalidAndCodeExpressionsStayVisibleAndDoNotConsumeRolls() {
    val source = "[code][dice]d6[/dice][/code] [dice]99d6[/dice] [dice]d6[/dice]"
    val rendered = PostDiceRolls.render(source, context)
    assertTrue(rendered.contains("[code][dice]d6[/dice][/code]"))
    assertTrue(rendered.contains("[dice]99d6[/dice]"))
    assertTrue(rendered.contains("🎲 d6 = d6(4) = 4"))
  }
}
