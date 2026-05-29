package com.yanga.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BoardTopicListScreen(
  state: BoardTopicListUiState,
  onBack: () -> Unit,
  onTopicClick: (TopicPreview) -> Unit,
  onToggleFavorite: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Marker(text = state.boardName, iconUrl = state.iconUrl, modifier = Modifier.size(32.dp))
            Column {
              Text(text = state.boardName, style = MaterialTheme.typography.titleMedium)
              if (state.fid.isNotBlank()) {
                Text(
                  text = "fid: ${state.fid}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = onToggleFavorite) {
            Icon(
              imageVector = if (state.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
              contentDescription = if (state.isFavorite) "Unfavorite board" else "Favorite board",
            )
          }
        }
      )
    },
    floatingActionButton = {
      FloatingActionButton(onClick = {}) {
        Icon(Icons.Filled.Add, contentDescription = "Add")
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .padding(paddingValues)
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      SearchPill(text = "Search in ${state.boardName}")
      FilterChipRow(labels = listOf("All", "Essence", "Latest reply", "Favorites"))

      SectionHeader(title = "Pinned topics")
      TonalCard {
        Text("Pinned topics would go here", style = MaterialTheme.typography.bodyMedium)
      }

      SectionHeader(title = "Topics")
      TonalCard {
        when (state.topics) {
          is LoadableUiState.Content -> {
            for (topic in state.topics.value) {
              TopicRow(topic = topic, onClick = { onTopicClick(topic) })
            }
          }
          else -> {
            Text("No topics available", style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThreadReadingScreen(
  state: ThreadUiState,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = state.title,
              style = MaterialTheme.typography.titleMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "Page ${state.page} · ${state.replyCount} replies",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = {}) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More")
          }
        }
      )
    },
    bottomBar = {
      ThreadBottomBar()
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .padding(paddingValues)
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      ThreadTitleCard(title = state.title)
      FilterChipRow(labels = listOf("All", "Author only", "Images only", "Jump floor"))

      when (state.posts) {
        is LoadableUiState.Content -> {
          for (post in state.posts.value) {
            PostItem(post = post)
          }
        }
        else -> {
          TonalCard {
            Text("Loading posts...", style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
    }
  }
}

@Composable
private fun ThreadTitleCard(title: String, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    shape = MaterialTheme.shapes.large
  ) {
    Text(
      text = title,
      modifier = Modifier.padding(16.dp),
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSecondaryContainer
    )
  }
}

@Composable
private fun PostItem(post: PostPreview, modifier: Modifier = Modifier) {
  val contentParts = remember(post.content) { parsePostContent(post.content) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.tertiaryContainer),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = post.avatarInitial,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onTertiaryContainer
        )
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(text = post.author, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
          text = "${post.floor} · ${post.time}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    for (part in contentParts) {
      when (part) {
        is ContentPart.Text -> {
          Text(
            text = part.text,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3f
          )
        }
        is ContentPart.Quote -> {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = MaterialTheme.shapes.small,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
          ) {
            Text(
              text = part.text,
              modifier = Modifier.padding(12.dp),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
          }
        }
      }
    }
  }
}

private sealed class ContentPart {
  data class Text(val text: String) : ContentPart()
  data class Quote(val text: String) : ContentPart()
}

private fun parsePostContent(content: String): List<ContentPart> {
  val parts = mutableListOf<ContentPart>()
  val current = content
    .replace("<br/>", "\n")
    .replace("<br />", "\n")
    .replace("&nbsp;", " ")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&amp;", "&")

  val quoteRegex = Regex("\\[quote\\]([\\s\\S]*?)\\[/quote\\]")
  var match = quoteRegex.find(current)
  var lastIndex = 0

  while (match != null) {
    if (match.range.first > lastIndex) {
      val textBefore = current.substring(lastIndex, match.range.first).trim()
      if (textBefore.isNotEmpty()) {
        parts.add(ContentPart.Text(textBefore))
      }
    }

    val quoteContent = match.groupValues[1].trim()
    if (quoteContent.isNotEmpty()) {
      parts.add(ContentPart.Quote(quoteContent))
    }

    lastIndex = match.range.last + 1
    match = quoteRegex.find(current, lastIndex)
  }

  if (lastIndex < current.length) {
    val remaining = current.substring(lastIndex).trim()
    if (remaining.isNotEmpty()) {
      parts.add(ContentPart.Text(remaining))
    }
  }

  return if (parts.isEmpty() && current.isNotBlank()) {
    listOf(ContentPart.Text(current))
  } else {
    parts
  }
}

@Composable
private fun ThreadBottomBar() {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tonalElevation = 3.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Surface(
        modifier = Modifier.weight(1f),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
      ) {
        Text(
          text = "Reply to thread...",
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      IconButton(onClick = {}) {
        Icon(Icons.Filled.Favorite, contentDescription = "Favorite")
      }
      IconButton(onClick = {}) {
        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
      }
    }
  }
}








