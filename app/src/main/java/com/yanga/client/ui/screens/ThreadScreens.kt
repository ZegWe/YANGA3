package com.yanga.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yanga.client.data.image.imageCacheManager
import androidx.compose.material3.HorizontalDivider
import com.yanga.client.ui.components.CachedAsyncImage
import com.yanga.client.ui.components.CachedPostImage
import com.yanga.client.ui.components.PostAudioPlayer
import com.yanga.client.ui.components.PrefetchUserAvatars
import com.yanga.client.ui.components.SubBoardDirectorySheet
import com.yanga.client.ui.components.TopicListItem
import com.yanga.client.ui.components.UserAvatar
import com.yanga.client.ui.content.PostContentPart
import com.yanga.client.ui.content.PostContentParser
import com.yanga.client.ui.content.PostTextStyleRange
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun BoardTopicListScreen(
  state: BoardTopicListUiState,
  onBack: () -> Unit,
  onTopicClick: (TopicPreview) -> Unit,
  onToggleFavorite: () -> Unit,
  onSelectAllSubBoards: () -> Unit = {},
  onSetSubBoardEnabled: (String, Boolean) -> Unit = { _, _ -> },
  onOpenSubBoard: (SubBoardOption) -> Unit = {},
  onTopicFilterChange: (BoardTopicFilter) -> Unit = {},
  onRefresh: () -> Unit = {},
  onLoadNextPage: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  var showSubBoardSheet by remember { mutableStateOf(false) }
  val subBoardOptions = (state.subBoards as? LoadableUiState.Content)?.value.orEmpty()
  val hasSubBoards = subBoardOptions.isNotEmpty()
  val topicListState = rememberLazyListState()
  LaunchedEffect(topicListState, state.hasNextTopicPage, state.isLoadingNextTopicPage, state.topics) {
    if (!state.hasNextTopicPage || state.isLoadingNextTopicPage || state.topics !is LoadableUiState.Content) {
      return@LaunchedEffect
    }
    snapshotFlow {
      val layoutInfo = topicListState.layoutInfo
      val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
      lastVisibleIndex >= layoutInfo.totalItemsCount - 3
    }
      .distinctUntilChanged()
      .collect { shouldLoad ->
        if (shouldLoad) onLoadNextPage()
      }
  }
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
          IconButton(onClick = {}) {
            Icon(
              imageVector = Icons.Outlined.Search,
              contentDescription = "搜索",
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
        .fillMaxSize(),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          FilterChipRow(
            labels = listOf("全部", "精华"),
            selectedIndex = if (state.selectedTopicFilter == BoardTopicFilter.Recommend) 1 else 0,
            onSelectedIndexChange = { index ->
              onTopicFilterChange(if (index == 1) BoardTopicFilter.Recommend else BoardTopicFilter.All)
            },
            modifier = if (hasSubBoards) Modifier.weight(1f) else Modifier.fillMaxWidth(),
          )
          if (hasSubBoards) {
            TextButton(onClick = { showSubBoardSheet = true }) {
              Text("子版块")
            }
          }
        }
      }

      PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
      ) {
        when (val topics = state.topics) {
          LoadableUiState.Loading -> {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center,
            ) {
              LoadingIndicator(modifier = Modifier.size(64.dp))
            }
          }
          else -> {
            LazyColumn(
              state = topicListState,
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              when (topics) {
                is LoadableUiState.Content -> {
                  if (topics.value.isEmpty()) {
                    item(key = "topics-empty") {
                      TonalCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                          Text(
                            text =
                              if (state.selectedSubBoardIds.isNotEmpty()) {
                                "No topics match the current sub-board filter"
                              } else {
                                "No topics available"
                              },
                            style = MaterialTheme.typography.bodyMedium,
                          )
                          if (state.selectedSubBoardIds.isNotEmpty()) {
                            TextButton(onClick = onSelectAllSubBoards) {
                              Text("Show all sub-boards")
                            }
                          }
                        }
                      }
                    }
                  } else {
                    itemsIndexed(
                      items = topics.value,
                      key = { _, topic -> topic.id },
                    ) { _, topic ->
                      Card(
                        onClick = { onTopicClick(topic) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                          containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        shape = MaterialTheme.shapes.large,
                      ) {
                        TopicListItem(
                          topic = topic,
                          onClick = { onTopicClick(topic) },
                          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                          isClickable = false,
                        )
                      }
                    }
                    if (state.isLoadingNextTopicPage) {
                      item(key = "topics-loading-next") {
                        Box(
                          modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                          contentAlignment = Alignment.Center,
                        ) {
                          LoadingIndicator(modifier = Modifier.size(36.dp))
                        }
                      }
                    }
                  }
                }
                is LoadableUiState.Empty -> {
                  item(key = "topics-empty-state") {
                    TonalCard {
                      Text(topics.message, style = MaterialTheme.typography.bodyMedium)
                    }
                  }
                }
                is LoadableUiState.Error -> {
                  item(key = "topics-error") {
                    TonalCard {
                      Text(
                        text = topics.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                      )
                    }
                  }
                }
                LoadableUiState.LoginRequired -> {
                  item(key = "topics-login-required") {
                    TonalCard {
                      Text("Sign in to load topics", style = MaterialTheme.typography.bodyMedium)
                    }
                  }
                }
                LoadableUiState.Loading -> Unit
              }
            }
          }
        }
      }
    }
  }

  if (showSubBoardSheet && hasSubBoards) {
    SubBoardDirectorySheet(
      boardName = state.boardName,
      options = subBoardOptions,
      selectedIds = state.selectedSubBoardIds,
      onDismiss = { showSubBoardSheet = false },
      onSetSubBoardEnabled = onSetSubBoardEnabled,
      onSelectAllSubBoards = onSelectAllSubBoards,
      onOpenSubBoard = { option ->
        showSubBoardSheet = false
        onOpenSubBoard(option)
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThreadReadingScreen(
  state: ThreadUiState,
  onBack: () -> Unit,
  onOpenInBrowser: () -> Unit = {},
  onPageChange: (Int) -> Unit = {},
  onFloorJump: (Int) -> Unit = {},
  onReplyClick: () -> Unit = {},
  onLinkClick: (String) -> Unit = {},
  onAttachmentDownload: (PostAttachmentPreview) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  var menuExpanded by remember { mutableStateOf(false) }
  var isFavorited by remember { mutableStateOf(false) }
  var showQuickJumpSheet by remember { mutableStateOf(false) }
  var pendingAttachment by remember { mutableStateOf<PostAttachmentPreview?>(null) }
  var previewImageUrls by remember { mutableStateOf(emptyList<String>()) }
  var previewImageIndex by remember { mutableStateOf<Int?>(null) }
  val currentPage = state.page.toIntOrNull()?.coerceAtLeast(1) ?: 1
  val maxPage = (state.maxPage.toIntOrNull() ?: currentPage).coerceAtLeast(currentPage)
  val pagerState =
    rememberPagerState(initialPage = currentPage - 1) {
      maxPage
    }

  LaunchedEffect(currentPage, maxPage) {
    val targetPage = (currentPage - 1).coerceIn(0, maxPage - 1)
    if (pagerState.currentPage != targetPage) {
      pagerState.scrollToPage(targetPage)
    }
  }

  LaunchedEffect(pagerState.currentPage, currentPage) {
    val selectedPage = pagerState.currentPage + 1
    if (selectedPage != currentPage) {
      onPageChange(selectedPage)
    }
  }

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
              text = "第 $currentPage / $maxPage 页 · 左右滑动翻页",
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
              onOpenInBrowser = {
                menuExpanded = false
                onOpenInBrowser()
              },
            )
          }
        }
      )
    }
  ) { paddingValues ->
    Box(
      modifier =
        Modifier
          .padding(paddingValues)
          .fillMaxSize(),
    ) {
      HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 0,
        key = { page -> page },
        modifier = Modifier.fillMaxSize(),
      ) { page ->
        ThreadPageContent(
          state = state,
          pageNumber = page + 1,
          onImageUrlsChange = { urls, index ->
            previewImageUrls = urls
            previewImageIndex = index
          },
          onLinkClick = onLinkClick,
          onAttachmentClick = { attachment -> pendingAttachment = attachment },
        )
      }
      Column(
        modifier =
          Modifier
            .align(Alignment.BottomEnd)
            .navigationBarsPadding()
            .padding(end = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        FloatingActionButton(
          onClick = { showQuickJumpSheet = true },
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
          Text(
            text = "$currentPage/$maxPage",
            modifier = Modifier.padding(horizontal = 12.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
          )
        }
        FloatingActionButton(onClick = onReplyClick) {
          Icon(Icons.Filled.Edit, contentDescription = "Reply")
        }
      }
    }
  }

  if (showQuickJumpSheet) {
    ThreadQuickJumpSheet(
      currentPage = currentPage,
      maxPage = maxPage,
      maxFloor = state.replyCount.toIntOrNull()?.coerceAtLeast(0) ?: 0,
      onDismiss = { showQuickJumpSheet = false },
      onPageJump = { page ->
        showQuickJumpSheet = false
        onPageChange(page)
      },
      onFloorJump = { floor ->
        showQuickJumpSheet = false
        onFloorJump(floor)
      },
    )
  }

  pendingAttachment?.let { attachment ->
    AttachmentDownloadDialog(
      attachment = attachment,
      onDismiss = { pendingAttachment = null },
      onConfirm = {
        pendingAttachment = null
        onAttachmentDownload(attachment)
      },
    )
  }

  val activePreviewIndex = previewImageIndex
  if (activePreviewIndex != null && previewImageUrls.isNotEmpty()) {
    ImagePreviewDialog(
      imageUrls = previewImageUrls,
      currentIndex = activePreviewIndex.coerceIn(0, previewImageUrls.lastIndex),
      onIndexChange = { previewImageIndex = it.coerceIn(0, previewImageUrls.lastIndex) },
      onDismiss = { previewImageIndex = null },
    )
  }
}

