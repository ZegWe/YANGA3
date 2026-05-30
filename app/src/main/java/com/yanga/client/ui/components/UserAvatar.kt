package com.yanga.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yanga.client.ui.initialOrFallback

@Composable
fun UserAvatar(
  name: String,
  avatarUrl: String?,
  modifier: Modifier = Modifier,
  size: Dp = 40.dp,
) {
  Box(
    modifier =
      modifier
        .size(size)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.tertiaryContainer),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = name.initialOrFallback(),
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.onTertiaryContainer,
      fontWeight = FontWeight.SemiBold,
    )
    if (!avatarUrl.isNullOrBlank()) {
      CachedAsyncImage(
        url = avatarUrl,
        contentDescription = name,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        sizeDp = size,
        crossfade = true,
      )
    }
  }
}
