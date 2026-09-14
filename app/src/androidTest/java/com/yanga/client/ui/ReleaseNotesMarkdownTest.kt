package com.yanga.client.ui

import android.text.style.ClickableSpan
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class ReleaseNotesMarkdownTest {
  @Test fun rendersMarkdownAndOpensReleaseLinks() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync {
      var opened = ""
      val renderer = releaseNotesRenderer(instrumentation.targetContext) { opened = it }
      val text = renderer.toMarkdown("## 更新内容\n\n- **修复链接**\n- ~~旧行为~~\n\n[完整记录](https://github.com/ZegWe/YANGA3/compare/v0.3.1...v0.3.3)\n\n`代码`\n\n> 引用")
      assertTrue(text.toString().contains("更新内容"))
      assertFalse(text.toString().contains("##"))
      assertFalse(text.toString().contains("**"))
      assertFalse(text.toString().contains("~~"))
      val links = text.getSpans(0, text.length, ClickableSpan::class.java)
      assertEquals(1, links.size)
      links.single().onClick(TextView(instrumentation.targetContext))
      assertEquals("https://github.com/ZegWe/YANGA3/compare/v0.3.1...v0.3.3", opened)
    }
  }
}