@Composable
private fun ThreadPageContent(
  state: ThreadUiState,
  pageNumber: Int,
  onImageUrlsChange: (List<String>, Int) -> Unit,
  onLinkClick: (String) -> Unit,
  onAttachmentClick: (PostAttachmentPreview) -> Unit,
) {
  val currentPage = state.page.toIntOrNull()
  val posts =
    when {
      pageNumber == currentPage && state.posts is LoadableUiState.Content -> state.posts.value
      else -> state.cachedPostsByPage[pageNumber]
    }

  if (posts != null) {
      val listState = rememberLazyListState()
      PrefetchPostImages(posts = posts)

      LaunchedEffect(state.targetPostId, state.targetFloorNumber, posts, pageNumber, currentPage) {
        if (pageNumber != currentPage) return@LaunchedEffect
        val targetIndex =
          when {
            state.targetPostId != null -> posts.indexOfFirst { it.pid == state.targetPostId }
            state.targetFloorNumber != null -> posts.indexOfFirst { it.floorNumber == state.targetFloorNumber }
            else -> -1
          }
        if (targetIndex >= 0) {
          listState.animateScrollToItem(index = targetIndex + THREAD_HEADER_ITEM_COUNT)
        }
      }

      LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        itemsIndexed(
          items = posts,
          key = { index, post -> post.pid.ifBlank { "post-$index-${post.floor}" } },
        ) { _, post ->
            PostItem(
              post = post,
              title = state.title.takeIf { post.floorNumber == 0 },
              onImageClick = onImageUrlsChange,
              onLinkClick = onLinkClick,
              onAttachmentClick = onAttachmentClick,
            )
        }
      }
  } else {
    ThreadPageLoadingIndicator()
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThreadPageLoadingIndicator() {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    LoadingIndicator(modifier = Modifier.size(64.dp))
  }
}

