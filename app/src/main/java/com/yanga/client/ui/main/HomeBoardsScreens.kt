package com.yanga.client.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun HomeScreen(
  loginSession: LoginSessionUiState?,
  state: HomeUiState = HomeUiState(),
  onLoginClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    PageHeader(
      title = "Yanga",
      subtitle = if (loginSession == null) {
        "Browse NGA boards before signing in"
      } else {
        "Signed in as ${loginSession.username} - UID ${loginSession.uid}"
      },
    )

    SearchPill(text = "Search boards, topics, and users")
    FilterChipRow(labels = listOf("Favorite", "Hot topics", "Favorites", "History"))

    if (loginSession == null) {
      LoginPrompt(onLoginClick = onLoginClick)
    }

    SectionHeader(title = "Favorite boards", trailing = "Manage")
    BoardGridState(state = state.boards)

    SectionHeader(title = "Active discussions", trailing = "Latest")
    TopicListState(state = state.activeTopics)
  }
}

@Composable
internal fun BoardsScreen(
  state: BoardsUiState = BoardsUiState(),
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    PageHeader(
      title = "Boards",
      subtitle = "Discover, subscribe, hide, and reorder forum boards",
    )

    SearchPill(text = "Board search")
    FilterChipRow(labels = listOf("All", "Subscribed", "Game", "Life", "More"))

    SectionHeader(title = "Subscribed boards", trailing = "Reorder")
    BoardListState(state = state.subscribedBoards, loginRequiredText = "Sign in to load subscribed boards")

    SectionHeader(title = "Full forum directory")
    BoardListState(state = state.categories, loginRequiredText = "Sign in to load forum directory")

    ManageBoardsCard()
  }
}

@Composable
private fun LoginPrompt(onLoginClick: () -> Unit, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    shape = MaterialTheme.shapes.large,
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = "Sign in to sync Favorites and History",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = "Public browsing stays available. Sign in when you want to post, manage favorites, or restore subscribed boards.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
      Button(onClick = onLoginClick) {
        Text(text = "Sign in")
      }
    }
  }
}

@Composable
private fun BoardGridState(state: LoadableUiState<List<BoardPreview>>, modifier: Modifier = Modifier) {
  when (state) {
    is LoadableUiState.Content -> {
      if (state.value.isEmpty()) {
        StateMessage(text = "No favorite boards")
      } else {
        FavoriteBoardGrid(boards = state.value, modifier = modifier)
      }
    }
    is LoadableUiState.Empty -> StateMessage(text = state.message)
    is LoadableUiState.Error -> StateMessage(text = state.message)
    LoadableUiState.Loading -> StateMessage(text = "Loading favorite boards")
    LoadableUiState.LoginRequired -> StateMessage(text = "Sign in to load favorite boards")
  }
}

@Composable
private fun FavoriteBoardGrid(boards: List<BoardPreview>, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    boards.chunked(2).forEach { rowBoards ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        rowBoards.forEach { board ->
          FavoriteBoardCard(board = board, modifier = Modifier.weight(1f))
        }
        if (rowBoards.size == 1) {
          Column(modifier = Modifier.weight(1f)) {}
        }
      }
    }
  }
}

@Composable
private fun TopicListState(state: LoadableUiState<List<TopicPreview>>, modifier: Modifier = Modifier) {
  TonalCard(modifier = modifier) {
    when (state) {
      is LoadableUiState.Content -> {
        if (state.value.isEmpty()) {
          Text(text = "No active discussions", style = MaterialTheme.typography.bodyMedium)
        } else {
          state.value.forEach { topic ->
            TopicRow(topic = topic)
          }
        }
      }
      is LoadableUiState.Empty -> Text(text = state.message, style = MaterialTheme.typography.bodyMedium)
      is LoadableUiState.Error ->
        Text(text = state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
      LoadableUiState.Loading -> Text(text = "Loading active discussions", style = MaterialTheme.typography.bodyMedium)
      LoadableUiState.LoginRequired -> Text(text = "Sign in to load active discussions", style = MaterialTheme.typography.bodyMedium)
    }
  }
}

@Composable
private fun BoardListState(
  state: LoadableUiState<List<BoardPreview>>,
  loginRequiredText: String,
  modifier: Modifier = Modifier,
) {
  TonalCard(modifier = modifier) {
    when (state) {
      is LoadableUiState.Content -> {
        if (state.value.isEmpty()) {
          Text(text = "No boards available", style = MaterialTheme.typography.bodyMedium)
        } else {
          state.value.forEach { board ->
            BoardListRow(board = board)
          }
        }
      }
      is LoadableUiState.Empty -> Text(text = state.message, style = MaterialTheme.typography.bodyMedium)
      is LoadableUiState.Error ->
        Text(text = state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
      LoadableUiState.Loading -> Text(text = "Loading boards", style = MaterialTheme.typography.bodyMedium)
      LoadableUiState.LoginRequired -> Text(text = loginRequiredText, style = MaterialTheme.typography.bodyMedium)
    }
  }
}

@Composable
private fun StateMessage(text: String, modifier: Modifier = Modifier) {
  TonalCard(modifier = modifier) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium)
  }
}

@Composable
private fun FavoriteBoardCard(board: BoardPreview, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    shape = MaterialTheme.shapes.large,
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Marker(text = board.marker)
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = board.name,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = board.metadata,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      board.badge?.let {
        Text(
          text = "$it new",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
private fun ManageBoardsCard(modifier: Modifier = Modifier) {
  TonalCard(modifier = modifier) {
    Text(
      text = "Manage boards",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
    )
    Text(
      text = "Subscribe to favorite forums, hide noisy boards, and adjust the order shown on Home.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    ActionChipRow(labels = listOf("Subscribe", "Hide boards", "Change order"))
    OutlinedButton(onClick = {}) {
      Text(text = "Open management")
    }
  }
}
