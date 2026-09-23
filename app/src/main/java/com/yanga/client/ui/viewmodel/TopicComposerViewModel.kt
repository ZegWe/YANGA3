package com.yanga.client.ui

import android.content.Context
import com.yanga.client.api.TopicPostOptions
import com.yanga.client.api.TopicUploadOptions
import com.yanga.client.api.TopicPostPreparation
import com.yanga.client.ui.content.ComposerMarkup
import com.yanga.client.data.TopicDraft
import com.yanga.client.data.TopicDraftStore
import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yanga.client.api.NgaTopicPosting
import com.yanga.client.api.TopicAttachment
import com.yanga.client.data.LoginSessionData
import com.yanga.client.data.NgaReadOnlyRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PendingTopicAttachment(val uri: String, val name: String, val rotation: Int = 0, val square: Boolean = false, val error: String? = null, val image: Boolean = false) : java.io.Serializable

class TopicComposerViewModel(private val saved: SavedStateHandle) : ViewModel() {
  var visible by mutableStateOf(saved.get<Boolean>("visible") ?: false)
    private set
  var title by mutableStateOf(saved.get<String>("title").orEmpty())
    private set
  var content by mutableStateOf(TextFieldValue(saved.get<String>("content").orEmpty()))
    private set
  var attachments: List<TopicAttachment> by mutableStateOf(saved.get<ArrayList<TopicAttachment>>("attachments") ?: emptyList())
    private set
  var busy by mutableStateOf(false)
    private set
  var status by mutableStateOf("")
    private set
  var error by mutableStateOf<String?>(null)
    private set
  var sent by mutableStateOf(false)

  var options by mutableStateOf(saved.get<TopicPostOptions>("options") ?: TopicPostOptions())
    private set
  var uploadOptions by mutableStateOf(saved.get<TopicUploadOptions>("uploadOptions") ?: TopicUploadOptions())
    private set
  var preparation by mutableStateOf<TopicPostPreparation?>(null)
    private set
  var preparationError by mutableStateOf<String?>(null)
    private set
  var preparing by mutableStateOf(false)
    private set
  var pending: List<PendingTopicAttachment> by mutableStateOf(saved.get<ArrayList<PendingTopicAttachment>>("pending") ?: emptyList())
    private set
  private var draftStore: TopicDraftStore? = null
  private var draftKey: String? = null
  private val undo = ArrayDeque<TextFieldValue>()
  private val redo = ArrayDeque<TextFieldValue>()
  var historyVersion by mutableStateOf(0)
    private set
  val canUndo get() = historyVersion >= 0 && undo.isNotEmpty()
  val canRedo get() = historyVersion >= 0 && redo.isNotEmpty()

