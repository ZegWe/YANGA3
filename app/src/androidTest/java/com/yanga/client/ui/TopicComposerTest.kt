package com.yanga.client.ui

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import com.yanga.client.api.TopicAttachment
import com.yanga.client.data.DefaultNgaReadOnlyRepository
import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.NgaReadOnlyRepository
import com.yanga.client.theme.YangaTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TopicComposerTest {
  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
  private val login = LoginSessionUiState(username = "Test", uid = "1", cookie = "test=1")

  @Test fun uploadInsertsAtCursorAndRemovalUpdatesBody() {
    val model = TopicComposerViewModel(SavedStateHandle())
    val attachment = TopicAttachment("attachment", "id", "check", "https://img.nga.cn/attachments/mon_202609/321/test.png", false)
    val requests = mutableListOf<com.yanga.client.api.NgaRequest>()
    val repository = DefaultNgaReadOnlyRepository(object : com.yanga.client.api.NgaHttpTransport {
      override fun execute(request: com.yanga.client.api.NgaRequest): Result<com.yanga.client.api.NgaHttpResponse> {
        requests += request
        val response = if (requests.size == 1) {
          assertEquals("321", request.query["fid"])
          """{"data":{"auth":"test-ticket"}}"""
        } else {
          assertEquals("https://img8.nga.cn/attach.php", request.url)
          assertTrue(request.binaryBody!!.toList().windowed(3).any { it == listOf<Byte>(1, 2, 3) })
          """window.script_muti_get_var_store={"data":{"attachments":"id","attachments_check":"check","url":"mon_202609/321/test.png"}};"""
        }
        return Result.success(com.yanga.client.api.NgaHttpResponse(200, response))
      }
    })
    compose.setContent { YangaTheme { TopicComposer("DOTA2", 321, model, repository, login, {}, {}) } }
    compose.runOnIdle {
      model.editContent(TextFieldValue("beforeafter", TextRange(6)))
      val file = java.io.File(compose.activity.cacheDir, "topic-upload-test.bin").apply { writeBytes(byteArrayOf(1, 2, 3)) }
      model.upload(compose.activity.contentResolver, Uri.fromFile(file), repository, login.toData(), 321)
    }
    compose.waitUntil(10_000) { !model.busy }
    compose.runOnIdle {
      assertNull(model.error)
      assertEquals("before\n${attachment.markup}\nafter", model.content.text)
      assertEquals(listOf(attachment), model.attachments)
      assertEquals(2, requests.size)
      model.remove(attachment)
      assertTrue(model.attachments.isEmpty())
      assertFalse(model.content.text.contains(attachment.markup))
    }
  }

  @Test fun failedPublishingPreservesDraftAndCanRestoreIt() {
    val saved = SavedStateHandle()
    val model = TopicComposerViewModel(saved)
    val repository = object : NgaReadOnlyRepository by DefaultNgaReadOnlyRepository() {
      override suspend fun submitTopic(session: LoginSessionData?, fid: Int, subject: String, content: String, attachments: List<TopicAttachment>): Result<Unit> =
        Result.failure(IllegalStateException("帐号声望不足"))
    }
    compose.setContent { YangaTheme { TopicComposer("DOTA2", 321, model, repository, login, {}, {}) } }
    compose.onNodeWithText("发布").assertIsNotEnabled()
    compose.onNodeWithText("标题").performTextInput("本地测试标题")
    compose.onNodeWithText("正文").performTextInput("本地测试正文")
    compose.onNodeWithText("发布").assertIsEnabled().performClick()
    compose.waitUntil(10_000) { model.error != null }
    compose.runOnIdle {
      assertTrue(model.error!!.contains("帐号声望不足"))
      model.close(); model.open()
      val restored = TopicComposerViewModel(saved)
      assertEquals("本地测试标题", restored.title)
      assertEquals("本地测试正文", restored.content.text)
      assertFalse(model.sent)
    }
  }

  @Test fun formattingUndoAndPreviewAreAvailableInNativeComposer() {
    val model = TopicComposerViewModel(SavedStateHandle())
    compose.setContent { YangaTheme { TopicComposer("DOTA2", 321, model, DefaultNgaReadOnlyRepository(), login, {}, {}) } }
    compose.runOnIdle {
      model.editContent(TextFieldValue("测试内容", TextRange(0, 4)))
      model.applyTool("b")
      assertEquals("[b]测试内容[/b]", model.content.text)
      model.undoEdit(); assertEquals("测试内容", model.content.text)
      model.redoEdit(); assertEquals("[b]测试内容[/b]", model.content.text)
    }
    compose.onNodeWithText("可视化").performScrollTo().performClick()
    compose.onNodeWithText("预览").performScrollTo().performClick()
    compose.onNodeWithText("未填写标题").assertExists()
    compose.onNodeWithText("测试内容", substring = true).assertExists()
  }

  @Test fun persistentDraftRestoresOptionsAndKeepsAccountsSeparate() {
    val fid = -987654
    val account = "composer_instrumentation"
    val store = com.yanga.client.data.TopicDraftStore(compose.activity, account, fid)
    store.clear()
    val model = TopicComposerViewModel(SavedStateHandle())
    compose.runOnUiThread {
      model.bindDraft(compose.activity, account, fid)
      model.editTitle("持久草稿")
      model.editContent(TextFieldValue("[b]正文[/b]"))
      model.updateOptions(com.yanga.client.api.TopicPostOptions(anonymous = true, voteType = com.yanga.client.api.TopicVoteType.Poll, voteItems = "甲\n乙"))
      val recreated = TopicComposerViewModel(SavedStateHandle())
      recreated.bindDraft(compose.activity, account, fid)
      assertEquals("持久草稿", recreated.title)
      assertEquals("[b]正文[/b]", recreated.content.text)
      assertTrue(recreated.options.anonymous)
      assertEquals("甲\n乙", recreated.options.voteItems)
      recreated.bindDraft(compose.activity, "another_test_account", fid)
      assertEquals("", recreated.title)
      recreated.bindDraft(compose.activity, account, fid)
      assertEquals("持久草稿", recreated.title)
      recreated.clearDraft()
    }
    com.yanga.client.data.TopicDraftStore(compose.activity, "another_test_account", fid).clear()
  }

  @Test fun imageEditingRotatesAndCropsWithoutMutatingOriginal() {
    val original = android.graphics.Bitmap.createBitmap(20, 10, android.graphics.Bitmap.Config.ARGB_8888)
    val bytes = java.io.ByteArrayOutputStream().use { original.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it); it.toByteArray() }
    val rotated = com.yanga.client.ui.content.ComposerImageEdit.transform(bytes, 90, false)
    android.graphics.BitmapFactory.decodeByteArray(rotated, 0, rotated.size).let {
      assertEquals(10, it.width); assertEquals(20, it.height); it.recycle()
    }
    val cropped = com.yanga.client.ui.content.ComposerImageEdit.transform(bytes, 0, true)
    android.graphics.BitmapFactory.decodeByteArray(cropped, 0, cropped.size).let {
      assertEquals(10, it.width); assertEquals(10, it.height); it.recycle()
    }
    assertEquals(20, original.width)
    original.recycle()
  }

  @Test fun imageAnnotationAndUndoProduceExpectedPixels() {
    compose.runOnUiThread {
      val source = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.WHITE) }
      val view = ComposerDrawingView(compose.activity) {}
      view.source = source; view.mode = "箭头"; view.penColor = android.graphics.Color.RED; view.penSize = 30f
      view.layout(0, 0, 200, 200)
      fun touch(action: Int, x: Float, y: Float) {
        android.view.MotionEvent.obtain(0, 0, action, x, y, 0).let { view.onTouchEvent(it); it.recycle() }
      }
      touch(android.view.MotionEvent.ACTION_DOWN, 40f, 40f)
      touch(android.view.MotionEvent.ACTION_MOVE, 160f, 160f)
      touch(android.view.MotionEvent.ACTION_UP, 160f, 160f)
      val annotated = view.render()
      assertEquals(android.graphics.Color.RED, annotated.getPixel(50, 50))
      assertEquals(android.graphics.Color.WHITE, source.getPixel(50, 50))
      view.undo()
      val restored = view.render()
      assertEquals(android.graphics.Color.WHITE, restored.getPixel(50, 50))
      annotated.recycle(); restored.recycle(); source.recycle()
    }
  }
}
