package com.yanga.client.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yanga.client.ui.content.PostContentPart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PostFloorMenu(
  post: PostPreview,
  parts: List<PostContentPart>,
  onDismiss: () -> Unit,
  onFilter: (PostPreview) -> Unit,
) {
  val context = LocalContext.current
  val canFilter = post.authorId.toIntOrNull()?.let { it > 0 } == true
  ModalBottomSheet(onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
      Text("楼层操作", Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.titleLarge)
      Text("${post.author} · ${post.floor}", Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      ListItem(
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        headlineContent = { Text("复制内容") },
        leadingContent = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
        modifier = Modifier.clickable {
          val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
          clipboard.setPrimaryClip(ClipData.newPlainText("帖子内容", postCopyText(parts)))
          onDismiss()
        },
      )
      ListItem(
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        headlineContent = { Text("只看该作者", color = if (canFilter) MaterialTheme.colorScheme.onSurface
          else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)) },
        leadingContent = { Icon(Icons.Outlined.Person, contentDescription = null,
          tint = if (canFilter) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)) },
        modifier = Modifier.clickable(enabled = canFilter) { onDismiss(); onFilter(post) },
      )
    }
  }
}

/** Copy readable content, including collapsed text and media URLs, without BBCode wrappers. */
internal fun postCopyText(parts: List<PostContentPart>): String = parts.joinToString("") { part ->
  when (part) {
    is PostContentPart.Text -> part.text
    is PostContentPart.Emoticon -> part.alt
    is PostContentPart.Image -> "\n${part.url}\n"
    is PostContentPart.Attachment -> "\n${part.name} ${part.url}\n"
    is PostContentPart.Audio -> "\n${part.label} ${part.url}\n"
    is PostContentPart.Video -> "\n${part.label} ${part.url}\n"
    is PostContentPart.Heading -> "\n${postCopyText(part.parts)}\n"
    is PostContentPart.Quote -> "\n${postCopyText(part.parts)}\n"
    is PostContentPart.Collapse -> "\n${part.title}\n${postCopyText(part.parts)}\n"
    is PostContentPart.Code -> "\n${part.text}\n"
    is PostContentPart.ListBlock -> "\n" + part.items.mapIndexed { index, item ->
      "${listMarker(part.marker, index)} ${postCopyText(item)}"
    }.joinToString("\n") + "\n"
    is PostContentPart.Table -> "\n" + part.rows.joinToString("\n") { row -> row.joinToString("\t") { postCopyText(it) } } + "\n"
    PostContentPart.Rule -> "\n—\n"
  }
}.trim()
