package com.yanga.client.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.yanga.client.data.image.ImageCacheKind
import com.yanga.client.data.image.ImageUrlResolver
import com.yanga.client.data.image.imageCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CachedAsyncImage(
  url: String,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  contentScale: ContentScale = ContentScale.Fit,
  sizeDp: Dp? = null,
  placeholder: Painter? = null,
  error: Painter? = null,
  fallback: Painter? = null,
  crossfade: Boolean = true,
  cacheKind: ImageCacheKind = ImageCacheKind.Content,
) {
  val context = LocalContext.current
  val cacheManager = context.imageCacheManager()
  val sizePx =
    sizeDp?.let { size ->
      with(LocalDensity.current) { size.roundToPx() }
    }

  when (cacheKind) {
    ImageCacheKind.BoardIcon ->
      AsyncImage(
        model =
          cacheManager.buildBoardIconRequest(
            context = context,
            rawUrl = url,
            crossfade = crossfade,
            sizePx = sizePx,
          ),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        placeholder = placeholder,
        error = error,
        fallback = fallback,
      )
    ImageCacheKind.Content -> {
      val resolvedUrl = remember(url) { ImageUrlResolver.resolve(url) }
      var modelData by remember(resolvedUrl) {
        mutableStateOf<Any>(cacheManager.getRawContentFile(url) ?: resolvedUrl)
      }

      LaunchedEffect(resolvedUrl) {
        val cached = withContext(Dispatchers.IO) { cacheManager.ensureRawContentCached(url) }
        if (cached != null) {
          modelData = cached
        }
      }

      AsyncImage(
        model =
          cacheManager.buildContentImageRequest(
            context = context,
            rawUrl = url,
            crossfade = crossfade,
            sizePx = sizePx,
            data = modelData,
          ),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        placeholder = placeholder,
        error = error,
        fallback = fallback,
      )
    }
  }
}

@Composable
fun CachedPostImage(
  url: String,
  modifier: Modifier = Modifier,
) {
  CachedAsyncImage(
    url = url,
    contentDescription = null,
    modifier = modifier.fillMaxWidth(),
    contentScale = ContentScale.FillWidth,
  )
}
