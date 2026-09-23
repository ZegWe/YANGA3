package com.yanga.client.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yanga.client.api.TopicVoteType
import com.yanga.client.ui.content.ComposerMarkup
import com.yanga.client.ui.content.ComposerTool
import com.yanga.client.ui.content.NgaEmoticons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComposerToolSheet(model: TopicComposerViewModel, onClose: () -> Unit) {
  var selected by remember { mutableStateOf<ComposerTool?>(null) }
  var error by remember { mutableStateOf<String?>(null) }
  ModalBottomSheet(onDismissRequest = onClose) {
    Text("格式与插入", Modifier.padding(horizontal = 24.dp, vertical = 8.dp), style = MaterialTheme.typography.titleLarge)
    Text("选中文字后应用格式；未选中时在光标处插入。", Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.bodySmall)
    LazyVerticalGrid(columns = GridCells.Adaptive(100.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp).padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      items(ComposerMarkup.tools) { tool ->
        OutlinedButton(onClick = {
          if (tool.fields.isEmpty()) { model.applyTool(tool.id); onClose() }
          else selected = tool
        }, contentPadding = PaddingValues(8.dp)) { Text(tool.label) }
      }
    }
    Spacer(Modifier.height(20.dp))
  }
  selected?.let { tool ->
    var fields by remember(tool.id) { mutableStateOf(tool.fields.mapIndexed { index, _ ->
      val selectedText = model.content.text.substring(model.content.selection.min, model.content.selection.max)
      when {
        tool.id in listOf("list", "ordered", "table") && index == 0 -> selectedText
        tool.id in listOf("code", "url", "tid", "pid") && index == 1 -> selectedText
        else -> ""
      }
    }) }
    AlertDialog(onDismissRequest = { selected = null; error = null }, title = { Text(tool.label) },
      text = {
        Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          if (tool.hint.isNotBlank()) Text(tool.hint)
          tool.fields.forEachIndexed { index, label ->
            OutlinedTextField(fields[index], { value -> fields = fields.toMutableList().also { it[index] = value } },
              label = { Text(label) }, modifier = Modifier.fillMaxWidth(), minLines = if (label.contains("每行") || label == "代码") 3 else 1)
          }
          if (tool.id == "color") Row(Modifier.horizontalScroll(rememberScrollState())) {
            listOf("red", "blue", "green", "orange", "purple", "gray").forEach { color -> TextButton(onClick = { fields = listOf(color) }) { Text(color) } }
          }
          if (tool.id == "font") Row(Modifier.horizontalScroll(rememberScrollState())) {
            listOf("simsun" to "宋体", "simhei" to "黑体", "Arial" to "Arial", "Courier New" to "等宽").forEach { (font, label) -> TextButton(onClick = { fields = listOf(font) }) { Text(label) } }
          }
          error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
      }, confirmButton = { TextButton(onClick = {
        runCatching { model.applyTool(tool.id, fields) }.fold(
          onSuccess = { selected = null; error = null; onClose() },
          onFailure = { error = it.message ?: "请检查输入" },
        )
      }) { Text("插入") } }, dismissButton = { TextButton(onClick = { selected = null; error = null }) { Text("取消") } })
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComposerEmojiSheet(onInsert: (String) -> Unit, onClose: () -> Unit) {
  val groups = remember { NgaEmoticons.catalog() }
  var group by remember { mutableStateOf("ac") }
  ModalBottomSheet(onDismissRequest = onClose) {
    Text("表情", Modifier.padding(20.dp), style = MaterialTheme.typography.titleLarge)
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      groups.keys.forEach { key -> FilterChip(selected = key == group, onClick = { group = key }, label = { Text(key.uppercase()) }) }
    }
    LazyVerticalGrid(GridCells.Adaptive(68.dp), Modifier.fillMaxWidth().heightIn(max = 380.dp).padding(16.dp)) {
      items(groups.getValue(group).entries.toList(), key = { it.key }) { (name, file) ->
        Column(Modifier.clickable { onInsert("[s:$group:$name]"); onClose() }.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
          AsyncImage(model = com.yanga.client.api.NgaStaticUrls.emoticonBaseUrl + file, contentDescription = name, modifier = Modifier.size(40.dp))
          Text(name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
      }
    }
  }
}

@Composable
internal fun ComposerChoice(label: String, value: String, choices: List<Pair<String, String>>, enabled: Boolean = true, onChoose: (String) -> Unit) {
  var open by remember { mutableStateOf(false) }
  Box {
    OutlinedButton(onClick = { open = true }, enabled = enabled) { Text("$label：${choices.firstOrNull { it.first == value }?.second ?: value}") }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
      choices.forEach { (key, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { onChoose(key); open = false }) }
    }
  }
}

@Composable
internal fun ComposerOptionsPanel(model: TopicComposerViewModel) {
  val options = model.options
  val enabled = !model.busy
  var expanded by remember { mutableStateOf(false) }
  OutlinedCard(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "收起发布设置" else if (model.isReply) "回复设置 · 匿名与可见性" else "发布设置 · 投票与权限") }
      if (expanded) {
        ComposerCheck(if (model.isReply) "匿名回复" else "匿名发帖", options.anonymous, enabled) { model.updateOptions(options.copy(anonymous = it)) }
        if (options.anonymous) Text(if (model.isReply) "匿名资格和费用以论坛规则为准。" else "网页提示：匿名主题需 5000 铜币，违规会加重处罚。", style = MaterialTheme.typography.bodySmall)
        ComposerCheck("隐藏内容，仅版主可见", options.hidden, enabled) { model.updateOptions(options.copy(hidden = it)) }
        if (!model.isReply) {
        ComposerCheck("只有作者和版主可回复", options.selfReply, enabled) { model.updateOptions(options.copy(selfReply = it)) }
        ComposerCheck("每个用户只能回复一次", options.replyOnce, enabled) { model.updateOptions(options.copy(replyOnce = it)) }
        if (options.replyOnce) Text("适用于前 2000 个回复，主题作者不受限制。", style = MaterialTheme.typography.bodySmall)
        ComposerCheck("创建合集主题", options.createCollection, enabled) { model.updateOptions(options.copy(createCollection = it)) }
        if (options.createCollection) Text("需论坛对应权限；非版主创建合集需 5 金币。", style = MaterialTheme.typography.bodySmall)
        if (model.preparation?.moderator == true) ComposerCheck("回复自动匿名", options.anonymousReplies, enabled) { model.updateOptions(options.copy(anonymousReplies = it)) }
        HorizontalDivider()
        val voteTypes = TopicVoteType.entries.filter { it == TopicVoteType.None || it == TopicVoteType.Poll || model.preparation?.moderator == true }
        ComposerChoice("投票类型", options.voteType.name, voteTypes.map { it.name to it.label }, enabled) { model.updateOptions(options.copy(voteType = TopicVoteType.valueOf(it))) }
        if (options.voteType != TopicVoteType.None) {
          Text("每行一项；多组投票在组名前加 ===。提交后选项不能修改。" + if (model.preparation?.moderator != true) "普通用户发布投票需 10 银币。" else "", style = MaterialTheme.typography.bodySmall)
          OutlinedTextField(options.voteItems, { model.updateOptions(options.copy(voteItems = it)) }, label = { Text("投票选项") }, minLines = 3, enabled = enabled, modifier = Modifier.fillMaxWidth())
          if (options.voteType != TopicVoteType.Score) OutlinedTextField(options.voteMax, { model.updateOptions(options.copy(voteMax = it)) }, label = { Text("每组最多可选项数（0 为不限）") }, enabled = enabled)
          OutlinedTextField(options.voteHours, { model.updateOptions(options.copy(voteHours = it)) }, label = { Text("几小时后结束（留空不限）") }, enabled = enabled)
          OutlinedTextField(options.voteReputation, { model.updateOptions(options.copy(voteReputation = it)) }, label = { Text("参与声望限制（留空不限）") }, enabled = enabled)
          if (options.voteType == TopicVoteType.Bet) {
            OutlinedTextField(options.voteMin, { model.updateOptions(options.copy(voteMin = it)) }, label = { Text("最少投注铜币") }, enabled = enabled)
            OutlinedTextField(options.voteBetMax, { model.updateOptions(options.copy(voteBetMax = it)) }, label = { Text("最多投注铜币") }, enabled = enabled)
          }
          if (options.voteType == TopicVoteType.Score) Text("评分范围：1–10 分")
          ComposerChoice("结果可见", options.voteVisibility.toString(), listOf("0" to "即时可见", "1" to "提交后可见", "2" to "结束后可见"), enabled) {
            model.updateOptions(options.copy(voteVisibility = it.toInt()))
          }
          options.validationError(model.preparation?.moderator == true)?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
        }
      }
    }
  }
}