private const val THREAD_HEADER_ITEM_COUNT = 0

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
private fun PostItem(
  post: PostPreview,
  title: String? = null,
  modifier: Modifier = Modifier,
  onImageClick: (List<String>, Int) -> Unit = { _, _ -> },
  onLinkClick: (String) -> Unit = {},
  onAttachmentClick: (PostAttachmentPreview) -> Unit = {},
) {
  val contentParts = remember(post.content) { PostContentParser.parse(post.content) }
  val contentBlocks = remember(contentParts) { groupPostContentParts(contentParts) }
  val imageUrls = remember(post) { postPreviewImageUrls(post) }

  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .semantics { contentDescription = "Post card ${post.floor}" },
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    shape = MaterialTheme.shapes.medium,
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
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

      if (!title.isNullOrBlank()) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }

      for (block in contentBlocks) {
        when (block) {
          is PostContentBlock.Inline -> {
            PostInlineRichText(items = block.items, onLinkClick = onLinkClick)
          }
          is PostContentBlock.Quote -> {
            PostQuoteBlock(
              parts = block.part.parts,
              imageUrls = imageUrls,
              onLinkClick = onLinkClick,
              onImageClick = onImageClick,
            )
          }
          is PostContentBlock.Image -> {
            CachedPostImage(
              url = block.part.url,
              onClick = { onImageClick(imageUrls, imageUrls.indexOf(block.part.url).takeIf { it >= 0 } ?: 0) },
            )
          }
          is PostContentBlock.Audio -> {
            PostAudioPlayer(
              url = block.part.url,
              label = block.part.label,
            )
          }
        }
      }

      if (post.embeddedComments.isNotEmpty()) {
        PostSectionDivider()
        PostBodySection(title = "评论") {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            post.embeddedComments.forEach { reply ->
              PostEmbeddedReplyItem(
                reply = reply,
                onLinkClick = onLinkClick,
                onImageClick = onImageClick,
              )
            }
          }
        }
      }

      if (post.hotReplies.isNotEmpty()) {
        PostSectionDivider()
        PostBodySection(title = "热点回复") {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            post.hotReplies.forEach { reply ->
              PostEmbeddedReplyItem(
                reply = reply,
                onLinkClick = onLinkClick,
                onImageClick = onImageClick,
              )
            }
          }
        }
      }

      if (post.attachments.isNotEmpty()) {
        PostSectionDivider()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          post.attachments.forEach { attachment ->
            AttachmentRow(
              attachment = attachment,
              onClick = { onAttachmentClick(attachment) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PostSectionDivider(modifier: Modifier = Modifier) {
  HorizontalDivider(
    modifier = modifier.padding(vertical = 4.dp),
    color = MaterialTheme.colorScheme.outlineVariant,
  )
}

@Composable
private fun PostBodySection(
  title: String,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    content()
  }
}

@Composable
private fun PostEmbeddedReplyItem(
  reply: PostEmbeddedReplyPreview,
  onLinkClick: (String) -> Unit,
  onImageClick: (List<String>, Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val contentParts = remember(reply.content) { PostContentParser.parse(reply.content) }
  val contentBlocks = remember(contentParts) { groupPostContentParts(contentParts) }
  val imageUrls = remember(reply) { embeddedReplyPreviewImageUrls(reply) }

  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Column(
      modifier = Modifier.width(56.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      UserAvatar(
        name = reply.author,
        avatarUrl = reply.authorAvatarUrl,
        modifier = Modifier.size(36.dp),
        size = 36.dp,
      )
      Text(
        text = reply.author,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }

    Surface(
      modifier = Modifier.weight(1f),
      color = MaterialTheme.colorScheme.surfaceContainer,
      shape =
        RoundedCornerShape(
          topStart = 0.dp,
          topEnd = 8.dp,
          bottomEnd = 8.dp,
          bottomStart = 8.dp,
        ),
      border =
        androidx.compose.foundation.BorderStroke(
          width = 1.dp,
          color = MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        for (block in contentBlocks) {
          when (block) {
            is PostContentBlock.Inline -> {
              PostInlineRichText(items = block.items, onLinkClick = onLinkClick)
            }
            is PostContentBlock.Quote -> {
              PostQuoteBlock(
                parts = block.part.parts,
                imageUrls = imageUrls,
                onLinkClick = onLinkClick,
                onImageClick = onImageClick,
                nested = true,
              )
            }
            is PostContentBlock.Image -> {
              CachedPostImage(
                url = block.part.url,
                onClick = { onImageClick(imageUrls, imageUrls.indexOf(block.part.url).takeIf { it >= 0 } ?: 0) },
              )
            }
            is PostContentBlock.Audio -> {
              PostAudioPlayer(
                url = block.part.url,
                label = block.part.label,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PostQuoteBlock(
  parts: List<PostContentPart>,
  imageUrls: List<String>,
  onLinkClick: (String) -> Unit,
  onImageClick: (List<String>, Int) -> Unit,
  modifier: Modifier = Modifier,
  nested: Boolean = false,
) {
  val blocks = remember(parts) { groupPostContentParts(parts) }
  val originalPostUrl = remember(parts) { quotedOriginalPostUrl(parts) }
  Surface(
    modifier = modifier.fillMaxWidth(),
    color =
      if (nested) {
        MaterialTheme.colorScheme.surfaceContainerHigh
      } else {
        MaterialTheme.colorScheme.surfaceContainer
      },
    shape = MaterialTheme.shapes.small,
    border =
      androidx.compose.foundation.BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outlineVariant,
      ),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (originalPostUrl != null) {
        TextButton(
          onClick = { onLinkClick(originalPostUrl) },
          modifier = Modifier.align(Alignment.End),
        ) {
          Text("[原帖]")
        }
      }
      for (block in blocks) {
        when (block) {
          is PostContentBlock.Inline -> {
            PostInlineRichText(
              items = block.items,
              onLinkClick = onLinkClick,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          is PostContentBlock.Image -> {
            CachedPostImage(
              url = block.part.url,
              onClick = { onImageClick(imageUrls, imageUrls.indexOf(block.part.url).takeIf { it >= 0 } ?: 0) },
            )
          }
          is PostContentBlock.Audio -> {
            PostAudioPlayer(
              url = block.part.url,
              label = block.part.label,
            )
          }
          is PostContentBlock.Quote -> {
            PostQuoteBlock(
              parts = block.part.parts,
              imageUrls = imageUrls,
              onLinkClick = onLinkClick,
              onImageClick = onImageClick,
              nested = true,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AttachmentRow(
  attachment: PostAttachmentPreview,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val fileCategory =
    remember(attachment.name, attachment.url) {
      AttachmentFileType.category(attachment.name, attachment.url)
    }
  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .semantics { contentDescription = "Attachment ${attachment.name}" },
    color = MaterialTheme.colorScheme.surfaceContainer,
    shape = MaterialTheme.shapes.small,
    border =
      androidx.compose.foundation.BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outlineVariant,
      ),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        imageVector = AttachmentFileType.icon(fileCategory),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = attachment.name,
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun AttachmentDownloadDialog(
  attachment: PostAttachmentPreview,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("下载附件") },
    text = { Text("保存 ${attachment.name} 到 Downloads？") },
    confirmButton = {
      TextButton(onClick = onConfirm) {
        Text("下载")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("取消")
      }
    },
  )
}

@Composable
private fun ImagePreviewDialog(
  imageUrls: List<String>,
  currentIndex: Int,
  onIndexChange: (Int) -> Unit,
  onDismiss: () -> Unit,
) {
  val initialPage = currentIndex.coerceIn(0, imageUrls.lastIndex)
  val pagerState =
    rememberPagerState(initialPage = initialPage) {
      imageUrls.size
    }
  var currentPageScale by remember { mutableStateOf(1f) }
  var headerVisible by remember { mutableStateOf(false) }

  LaunchedEffect(currentIndex, imageUrls.size) {
    val targetPage = currentIndex.coerceIn(0, imageUrls.lastIndex)
    if (pagerState.currentPage != targetPage) {
      pagerState.scrollToPage(targetPage)
    }
  }

  LaunchedEffect(pagerState.currentPage) {
    onIndexChange(pagerState.currentPage)
    currentPageScale = 1f
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties =
      DialogProperties(
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false,
      ),
  ) {
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(Color.Black)
          .semantics { contentDescription = "Image preview ${pagerState.currentPage + 1} of ${imageUrls.size}" },
      contentAlignment = Alignment.Center,
    ) {
      HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 1,
        userScrollEnabled = currentPageScale <= 1.01f,
        key = { page -> imagePreviewPageKey(page, imageUrls[page]) },
        modifier = Modifier.fillMaxSize(),
      ) { page ->
        ImagePreviewPage(
          url = imageUrls[page],
          page = page,
          isCurrentPage = page == pagerState.currentPage,
          onCurrentPageScaleChange = { scale ->
            if (page == pagerState.currentPage) {
              currentPageScale = scale
            }
          },
          onToggleHeader = { headerVisible = !headerVisible },
        )
      }
      if (headerVisible) {
        Row(
          modifier =
            Modifier
              .align(Alignment.TopStart)
              .fillMaxWidth()
              .background(Color.Black.copy(alpha = 0.56f))
              .padding(top = 24.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.semantics { contentDescription = "Close image preview" },
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = null,
              tint = Color.White,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ImagePreviewPage(
  url: String,
  page: Int,
  isCurrentPage: Boolean,
  onCurrentPageScaleChange: (Float) -> Unit,
  onToggleHeader: () -> Unit,
) {
  var scale by remember(url) { mutableStateOf(1f) }
  var offset by remember(url) { mutableStateOf(Offset.Zero) }
  var dragEnabled by remember(url) { mutableStateOf(false) }
  var viewportSize by remember(url) { mutableStateOf(IntSize.Zero) }
  var imageAspectRatio by remember(url) { mutableStateOf<Float?>(null) }

  LaunchedEffect(isCurrentPage, scale) {
    if (isCurrentPage) {
      onCurrentPageScaleChange(scale)
    }
  }
  LaunchedEffect(viewportSize, scale, imageAspectRatio) {
    offset = coercePreviewPanOffset(offset, viewportSize, scale, imageAspectRatio)
  }

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .onSizeChanged { viewportSize = it }
        .semantics { contentDescription = "Image preview page ${page + 1}" }
        .pointerInput(url) {
          coroutineScope {
            launch {
              detectTapGestures(
                onTap = { onToggleHeader() },
                onDoubleTap = {
                  val transform =
                    togglePreviewScaleOnDoubleTap(
                      scale = scale,
                      offset = offset,
                      dragEnabled = dragEnabled,
                    )
                  scale = transform.scale
                  offset = transform.offset
                  dragEnabled = transform.dragEnabled
                },
                onLongPress = { dragEnabled = true },
              )
            }
            launch {
              awaitPointerEventScope {
                while (true) {
                  val event = awaitPointerEvent()
                  val pressed = event.changes.filter { it.pressed }
                  val isMultiTouch = pressed.size >= 2
                  val panChange = if (pressed.size == 1) pressed.first().positionChange() else Offset.Zero
                  val canPan = shouldConsumePreviewPanChange(scale, dragEnabled, panChange)
                  if (!isMultiTouch && !canPan) {
                    continue
                  }

                  if (isMultiTouch) {
                    val zoomChange = event.calculateZoom()
                    val zoomPanChange = event.calculatePan()
                    val nextScale = (scale * zoomChange).coerceIn(1f, 5f)
                    scale = nextScale
                    if (nextScale <= 1.01f) {
                      offset = Offset.Zero
                      dragEnabled = false
                    } else {
                      offset =
                        coercePreviewPanOffset(
                          offset = offset + zoomPanChange,
                          viewportSize = viewportSize,
                          scale = nextScale,
                          imageAspectRatio = imageAspectRatio,
                        )
                    }
                    event.changes.forEach { it.consume() }
                  } else if (canPan) {
                    offset =
                      coercePreviewPanOffset(
                        offset = offset + panChange,
                        viewportSize = viewportSize,
                        scale = scale,
                        imageAspectRatio = imageAspectRatio,
                      )
                    pressed.first().consume()
                  }
                }
              }
            }
          }
        },
    contentAlignment = Alignment.Center,
  ) {
    CachedAsyncImage(
      url = url,
      contentDescription = null,
      modifier =
        Modifier
          .fillMaxSize()
          .graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationX = offset.x
            translationY = offset.y
          },
      contentScale = ContentScale.Fit,
      crossfade = false,
      onSuccess = { state ->
        imageAspectRatio = state.result.drawable.intrinsicAspectRatio()
      },
      onError = {
        imageAspectRatio = null
      },
    )
  }
}

internal data class ImagePreviewTransform(
  val scale: Float,
  val offset: Offset,
  val dragEnabled: Boolean,
)

internal fun imagePreviewPageKey(page: Int, url: String): String = "$page:$url"

internal fun togglePreviewScaleOnDoubleTap(
  scale: Float,
  offset: Offset,
  dragEnabled: Boolean,
): ImagePreviewTransform =
  if (scale <= 1.01f) {
    ImagePreviewTransform(
      scale = 2.5f,
      offset = Offset.Zero,
      dragEnabled = true,
    )
  } else {
    ImagePreviewTransform(
      scale = 1f,
      offset = Offset.Zero,
      dragEnabled = false,
    )
  }

internal fun shouldConsumePreviewPanChange(
  scale: Float,
  dragEnabled: Boolean,
  panChange: Offset,
): Boolean =
  scale > 1.01f &&
    dragEnabled &&
    panChange != Offset.Zero

internal fun coercePreviewPanOffset(
  offset: Offset,
  viewportSize: IntSize,
  scale: Float,
  imageAspectRatio: Float?,
): Offset {
  if (scale <= 1.01f || viewportSize.width <= 0 || viewportSize.height <= 0) {
    return Offset.Zero
  }
  val fittedSize = previewFittedImageSize(viewportSize, imageAspectRatio)
  val maxX = ((fittedSize.width * scale) - viewportSize.width).coerceAtLeast(0f) / 2f
  val maxY = ((fittedSize.height * scale) - viewportSize.height).coerceAtLeast(0f) / 2f
  return Offset(
    x = offset.x.coerceIn(-maxX, maxX),
    y = offset.y.coerceIn(-maxY, maxY),
  )
}

private data class PreviewImageSize(
  val width: Float,
  val height: Float,
)

private fun previewFittedImageSize(
  viewportSize: IntSize,
  imageAspectRatio: Float?,
): PreviewImageSize {
  val viewportWidth = viewportSize.width.toFloat()
  val viewportHeight = viewportSize.height.toFloat()
  val ratio = imageAspectRatio?.takeIf { it > 0f }
    ?: return PreviewImageSize(width = viewportWidth, height = viewportHeight)
  val viewportRatio = viewportWidth / viewportHeight
  return if (viewportRatio > ratio) {
    PreviewImageSize(width = viewportHeight * ratio, height = viewportHeight)
  } else {
    PreviewImageSize(width = viewportWidth, height = viewportWidth / ratio)
  }
}

private fun android.graphics.drawable.Drawable.intrinsicAspectRatio(): Float? {
  val width = intrinsicWidth
  val height = intrinsicHeight
  return if (width > 0 && height > 0) {
    width.toFloat() / height.toFloat()
  } else {
    null
  }
}

internal fun postPreviewImageUrls(post: PostPreview): List<String> =
  postContentImageUrls(post.content)

internal fun embeddedReplyPreviewImageUrls(reply: PostEmbeddedReplyPreview): List<String> =
  postContentImageUrls(reply.content)

internal fun quotedOriginalPostUrl(parts: List<PostContentPart>): String? {
  for (part in parts) {
    when (part) {
      is PostContentPart.Text ->
        part.styles
          .asSequence()
          .mapNotNull { it.linkUrl }
          .firstOrNull { it.startsWith("nga://post/", ignoreCase = true) }
          ?.let { return it }
      is PostContentPart.Quote -> quotedOriginalPostUrl(part.parts)?.let { return it }
      else -> Unit
    }
  }
  return null
}

private fun postContentImageUrls(content: String): List<String> =
  PostContentParser.collectImageUrls(PostContentParser.parse(content)).distinct()

@Composable
private fun PostInlineRichText(
  items: List<PostInlineItem>,
  modifier: Modifier = Modifier,
  style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
  color: Color = MaterialTheme.colorScheme.onSurface,
  onLinkClick: (String) -> Unit = {},
) {
  val linkColor = MaterialTheme.colorScheme.primary
  val inlineContent = mutableMapOf<String, InlineTextContent>()
  var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
  val hasLinks = items.any { item -> item is PostInlineItem.Text && item.styles.any { it.linkUrl != null } }
  val semanticText =
    items.joinToString(separator = "") { item ->
      when (item) {
        is PostInlineItem.Text -> item.text
        is PostInlineItem.Emoticon -> item.part.alt
      }
    }
  val emoticonDescription =
    items
      .mapNotNull { item -> (item as? PostInlineItem.Emoticon)?.part?.alt }
      .joinToString(separator = "")
  val text =
    buildAnnotatedString {
      items.forEachIndexed { index, item ->
        when (item) {
          is PostInlineItem.Text -> {
            val textStart = length
            append(item.text)
            item.styles.forEach { range ->
              val start = (textStart + range.start).coerceIn(textStart, textStart + item.text.length)
              val end = (textStart + range.end).coerceIn(textStart, textStart + item.text.length)
              addStyle(
                range.toSpanStyle(linkColor, MaterialTheme.typography.bodyLarge.fontSize),
                start,
                end,
              )
              range.linkUrl?.let { url ->
                addStringAnnotation(LinkAnnotationTag, url, start, end)
              }
            }
          }
          is PostInlineItem.Emoticon -> {
            val inlineId = "post-emoticon-$index"
            appendInlineContent(inlineId, item.part.alt)
            inlineContent[inlineId] =
              InlineTextContent(
                placeholder =
                  Placeholder(
                    width = 1.5.em,
                    height = 1.5.em,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                  ),
              ) {
                CachedAsyncImage(
                  url = item.part.url,
                  contentDescription = item.part.alt,
                  modifier = Modifier.size(28.dp),
                  contentScale = ContentScale.Fit,
                  sizeDp = 28.dp,
                  colorFilter =
                    if (
                      shouldInvertEmoticonForBackground(
                        MaterialTheme.colorScheme.background,
                        item.part.code,
                      )
                    ) {
                      invertedEmoticonColorFilter()
                    } else {
                      null
                    },
                )
              }
          }
        }
      }
    }

  val linkModifier =
    if (hasLinks) {
      Modifier.pointerInput(text, onLinkClick) {
        detectTapGestures { position ->
          val offset = textLayoutResult?.getOffsetForPosition(position) ?: return@detectTapGestures
          text
            .getStringAnnotations(LinkAnnotationTag, offset, offset)
            .firstOrNull()
            ?.let { onLinkClick(it.item) }
        }
      }
    } else {
      Modifier
    }

  Text(
    text = text,
    inlineContent = inlineContent,
    modifier =
      modifier
        .then(linkModifier)
        .clearAndSetSemantics {
        this.text = AnnotatedString(semanticText)
        if (emoticonDescription.isNotEmpty()) {
          contentDescription = emoticonDescription
        }
      },
    style = style,
    color = color,
    lineHeight = style.lineHeight * 1.35f,
    onTextLayout = { textLayoutResult = it },
  )
}

@Composable
private fun PostRichText(
  text: String,
  styles: List<PostTextStyleRange>,
  modifier: Modifier = Modifier,
  style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
  color: Color = MaterialTheme.colorScheme.onSurface,
  baseItalic: Boolean = false,
  onLinkClick: (String) -> Unit = {},
) {
  val linkColor = MaterialTheme.colorScheme.primary
  var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
  val baseFontSize = style.fontSize
  val annotatedText =
    remember(text, styles, linkColor, baseItalic, baseFontSize) {
      buildAnnotatedString {
        if (baseItalic) {
          withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
            append(text)
          }
        } else {
          append(text)
        }
        styles.forEach { range ->
          val start = range.start.coerceIn(0, text.length)
          val end = range.end.coerceIn(0, text.length)
          if (start < end) {
            addStyle(
              range.toSpanStyle(linkColor, baseFontSize),
              start,
              end,
            )
            range.linkUrl?.let { url ->
              addStringAnnotation(LinkAnnotationTag, url, start, end)
            }
          }
        }
      }
    }
  val linkModifier =
    if (styles.any { it.linkUrl != null }) {
      Modifier.pointerInput(annotatedText, onLinkClick) {
        detectTapGestures { position ->
          val offset = textLayoutResult?.getOffsetForPosition(position) ?: return@detectTapGestures
          annotatedText
            .getStringAnnotations(LinkAnnotationTag, offset, offset)
            .firstOrNull()
            ?.let { onLinkClick(it.item) }
        }
      }
    } else {
      Modifier
    }

  Text(
    text = annotatedText,
    modifier = modifier.then(linkModifier),
    style = style,
    color = color,
    lineHeight = style.lineHeight * 1.3f,
    onTextLayout = { textLayoutResult = it },
  )
}

private const val LinkAnnotationTag = "url"

private fun PostTextStyleRange.toSpanStyle(linkColor: Color, baseFontSize: TextUnit): SpanStyle {
  val decorations =
    buildList {
      if (underline || linkUrl != null) add(TextDecoration.Underline)
      if (strikeThrough) add(TextDecoration.LineThrough)
    }
  return SpanStyle(
    brush = null,
    fontSize = sizePercent?.let { baseFontSize * (it / 100f) } ?: TextUnit.Unspecified,
    fontWeight = if (bold) FontWeight.Bold else null,
    fontStyle = if (italic) androidx.compose.ui.text.font.FontStyle.Italic else null,
    textDecoration = if (decorations.isNotEmpty()) TextDecoration.combine(decorations) else null,
  ).copy(color = color?.toComposeColor() ?: if (linkUrl != null) linkColor else Color.Unspecified)
}

private fun String.toComposeColor(): Color =
  when (lowercase()) {
    "red", "crimson", "firebrick", "darkred" -> Color(0xFFB3261E)
    "blue", "royalblue", "darkblue", "skyblue" -> Color(0xFF315EAD)
    "green", "limegreen", "seagreen", "teal" -> Color(0xFF2E7D32)
    "orange", "orangered", "coral", "tomato" -> Color(0xFFB75E00)
    "purple", "indigo" -> Color(0xFF6D3CC7)
    "silver", "gray", "grey" -> Color(0xFF73777F)
    "deeppink" -> Color(0xFFC2185B)
    "burlywood", "sandybrown", "sienna", "chocolate" -> Color(0xFF8B5A2B)
    else -> Color.Unspecified
  }

@Composable
private fun PrefetchPostImages(posts: List<PostPreview>) {
  PrefetchUserAvatars(posts.map { it.authorAvatarUrl })

  val context = LocalContext.current
  val imageUrls =
    remember(posts) {
      posts
        .flatMap { post ->
          val parts = PostContentParser.parse(post.content)
          PostContentParser.collectImageUrls(parts) +
            parts.mapNotNull { (it as? PostContentPart.Emoticon)?.url }
        }
        .distinct()
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
  onOpenInBrowser: () -> Unit,
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
      text = { Text("用浏览器打开") },
      onClick = onOpenInBrowser,
      leadingIcon = {
        Icon(
          imageVector = Icons.Outlined.OpenInBrowser,
          contentDescription = null,
        )
      },
    )
  }
}

private enum class ThreadJumpMode {
  Page,
  Floor,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThreadQuickJumpSheet(
  currentPage: Int,
  maxPage: Int,
  maxFloor: Int,
  onDismiss: () -> Unit,
  onPageJump: (Int) -> Unit,
  onFloorJump: (Int) -> Unit,
) {
  var mode by remember { mutableStateOf(ThreadJumpMode.Page) }
  var input by remember { mutableStateOf("") }
  val target = input.toIntOrNull()
  val isPageMode = mode == ThreadJumpMode.Page
  val isValid =
    when (mode) {
      ThreadJumpMode.Page -> target != null && target in 1..maxPage
      ThreadJumpMode.Floor -> target != null && target in 0..maxFloor
    }

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .imePadding()
          .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("快速跳转", style = MaterialTheme.typography.titleLarge)
        Text(
          text = "当前第 $currentPage / $maxPage 页",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
      ) {
        Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          ThreadJumpModeButton(
            text = "页码",
            selected = mode == ThreadJumpMode.Page,
            modifier = Modifier.weight(1f),
            onClick = {
              mode = ThreadJumpMode.Page
              input = ""
            },
          )
          ThreadJumpModeButton(
            text = "楼层",
            selected = mode == ThreadJumpMode.Floor,
            modifier = Modifier.weight(1f),
            onClick = {
              mode = ThreadJumpMode.Floor
              input = ""
            },
          )
        }
      }

      OutlinedTextField(
        value = input,
        onValueChange = { value -> input = value.filter(Char::isDigit) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(if (isPageMode) "页码" else "楼层") },
        supportingText = {
          Text(
            if (isPageMode) {
              "输入 1 到 $maxPage 之间的页码"
            } else {
              "当前帖子共 $maxFloor 楼，输入 0 到 $maxFloor 之间的楼层号"
            },
          )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        FilledTonalButton(
          onClick = onDismiss,
          modifier = Modifier.weight(1f),
        ) {
          Text("取消")
        }
        Button(
          onClick = {
            val value = target ?: return@Button
            if (isPageMode) {
              onPageJump(value)
            } else {
              onFloorJump(value)
            }
          },
          enabled = isValid,
          modifier = Modifier.weight(1f),
        ) {
          Text("跳转")
        }
      }

      Spacer(modifier = Modifier.height(2.dp))
    }
  }
}

@Composable
private fun ThreadJumpModeButton(
  text: String,
  selected: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  Surface(
    modifier = modifier.clickable(onClick = onClick),
    shape = RoundedCornerShape(24.dp),
    color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
    contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    tonalElevation = if (selected) 2.dp else 0.dp,
  ) {
    Box(
      modifier = Modifier.padding(vertical = 10.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
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








