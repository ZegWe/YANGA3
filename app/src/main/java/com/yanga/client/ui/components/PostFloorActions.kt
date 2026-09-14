package com.yanga.client.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
internal fun PostFloorActions(
  post: PostPreview,
  onReply: (PostPreview) -> Unit,
  onReact: (suspend (PostPreview, Boolean) -> Result<Int?>)?,
) {
  var busy by remember { mutableStateOf(false) }
  var reaction by rememberSaveable(post.pid) { mutableIntStateOf(0) }
  var message by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()
  fun react(support: Boolean) {
    if (busy) return
    if (onReact == null) { message = "请先登录后再赞踩"; return }
    busy = true
    message = null
    scope.launch {
      try {
        onReact(post, support).fold(
          onSuccess = {
            val clicked = if (support) 1 else -1
            reaction = if (reaction == clicked) 0 else clicked
          },
          onFailure = { message = it.message ?: "操作失败，请稍后重试" },
        )
      } finally { busy = false }
    }
  }
  val liked = onReact != null && reaction == 1
  val disliked = onReact != null && reaction == -1
  val likeColor = if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
  val dislikeColor = if (disliked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
  Column {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      TextButton(onClick = { react(true) }, enabled = !busy,
        modifier = Modifier.semantics {
          selected = liked
          stateDescription = if (liked) "已点赞" else "未标记点赞"
        },
        colors = ButtonDefaults.textButtonColors(contentColor = likeColor, disabledContentColor = likeColor),
        contentPadding = PaddingValues(horizontal = 8.dp)) {
        Icon(if (liked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
          contentDescription = "点赞", Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(post.score.toString())
      }
      IconButton(onClick = { react(false) }, enabled = !busy,
        modifier = Modifier.semantics {
          selected = disliked
          stateDescription = if (disliked) "已点踩" else "未标记点踩"
        },
        colors = IconButtonDefaults.iconButtonColors(contentColor = dislikeColor, disabledContentColor = dislikeColor)) {
        Icon(if (disliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
          contentDescription = "点踩", Modifier.size(18.dp))
      }
      Spacer(Modifier.weight(1f))
      IconButton(onClick = { onReply(post) }) {
        Icon(Icons.AutoMirrored.Outlined.Reply, contentDescription = "回复", Modifier.size(20.dp))
      }
    }
    message?.let { error ->
      AlertDialog(
        onDismissRequest = { message = null },
        title = { Text("赞踩未完成") },
        text = { Text(error) },
        confirmButton = { TextButton(onClick = { message = null }) { Text("知道了") } },
      )
    }
  }
}
