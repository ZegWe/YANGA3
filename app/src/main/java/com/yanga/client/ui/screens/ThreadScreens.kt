package com.yanga.client.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yanga.client.data.image.imageCacheManager
import androidx.compose.material3.HorizontalDivider
import com.yanga.client.ui.components.CachedPostImage
import com.yanga.client.ui.components.PrefetchUserAvatars
import com.yanga.client.ui.components.TopicListItem
import com.yanga.client.ui.components.UserAvatar
import com.yanga.client.ui.content.PostContentPart
import com.yanga.client.ui.content.PostContentParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            Marker(
              text = state.boardName,
              iconUrl = state.iconUrl,
              modifier = Modifier.size(32.dp),
              iconSize = 32.dp,
            )
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
            state.topics.value.forEachIndexed { index, topic ->
              TopicListItem(topic = topic, onClick = { onTopicClick(topic) })
              if (index < state.topics.value.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
              }
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
  var menuExpanded by remember { mutableStateOf(false) }
  var isFavorited by remember { mutableStateOf(false) }
  var showJumpFloorDialog by remember { mutableStateOf(false) }

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
          Box {
            IconButton(onClick = { menuExpanded = true }) {
              Icon(Icons.Filled.MoreVert, contentDescription = "More")
            }
            ThreadOverflowMenu(
              expanded = menuExpanded,
              isFavorited = isFavorited,
              onDismiss = { menuExpanded = false },
              onToggleFavorite = {
                isFavorited = !isFavorited
                menuExpanded = false
              },
              onJumpFloor = {
                menuExpanded = false
                showJumpFloorDialog = true
              },
            )
          }
        }
      )
    },
    floatingActionButton = {
      FloatingActionButton(onClick = {}) {
        Icon(Icons.Filled.Edit, contentDescription = "Reply")
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
      ThreadTitleCard(title = state.title)
      FilterChipRow(labels = listOf("All", "Author only", "Images only"))

      when (state.posts) {
        is LoadableUiState.Content -> {
          PrefetchPostImages(posts = state.posts.value)
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

  if (showJumpFloorDialog) {
    JumpFloorDialog(
      onDismiss = { showJumpFloorDialog = false },
      onConfirm = { showJumpFloorDialog = false },
    )
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
  val contentParts = remember(post.content) { PostContentParser.parse(post.content) }

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
      UserAvatar(
        name = post.author,
        avatarUrl = post.authorAvatarUrl,
        modifier = Modifier.size(40.dp),
        size = 40.dp,
      )
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
        is PostContentPart.Text -> {
          Text(
            text = part.text,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3f
          )
        }
        is PostContentPart.Quote -> {
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
        is PostContentPart.Image -> {
          CachedPostImage(url = part.url)
        }
      }
    }
  }
}

@Composable
private fun PrefetchPostImages(posts: List<PostPreview>) {
  PrefetchUserAvatars(posts.map { it.authorAvatarUrl })

  val context = LocalContext.current
  val imageUrls =
    remember(posts) {
      posts.flatMap { post ->
        PostContentParser.parse(post.content).filterIsInstance<PostContentPart.Image>().map { it.url }
      }.distinct()
    }

  LaunchedEffect(imageUrls) {
    if (imageUrls.isEmpty()) return@LaunchedEffect
    withContext(Dispatchers.IO) {
      context.imageCacheManager().prefetchAll(context, imageUrls)
    }
  }
}

@Composable
private fun ThreadOverflowMenu(
  expanded: Boolean,
  isFavorited: Boolean,
  onDismiss: () -> Unit,
  onToggleFavorite: () -> Unit,
  onJumpFloor: () -> Unit,
) {
  DropdownMenu(
    expanded = expanded,
    onDismissRequest = onDismiss,
  ) {
    DropdownMenuItem(
      text = { Text(if (isFavorited) "取消收藏" else "收藏") },
      onClick = onToggleFavorite,
      leadingIcon = {
        Icon(
          imageVector = if (isFavorited) Icons.Filled.Star else Icons.Outlined.StarBorder,
          contentDescription = null,
        )
      },
    )
    DropdownMenuItem(
      text = { Text("跳楼") },
      onClick = onJumpFloor,
    )
  }
}

@Composable
private fun JumpFloorDialog(
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var floorInput by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("跳楼") },
    text = {
      OutlinedTextField(
        value = floorInput,
        onValueChange = { floorInput = it.filter { char -> char.isDigit() } },
        label = { Text("楼层") },
        singleLine = true,
      )
    },
    confirmButton = {
      TextButton(
        onClick = { onConfirm(floorInput) },
        enabled = floorInput.isNotBlank(),
      ) {
        Text("跳转")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("取消")
      }
    },
  )
}








