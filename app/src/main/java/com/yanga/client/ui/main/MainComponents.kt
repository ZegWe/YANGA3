package com.yanga.client.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yanga.client.R

@Composable
internal fun PageHeader(title: String, subtitle: String, modifier: Modifier = Modifier) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.SemiBold,
    )
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
internal fun SearchPill(text: String, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surfaceContainer,
    shape = MaterialTheme.shapes.extraLarge,
  ) {
    Text(
      text = text,
      modifier = Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun FilterChipRow(
  labels: List<String>,
  selectedIndex: Int = 0,
  modifier: Modifier = Modifier,
) {
  FlowRow(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    labels.forEachIndexed { index, label ->
      FilterChip(selected = index == selectedIndex, onClick = {}, label = { Text(label) })
    }
  }
}

@Composable
internal fun SectionHeader(
  title: String,
  trailing: String? = null,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    trailing?.let {
      Text(
        text = it,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

@Composable
internal fun Marker(text: String, iconUrl: String? = null, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.size(42.dp),
    contentAlignment = Alignment.Center,
  ) {
    if (!iconUrl.isNullOrBlank()) {
      AsyncImage(
        model = iconUrl,
        contentDescription = null,
        placeholder = painterResource(id = R.drawable.default_board_icon),
        error = painterResource(id = R.drawable.default_board_icon),
        fallback = painterResource(id = R.drawable.default_board_icon),
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      androidx.compose.foundation.Image(
        painter = painterResource(id = R.drawable.default_board_icon),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}

@Composable
internal fun RoundMarker(text: String, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier
      .size(42.dp)
      .clip(MaterialTheme.shapes.extraLarge),
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.tertiaryContainer,
  ) {
    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = text.take(1),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
internal fun BoardListRow(
  board: BoardPreview,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Marker(text = board.marker, iconUrl = board.iconUrl)
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(text = board.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
      Text(
        text = board.metadata,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
    board.badge?.let { Badge { Text(it) } }
  }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun TopicRow(
  topic: TopicPreview,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    RoundMarker(text = topic.authorInitial)
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
      Text(
        text = topic.title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(topic.replies, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(topic.board, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(topic.lastActive, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

@Composable
internal fun TonalCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    shape = MaterialTheme.shapes.large,
    content = {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
      )
    },
  )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun ActionChipRow(labels: List<String>, modifier: Modifier = Modifier) {
  FlowRow(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    labels.forEach { AssistChip(onClick = {}, label = { Text(it) }) }
  }
}
