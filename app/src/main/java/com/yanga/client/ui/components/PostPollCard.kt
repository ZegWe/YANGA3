package com.yanga.client.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.yanga.client.api.NgaPoll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal typealias PollSubmit = suspend (NgaPoll, List<Int>) -> Result<NgaPoll?>

@Composable
internal fun PostPollCard(poll: NgaPoll, onVote: PollSubmit? = null, onOpenWeb: () -> Unit = {}) {
  var displayed by remember(poll) { mutableStateOf(poll) }
  var selected by rememberSaveable(poll.tid, poll.options.map { it.id }) { mutableStateOf(arrayListOf<Int>()) }
  var submitted by rememberSaveable(poll.tid) { mutableStateOf(false) }
  var submitting by remember { mutableStateOf(false) }
  var message by remember { mutableStateOf<String?>(null) }
  var now by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
  val scope = rememberCoroutineScope()
  LaunchedEffect(poll.endsAt) {
    while (poll.endsAt != null && !displayed.isClosed(now)) {
      delay(((poll.endsAt - now).coerceAtMost(60) * 1000).coerceAtLeast(1))
      now = System.currentTimeMillis() / 1000
    }
  }
  val closed = displayed.isClosed(now)
  val editable = onVote != null && !closed && !submitting && !submitted && !poll.isBet
  Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text(if (poll.isBet) "投注结果" else "投票", style = MaterialTheme.typography.titleMedium)
      Text(if (closed) "投票已结束" else "最多选择 ${poll.maxSelections} 项", style = MaterialTheme.typography.labelMedium)
      val total = displayed.totalVotes
      displayed.options.forEach { option ->
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(
            Modifier.fillMaxWidth().toggleable(
              value = option.id in selected, enabled = editable,
              role = if (poll.maxSelections == 1) Role.RadioButton else Role.Checkbox,
              onValueChange = { checked ->
                message = null
                selected = when {
                  !checked -> ArrayList(selected - option.id)
                  poll.maxSelections == 1 -> arrayListOf(option.id)
                  selected.size < poll.maxSelections -> ArrayList(selected + option.id)
                  else -> { message = "最多选择 ${poll.maxSelections} 项"; selected }
                }
              },
            ), verticalAlignment = Alignment.CenterVertically,
          ) {
            if (!closed && !poll.isBet) {
              if (poll.maxSelections == 1) RadioButton(option.id in selected, onClick = null, enabled = editable)
              else Checkbox(option.id in selected, onCheckedChange = null, enabled = editable)
              Spacer(Modifier.width(8.dp))
            }
            Text(option.label, Modifier.weight(1f))
            val percentage = if (total != null && total > 0 && option.votes != null) option.votes.toDouble() / total else 0.0
            Text(option.votes?.let { "$it 票 · ${String.format(Locale.ROOT, "%.1f", percentage * 100)}%" } ?: "票数未公开",
              style = MaterialTheme.typography.labelSmall)
          }
          if (option.votes != null && total != null) LinearProgressIndicator(
            progress = { if (total > 0) (option.votes.toDouble() / total).toFloat().coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth(),
          )
          if (poll.isBet) option.stake?.let { Text("投注 $it 铜币", style = MaterialTheme.typography.labelSmall) }
        }
      }
      Text(buildList {
        displayed.participants?.let { add("$it 人参与") }
        total?.let { add("共 $it 票") }
        displayed.endsAt?.let { add("截止 ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it * 1000))}") }
      }.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
      if (poll.isBet) TextButton(onClick = onOpenWeb) { Text("在网页中查看") }
      else if (!closed && !submitted) {
        Button(enabled = editable && selected.isNotEmpty(), onClick = {
          if (submitting) return@Button
          val ids = selected.toList()
          poll.validationError(ids)?.let { message = it; return@Button }
          submitting = true
          message = null
          scope.launch {
            try {
              val result = onVote?.invoke(poll, ids) ?: Result.failure(IllegalStateException("请先登录"))
              result.fold(onSuccess = { fresh ->
                submitted = true
                if (fresh != null) displayed = fresh
                message = if (fresh == null) "投票成功，结果暂未刷新，请重新打开帖子查看" else "投票成功"
              }, onFailure = { message = it.message ?: "投票失败，请稍后重试" })
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { message = error.message ?: "投票失败" }
            finally { submitting = false }
          }
        }) { Text(if (submitting) "提交中…" else "提交投票") }
        if (onVote == null) Text("登录后可投票", style = MaterialTheme.typography.bodySmall)
      }
      if (submitted && message == null) Text("已提交投票")
      message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
  }
}
