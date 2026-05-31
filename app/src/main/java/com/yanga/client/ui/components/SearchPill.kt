package com.yanga.client.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun SearchPill(
  text: String,
  modifier: Modifier = Modifier,
  fillMaxWidth: Boolean = true,
) {
  Surface(
    modifier = if (fillMaxWidth) modifier.fillMaxWidth() else modifier,
    color = MaterialTheme.colorScheme.surfaceContainer,
    shape = MaterialTheme.shapes.extraLarge,
  ) {
    Text(
      text = text,
      modifier = Modifier.padding(
        horizontal = if (fillMaxWidth) 18.dp else 12.dp,
        vertical = if (fillMaxWidth) 15.dp else 8.dp,
      ),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}






