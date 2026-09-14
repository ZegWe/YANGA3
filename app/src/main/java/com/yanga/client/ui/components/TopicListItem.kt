package com.yanga.client.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yanga.client.ui.TopicPreview
import com.yanga.client.ui.TopicNavigationTarget

@Composable
internal fun TopicListItem(
  topic: TopicPreview,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isClickable: Boolean = true,
) {
  val itemModifier =
    if (isClickable) {
      modifier.clickable(onClick = onClick)
    } else {
      modifier
    }
  Column(
    modifier =
      itemModifier
        .fillMaxWidth()
        .padding(vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(7.dp),
  ) {
    Text(
      text = topic.title,
      style = MaterialTheme.typography.titleMedium,
      color = topic.titleStyle.color.topicTitleColor(),
      fontWeight = if (topic.titleStyle.bold) FontWeight.Bold else FontWeight.SemiBold,
      fontStyle = if (topic.titleStyle.italic) FontStyle.Italic else FontStyle.Normal,
      textDecoration = if (topic.titleStyle.underline) TextDecoration.Underline else TextDecoration.None,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )

    val labels = buildList {
      if (topic.isLocked) add("锁定")
      val destination = (topic.navigationTarget as? TopicNavigationTarget.Board)?.destination
      if (destination != null) add(if (destination.id.startsWith('t')) "合集" else "版面")
      if (topic.hasAttachments) add("附件")
      if (topic.board.isNotBlank()) add(topic.board)
    }
    if (labels.isNotEmpty()) {
      Text(
        text = labels.joinToString(" · "),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f, fill = false),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Outlined.Person,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = topic.authorDisplayName(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Outlined.ChatBubbleOutline,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = topic.replyCount.toString(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
        )
      }
    }
  }
}

private fun TopicPreview.authorDisplayName(): String =
  authorName.ifBlank { "匿名" }

@Composable
private fun String?.topicTitleColor(): Color {
  // Keep moderator colors readable on both light and dark surfaces.
  val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
  return when (this) {
    "red" -> if (dark) Color(0xFFFFB4AB) else Color(0xFFB3261E)
    "blue" -> if (dark) Color(0xFFA8C7FA) else Color(0xFF245EA8)
    "green" -> if (dark) Color(0xFF8CD6A0) else Color(0xFF246D38)
    "orange" -> if (dark) Color(0xFFFFB877) else Color(0xFF895000)
    "silver" -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.onSurface
  }
}
