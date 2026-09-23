package com.yanga.client.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.input.VisualTransformation
import com.yanga.client.ui.content.ComposerVisualTransformation
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import coil.compose.AsyncImage
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yanga.client.data.NgaReadOnlyRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicComposer(
  boardName: String,
  fid: Int,
  model: TopicComposerViewModel,
  repository: NgaReadOnlyRepository,
  loginSession: LoginSessionUiState?,
  onLogin: () -> Unit,
  onWeb: () -> Unit,
) {
  val context = LocalContext.current
  val loggedIn = !loginSession?.cookie.isNullOrBlank()
  var webPrompt by remember { mutableStateOf(false) }
  var tools by remember { mutableStateOf(false) }
  var emojis by remember { mutableStateOf(false) }
  var editorMode by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("源码") }
  var menu by remember { mutableStateOf(false) }
  var clearPrompt by remember { mutableStateOf(false) }
  var imageEditing by remember { mutableStateOf<PendingTopicAttachment?>(null) }
  var uploadSettings by remember { mutableStateOf(false) }
  var clipboardError by remember { mutableStateOf<String?>(null) }
  val canPublish = loggedIn && !model.busy && model.title.isNotBlank() && model.content.text.length >= 3 && model.pending.isEmpty()
  fun publish() { if (canPublish) model.submit(repository, loginSession?.toData(), fid) }
  val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
    uris.forEach { uri -> runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
    model.stage(context.contentResolver, uris)
  }
  Dialog(onDismissRequest = model::close, properties = DialogProperties(
    usePlatformDefaultWidth = false, dismissOnBackPress = !model.busy, dismissOnClickOutside = false,
  )) {
    ComposerDropTarget(loggedIn && !model.busy) { model.stage(context.contentResolver, it) }
    Scaffold(
      modifier = Modifier.fillMaxSize().imePadding().onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && event.isCtrlPressed && event.key == Key.Enter) { publish(); true } else false
      },
      topBar = {
        TopAppBar(title = { Text("发帖") }, navigationIcon = {
          IconButton(onClick = model::close, enabled = !model.busy) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回并保留草稿")
          }
        }, actions = {
          Box {
            TextButton(onClick = { menu = true }, enabled = !model.busy) { Text("更多") }
            DropdownMenu(menu, onDismissRequest = { menu = false }) {
              DropdownMenuItem(text = { Text("恢复已保存草稿") }, onClick = { model.restoreDraft(); menu = false })
              DropdownMenuItem(text = { Text("清空草稿") }, onClick = { clearPrompt = true; menu = false })
              DropdownMenuItem(text = { Text("网页发帖") }, onClick = { webPrompt = true; menu = false })
            }
          }
          Button(onClick = ::publish, enabled = canPublish) { Text("发布") }
          Spacer(Modifier.width(12.dp))
        })
      },
    ) { padding ->
      Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(boardName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        if (!loggedIn) {
          Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              Text("登录后即可发布和上传附件", modifier = Modifier.weight(1f))
              TextButton(onClick = onLogin) { Text("登录") }
            }
          }
        }
        if (model.preparing) LinearProgressIndicator(Modifier.fillMaxWidth())
        model.preparation?.let { info ->
          if (info.warning.isNotBlank()) Text(info.warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          if (info.categories.isNotEmpty()) ComposerChoice(
            if (info.categoryRequired) "主题分类（必选）" else "主题分类", "请选择", info.categories.map { it to it }, !model.busy,
          ) { model.chooseCategory(it) }
        }
        model.preparationError?.let { message ->
          Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
          TextButton(onClick = { model.prepare(repository, loginSession?.toData(), fid) }, enabled = loggedIn && !model.preparing) { Text("重新读取版块设置") }
        }
        OutlinedTextField(value = model.title, onValueChange = model::editTitle, enabled = !model.busy,
          label = { Text("标题") }, placeholder = { Text("用一句话概括你的主题") },
          modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.large)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("源码", "可视化", "预览").forEach { mode ->
            FilterChip(selected = editorMode == mode, onClick = { editorMode = mode }, label = { Text(mode) })
          }
        }
        if (editorMode == "预览") {
          OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Text(model.title.ifBlank { "未填写标题" }, style = MaterialTheme.typography.titleLarge)
              SignatureContent(model.content.text.ifBlank { "正文预览" })
              if (model.options.voteType != com.yanga.client.api.TopicVoteType.None) {
                HorizontalDivider()
                Text("投票预览 · ${model.options.voteType.label}", style = MaterialTheme.typography.titleSmall)
                model.options.voteItems.lines().filter(String::isNotBlank).forEach { Text(it) }
              }
              Text("骰子、随机段落和外站卡片以论坛发布后的展示为准。", style = MaterialTheme.typography.bodySmall)
            }
          }
        } else {
          Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { model.applyTool("b") }, enabled = !model.busy) { Text("粗体") }
            TextButton(onClick = { model.applyTool("quote") }, enabled = !model.busy) { Text("引用") }
            TextButton(onClick = { emojis = true }, enabled = !model.busy) { Text("表情") }
            TextButton(onClick = { tools = true }, enabled = !model.busy) { Text("格式 / 插入") }
            TextButton(onClick = model::undoEdit, enabled = !model.busy && model.canUndo) { Text("撤销") }
            TextButton(onClick = model::redoEdit, enabled = !model.busy && model.canRedo) { Text("重做") }
          }
          OutlinedTextField(value = model.content, onValueChange = model::editContent, enabled = !model.busy,
            label = { Text("正文") }, placeholder = { Text("分享你的想法…") },
            visualTransformation = if (editorMode == "可视化") remember { ComposerVisualTransformation() } else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp), minLines = 8, shape = MaterialTheme.shapes.large)
          if (editorMode == "可视化") Text("直接查看文字样式；复杂排版可切换预览。", style = MaterialTheme.typography.bodySmall)
        }
        Text("${model.content.text.length} 字符 · ${model.content.text.toByteArray(java.nio.charset.Charset.forName("GBK")).size} 字节 · 草稿自动保存",
          style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          FilledTonalButton(onClick = { picker.launch(arrayOf("*/*")) }, enabled = loggedIn && !model.busy) {
            Icon(Icons.Outlined.AttachFile, null)
            Spacer(Modifier.width(8.dp))
            Text("插入附件")
          }
          TextButton(onClick = { uploadSettings = !uploadSettings }, enabled = !model.busy) { Text("附件设置") }
        }
        Row(Modifier.horizontalScroll(rememberScrollState())) {
          TextButton(onClick = {
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val data = clipboard.primaryClip
            val uris = (0 until (data?.itemCount ?: 0)).mapNotNull { data?.getItemAt(it)?.uri }
            if (uris.isEmpty()) clipboardError = "剪贴板中没有文件；文字可长按正文粘贴。"
            else { clipboardError = null; model.stage(context.contentResolver, uris) }
          }, enabled = loggedIn && !model.busy) { Text("粘贴附件") }
          TextButton(onClick = model::insertAlbum, enabled = !model.busy && model.attachments.any { it.image }) { Text("插入相册") }
          TextButton(onClick = { webPrompt = true }, enabled = !model.busy) { Text("网页发帖") }
        }
        clipboardError?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (uploadSettings) ComposerAttachmentSettings(model)
        model.pending.forEach { file ->
          OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = android.net.Uri.parse(file.uri), contentDescription = "待上传附件预览", modifier = Modifier.size(64.dp).graphicsLayer { rotationZ = file.rotation.toFloat() })
                Text(file.name, Modifier.weight(1f).padding(horizontal = 12.dp), style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = { model.removePending(file) }, enabled = !model.busy) { Icon(Icons.Outlined.Close, "移除待上传附件") }
              }
              if (file.image) Row(Modifier.horizontalScroll(rememberScrollState())) {
                TextButton(onClick = { imageEditing = file }, enabled = !model.busy) { Text("编辑图片") }
                TextButton(onClick = { model.updatePending(file.copy(rotation = (file.rotation + 90) % 360)) }, enabled = !model.busy) { Text("旋转 ${file.rotation}°") }
                FilterChip(file.square, onClick = { model.updatePending(file.copy(square = !file.square)) }, label = { Text("居中裁成正方形") }, enabled = !model.busy)
              }
              if (file.rotation != 0 || file.square) Text("编辑后转为静态 PNG，最长边不超过 2048 像素。", style = MaterialTheme.typography.bodySmall)
              file.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
          }
        }
        if (model.pending.isNotEmpty()) Button(onClick = { model.uploadPending(context.contentResolver, repository, loginSession?.toData(), fid) }, enabled = loggedIn && !model.busy) { Text("上传附件（${model.pending.size}）") }
        model.attachments.forEach { attachment ->
          OutlinedCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
              Column(Modifier.weight(1f)) {
                Text(attachment.name, style = MaterialTheme.typography.bodyMedium)
                Text("已上传", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
              }
              TextButton(onClick = { model.insert(attachment) }, enabled = !model.busy) { Text("插入") }
              IconButton(onClick = { model.remove(attachment) }, enabled = !model.busy) {
                Icon(Icons.Outlined.Close, "移除附件 ${attachment.name}")
              }
            }
          }
        }
        ComposerOptionsPanel(model)
        if (model.busy) {
          LinearProgressIndicator(Modifier.fillMaxWidth())
          Text(model.status, style = MaterialTheme.typography.bodyMedium)
        }
        model.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("支持批量选择图片和文件 · 单个不超过 20 MB\n上传成功后自动插入正文，格式和权限以版块限制为准。",
          style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    imageEditing?.let { original ->
      ComposerImageEditor(original, onSave = { edited -> model.replacePending(original, edited); imageEditing = null }, onClose = { imageEditing = null })
    }
    if (tools) ComposerToolSheet(model) { tools = false }
    if (emojis) ComposerEmojiSheet(model::insertText) { emojis = false }
    if (clearPrompt) AlertDialog(onDismissRequest = { clearPrompt = false }, title = { Text("清空当前草稿？") },
      text = { Text("标题、正文、投票和附件引用都会移除。") },
      confirmButton = { TextButton(onClick = { model.clearDraft(); clearPrompt = false }) { Text("清空") } },
      dismissButton = { TextButton(onClick = { clearPrompt = false }) { Text("取消") } })
    if (webPrompt) AlertDialog(onDismissRequest = { webPrompt = false }, title = { Text("改用网页发帖") },
      text = { Text("原生草稿会保留。可复制标题和正文后前往网页粘贴；附件需要在网页重新添加。") },
      confirmButton = { TextButton(onClick = {
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("发帖草稿", model.title + "\n\n" + model.content.text))
        webPrompt = false
        onWeb()
      }) { Text("复制并打开网页") } },
      dismissButton = { TextButton(onClick = { webPrompt = false }) { Text("继续编辑") } })
  }
}
