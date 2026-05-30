package com.yanga.client.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yanga.client.R
import com.yanga.client.data.boards.BoardIconResolver
import com.yanga.client.ui.components.BoardIconImage

@Composable
internal fun Marker(
  text: String,
  iconUrl: String? = null,
  boardId: String? = null,
  modifier: Modifier = Modifier,
  iconSize: Dp = 42.dp,
) {
  val placeholder = painterResource(id = R.drawable.default_board_icon)
  val resolvedUrl =
    iconUrl?.takeIf { it.isNotBlank() }
      ?: boardId?.let(BoardIconResolver::networkIconUrl)

  Box(modifier = modifier.size(iconSize), contentAlignment = Alignment.Center) {
    if (!resolvedUrl.isNullOrBlank()) {
      BoardIconImage(
        url = resolvedUrl,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        sizeDp = iconSize,
        contentScale = ContentScale.Fit,
      )
    } else {
      Image(
        painter = placeholder,
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
    modifier = modifier.size(42.dp).clip(MaterialTheme.shapes.extraLarge),
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.tertiaryContainer,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = text.take(1),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}