  fun bindDraft(context: Context, account: String, fid: Int) {
    val key = "$account:$fid"
    if (draftKey == key) return
    val previous = draftKey
    if (previous != null) persistDraft()
    if (previous != null && !previous.startsWith("guest:")) {
      title = ""; content = TextFieldValue(); attachments = emptyList(); pending = emptyList()
      options = TopicPostOptions(); uploadOptions = TopicUploadOptions()
      saved["title"] = ""; saved["content"] = ""; saved["owner"] = null
      saved["attachments"] = arrayListOf<TopicAttachment>(); saved["pending"] = arrayListOf<PendingTopicAttachment>()
      saved["options"] = options; saved["uploadOptions"] = uploadOptions
      undo.clear(); redo.clear(); historyVersion++
    }
    draftKey = key
    draftStore = TopicDraftStore(context.applicationContext, account, fid)
    if (title.isBlank() && content.text.isBlank() && attachments.isEmpty()) restoreDraft()
  }
  private fun persistDraft() {
    draftStore?.save(TopicDraft(title, content.text, attachments, saved.get<String>("owner"), options, uploadOptions))
  }
  fun restoreDraft() {
    val draft = draftStore?.load() ?: return
    title = draft.title; content = TextFieldValue(draft.content); attachments = draft.attachments
    options = draft.options; uploadOptions = draft.uploadOptions; saved["owner"] = draft.owner
    saved["title"] = title; saved["content"] = content.text; saved["attachments"] = ArrayList(attachments)
    saved["options"] = options; saved["uploadOptions"] = uploadOptions
    undo.clear(); redo.clear(); historyVersion++
  }
  fun clearDraft() {
    if (busy) return
    title = ""; content = TextFieldValue(); attachments = emptyList(); pending = emptyList()
    options = TopicPostOptions(); error = null
    saved["title"] = ""; saved["content"] = ""; saved["attachments"] = arrayListOf<TopicAttachment>()
    saved["pending"] = arrayListOf<PendingTopicAttachment>(); saved["options"] = options
    undo.clear(); redo.clear(); historyVersion++
    draftStore?.clear()
  }
  fun updateOptions(value: TopicPostOptions) { options = value; saved["options"] = value; persistDraft() }
  fun updateUploadOptions(value: TopicUploadOptions) { uploadOptions = value; saved["uploadOptions"] = value; persistDraft() }
  fun prepare(repository: NgaReadOnlyRepository, session: LoginSessionData?, fid: Int) {
    if (preparing) return
    preparing = true; preparationError = null; preparation = null
    viewModelScope.launch {
      try {
        repository.prepareTopic(session, fid).fold(
          onSuccess = { preparation = it; if (title.isBlank() && it.defaultSubject.isNotBlank()) editTitle(it.defaultSubject) },
          onFailure = { preparationError = it.message ?: "发帖设置加载失败" },
        )
      } finally { preparing = false }
    }
  }
  fun chooseCategory(value: String) {
    val category = value.trim().let { if (it.startsWith("[")) it else "[$it]" }
    editTitle(category + title.replaceFirst(Regex("^\\[[^\\]]+\\]\\s*"), ""))
  }
  fun applyTool(id: String, values: List<String> = emptyList()) {
    val selected = content.text.substring(content.selection.min, content.selection.max)
    val text = ComposerMarkup.build(id, values, selected)
    val next = when {
      id in listOf("color", "size", "font", "align", "collapse") -> ComposerMarkup.wrap(content, text.substringBefore(']') + "]", "[/$id]")
      values.isEmpty() && id != "rule" -> ComposerMarkup.wrap(content, "[$id]", "[/$id]")
      else -> ComposerMarkup.insert(content, text)
    }
    editContent(next)
  }
  fun insertText(value: String) { editContent(ComposerMarkup.insert(content, value)) }
  fun undoEdit() {
    if (undo.isEmpty() || busy) return
    redo.addLast(content); content = undo.removeLast(); saved["content"] = content.text; historyVersion++; persistDraft()
  }
  fun redoEdit() {
    if (redo.isEmpty() || busy) return
    undo.addLast(content); content = redo.removeLast(); saved["content"] = content.text; historyVersion++; persistDraft()
  }
  fun stage(resolver: ContentResolver, uris: List<Uri>) {
    if (busy) return
    error = null
    viewModelScope.launch {
      val selected = withContext(Dispatchers.IO) { uris.distinct().map { uri ->
        val name = runCatching { resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null } }.getOrNull() ?: uri.lastPathSegment ?: "附件"
        PendingTopicAttachment(uri.toString(), name, image = resolver.getType(uri)?.startsWith("image/") == true || name.substringAfterLast('.').lowercase() in listOf("png", "jpg", "jpeg", "gif", "webp"))
      } }
      pending = (pending + selected).distinctBy { it.uri }
      saved["pending"] = ArrayList(pending)
    }
  }
  fun updatePending(item: PendingTopicAttachment) { pending = pending.map { if (it.uri == item.uri) item else it }; saved["pending"] = ArrayList(pending) }
  fun replacePending(original: PendingTopicAttachment, replacement: PendingTopicAttachment) {
    pending = pending.map { if (it.uri == original.uri) replacement else it }; saved["pending"] = ArrayList(pending)
  }
  fun removePending(item: PendingTopicAttachment) { pending = pending - item; saved["pending"] = ArrayList(pending) }
  fun insertAlbum() {
    val images = attachments.filter { it.image }
    if (images.isNotEmpty()) insertText("[album=图片集]\n" + images.joinToString("\n") { it.url } + "\n[/album]")
  }

  fun open() { visible = true; saved["visible"] = true }
  fun close() { if (!busy) { visible = false; saved["visible"] = false } }
  fun editTitle(value: String) { title = value; saved["title"] = value; persistDraft() }
  fun editContent(value: TextFieldValue) {
    if (value.text != content.text) {
      undo.addLast(content); if (undo.size > 60) undo.removeFirst()
      redo.clear(); historyVersion++
    }
    val changed = content.text != value.text
    content = value; saved["content"] = value.text
    if (changed) persistDraft()
  }
  fun insert(attachment: TopicAttachment) {
    val start = content.selection.min
    val text = "\n${attachment.markup}\n"
    editContent(TextFieldValue(content.text.replaceRange(start, content.selection.max, text), TextRange(start + text.length)))
  }
  private fun saveAttachments() { saved["attachments"] = ArrayList(attachments); persistDraft() }
  fun remove(attachment: TopicAttachment) {
    attachments = attachments - attachment
    saveAttachments()
    editContent(TextFieldValue(content.text.replace(attachment.markup, "")))
  }
  private fun checkOwner(session: LoginSessionData?) {
    require(!session?.cookie.isNullOrBlank()) { "请先登录" }
    require(attachments.isEmpty() || saved.get<String>("owner") == session?.uid) { "附件属于其他账号，请移除后重新上传" }
  }

  fun upload(resolver: ContentResolver, uri: Uri, repository: NgaReadOnlyRepository, session: LoginSessionData?, fid: Int) {
    uploadFiles(resolver, listOf(PendingTopicAttachment(uri.toString(), "attachment")), repository, session, fid)
  }
  fun uploadPending(resolver: ContentResolver, repository: NgaReadOnlyRepository, session: LoginSessionData?, fid: Int) {
    uploadFiles(resolver, pending.toList(), repository, session, fid)
  }
  private fun uploadFiles(resolver: ContentResolver, files: List<PendingTopicAttachment>, repository: NgaReadOnlyRepository, session: LoginSessionData?, fid: Int) {
    if (busy || files.isEmpty()) return
    busy = true; error = null
    val settings = uploadOptions
    viewModelScope.launch {
      try {
        checkOwner(session)
        for ((index, item) in files.withIndex()) {
          status = "正在上传 ${index + 1}/${files.size}：${item.name}"
          try {
            val attachment = withContext(Dispatchers.IO) {
              val uri = Uri.parse(item.uri)
              var name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
              } ?: item.name
              var mime = resolver.getType(uri) ?: android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substringAfterLast('.').lowercase()) ?: "application/octet-stream"
              var bytes = resolver.openInputStream(uri)?.use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                  val count = stream.read(buffer)
                  if (count < 0) break
                  require(output.size() + count <= NgaTopicPosting.MAX_FILE_BYTES) { "单个附件不能超过 20 MB" }
                  output.write(buffer, 0, count)
                }
                output.toByteArray()
              } ?: error("无法读取附件，请重新选择")
              if (item.rotation != 0 || item.square) {
                bytes = com.yanga.client.ui.content.ComposerImageEdit.transform(bytes, item.rotation, item.square)
                name = name.substringBeforeLast('.') + ".png"; mime = "image/png"
              }
              if (settings == TopicUploadOptions()) repository.uploadTopicAttachment(session, fid, name, mime, bytes).getOrThrow()
              else repository.uploadTopicAttachmentWithOptions(session, fid, name, mime, bytes, settings).getOrThrow()
            }
            saved["owner"] = session?.uid
            attachments = attachments + attachment; saveAttachments(); insert(attachment); removePending(item)
          } catch (e: CancellationException) { throw e
          } catch (e: Exception) {
            val message = e.message ?: "附件上传失败"
            updatePending(item.copy(error = message))
            error = "${item.name}：$message。可在附件列表重试。"
          }
        }
      } catch (e: CancellationException) { throw e
      } catch (e: Exception) { error = e.message ?: "附件上传失败"
      } finally { busy = false }
    }
  }

  fun submit(repository: NgaReadOnlyRepository, session: LoginSessionData?, fid: Int) {
    if (busy || title.isBlank() || content.text.isBlank()) return
    busy = true; status = "正在发布…"; error = null
    viewModelScope.launch {
      try {
        checkOwner(session)
        require(pending.isEmpty()) { "还有待上传的附件，请先上传或移除" }
        require(content.text.length >= 3) { "正文至少需要三个字符" }
        options.validationError(preparation?.moderator == true)?.let { error(it) }
        if (preparation?.categoryRequired == true) require(preparation!!.categories.any { title.trimStart().startsWith("[${it.trim().trim('[', ']')}]") }) { "请先选择主题分类" }
        if (options == TopicPostOptions()) repository.submitTopic(session, fid, title.trim(), content.text, attachments).getOrThrow()
        else repository.submitTopicWithOptions(session, fid, title.trim(), content.text, attachments, options).getOrThrow()
        busy = false
        clearDraft()
        visible = false; saved["visible"] = false; sent = true
      } catch (e: CancellationException) { throw e
      } catch (e: Exception) { error = (e.message ?: "发布失败") + "\n草稿已保留。如遇超时，请先查看版块确认是否发布成功，再决定是否重试。"
      } finally { busy = false }
    }
  }
}
