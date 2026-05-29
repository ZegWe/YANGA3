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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.yanga.client.R

@Composable
internal fun Marker(text: String, iconUrl: String? = null, modifier: Modifier = Modifier) {
  Box(modifier = modifier.size(42.dp), contentAlignment = Alignment.Center) {
    if (!iconUrl.isNullOrBlank()) {
      AsyncImage(
        model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
          .data(iconUrl)
          .memoryCachePolicy(CachePolicy.ENABLED)
          .diskCachePolicy(CachePolicy.ENABLED)
          .networkCachePolicy(CachePolicy.ENABLED)
          .build(),
        contentDescription = null,
        error = painterResource(id = R.drawable.default_board_icon),
        fallback = painterResource(id = R.drawable.default_board_icon),
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      Image(
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