@Composable
internal fun ComposerCheck(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
    Checkbox(checked, onChange, enabled = enabled)
    Text(label, Modifier.weight(1f).clickable(enabled = enabled) { onChange(!checked) }, style = MaterialTheme.typography.bodyMedium)
  }
}

@Composable
internal fun ComposerAttachmentSettings(model: TopicComposerViewModel) {
  val settings = model.uploadOptions
  ComposerChoice("图片处理", settings.compression, listOf("1" to "自动压缩", "8" to "转换 WebP", "5" to "PNG 压缩（保留透明）"), !model.busy) {
    model.updateUploadOptions(settings.copy(compression = it))
  }
  ComposerChoice("水印位置", settings.watermark, listOf("" to "无水印", "br" to "右下", "bl" to "左下", "tl" to "左上", "tr" to "右上", "cn" to "中央"), !model.busy) {
    model.updateUploadOptions(settings.copy(watermark = it))
  }
  Text("水印仅对处理后为 JPEG 的图片生效；设置用于之后的上传。", style = MaterialTheme.typography.bodySmall)
  OutlinedTextField(settings.description, { model.updateUploadOptions(settings.copy(description = it)) }, label = { Text("附件说明（可选）") }, enabled = !model.busy, modifier = Modifier.fillMaxWidth())
}
