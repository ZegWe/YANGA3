package com.yanga.client.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yanga.client.ui.components.TopicListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchScreen(
  state: SearchUiState,
  onBack: () -> Unit,
  onQueryChange: (String) -> Unit,
  onSubmitSearch: () -> Unit,
  onScopeChange: (SearchScope) -> Unit,
  onSearchContentChange: (Boolean) -> Unit,
  onEssenceOnlyChange: (Boolean) -> Unit,
  onBoardClick: (BoardPreview) -> Unit,
  onTopicClick: (TopicPreview) -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Text(
            text =
              when (val mode = state.mode) {
                SearchMode.Global -> "搜索"
                is SearchMode.BoardScoped -> "搜索 ${mode.board.name}"
              },
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
      )
    },
  ) { paddingValues ->
    Column(
      modifier =
        Modifier
          .padding(paddingValues)
          .fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      SearchControls(
        state = state,
        onQueryChange = onQueryChange,
        onSubmitSearch = onSubmitSearch,
        onScopeChange = onScopeChange,
        onSearchContentChange = onSearchContentChange,
        onEssenceOnlyChange = onEssenceOnlyChange,
        modifier = Modifier.padding(horizontal = 16.dp),
      )
      SearchResults(
        results = state.results,
        onBoardClick = onBoardClick,
        onTopicClick = onTopicClick,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}

@Composable
private fun SearchControls(
  state: SearchUiState,
  onQueryChange: (String) -> Unit,
  onSubmitSearch: () -> Unit,
  onScopeChange: (SearchScope) -> Unit,
  onSearchContentChange: (Boolean) -> Unit,
  onEssenceOnlyChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  var queryValue by remember { mutableStateOf(TextFieldValue(state.query)) }
  LaunchedEffect(state.query) {
    if (state.query != queryValue.text) {
      queryValue = TextFieldValue(state.query)
    }
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    OutlinedTextField(
      value = queryValue,
      onValueChange = { value ->
        queryValue = value
        onQueryChange(value.text)
      },
      modifier = Modifier.fillMaxWidth(),
      label = { Text("搜索") },
      trailingIcon = {
        if (queryValue.text.isNotEmpty()) {
          IconButton(
            onClick = {
              queryValue = TextFieldValue("")
              onQueryChange("")
            },
          ) {
            Icon(Icons.Outlined.Close, contentDescription = "清空搜索")
          }
        }
      },
      singleLine = true,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
      keyboardActions = KeyboardActions(onSearch = { onSubmitSearch() }),
    )

    if (state.mode is SearchMode.Global) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
          selected = state.scope == SearchScope.Boards,
          onClick = { onScopeChange(SearchScope.Boards) },
          label = { Text("板块") },
        )
        FilterChip(
          selected = state.scope == SearchScope.Topics,
          onClick = { onScopeChange(SearchScope.Topics) },
          label = { Text("帖子") },
        )
      }
    }

    if (state.isTopicSearch) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
          selected = state.searchContent,
          onClick = { onSearchContentChange(!state.searchContent) },
          label = { Text("搜正文") },
        )
        FilterChip(
          selected = state.essenceOnly,
          onClick = { onEssenceOnlyChange(!state.essenceOnly) },
          label = { Text("精华") },
        )
      }
    }
  }
}

@Composable
private fun SearchResults(
  results: SearchResultsUiState,
  onBoardClick: (BoardPreview) -> Unit,
  onTopicClick: (TopicPreview) -> Unit,
  modifier: Modifier = Modifier,
) {
  when (results) {
    SearchResultsUiState.Idle ->
      SearchStateMessage(
        text = "Enter keywords to search",
        modifier = modifier,
      )
    SearchResultsUiState.Loading ->
      Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    is SearchResultsUiState.Empty ->
      SearchStateMessage(text = results.message, modifier = modifier)
    is SearchResultsUiState.Error ->
      SearchStateMessage(
        text = results.message,
        isError = true,
        modifier = modifier,
      )
    is SearchResultsUiState.Boards ->
      LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      ) {
        items(results.boards, key = { it.id }) { board ->
          BoardListRow(board = board, onClick = { onBoardClick(board) })
          HorizontalDivider()
        }
      }
    is SearchResultsUiState.Topics ->
      LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(results.topics, key = { it.id }) { topic ->
          ElevatedCard(onClick = { onTopicClick(topic) }) {
            TopicListItem(
              topic = topic,
              onClick = { onTopicClick(topic) },
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
              isClickable = false,
            )
          }
        }
      }
  }
}

@Composable
private fun SearchStateMessage(
  text: String,
  modifier: Modifier = Modifier,
  isError: Boolean = false,
) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text,
      modifier = Modifier.padding(24.dp),
      style = MaterialTheme.typography.bodyMedium,
      color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
