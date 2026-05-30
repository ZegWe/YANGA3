package com.yanga.client.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.yanga.client.R
import com.yanga.client.data.image.ImageCacheKind
import com.yanga.client.data.image.ImageUrlResolver
import com.yanga.client.data.image.imageCacheManager

/**
 * Board icon that reuses a decoded bitmap cache (memory + raw pixel disk) when available.
 */
@Composable
fun BoardIconImage(
  url: String,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  sizeDp: Dp,
  contentScale: ContentScale = ContentScale.Fit,
) {
  val context = LocalContext.current
  val cacheManager = context.imageCacheManager()
  val resolvedUrl =
    remember(url) {
      url.takeIf { it.isNotBlank() }?.let(ImageUrlResolver::resolve).orEmpty()
    }
  val sizePx = with(LocalDensity.current) { sizeDp.roundToPx().coerceAtLeast(1) }
  var bitmap by remember(resolvedUrl, sizePx) {
    mutableStateOf(
      resolvedUrl.takeIf { it.isNotBlank() }?.let { cacheManager.getDecodedBoardIcon(it, sizePx) },
    )
  }
  var loadFinished by remember(resolvedUrl, sizePx) { mutableStateOf(bitmap != null) }
  val placeholder = painterResource(id = R.drawable.default_board_icon)

  LaunchedEffect(resolvedUrl, sizePx) {
    if (resolvedUrl.isBlank()) {
      loadFinished = true
      return@LaunchedEffect
    }
    if (bitmap == null) {
      bitmap = cacheManager.fetchDecodedBoardIcon(context, resolvedUrl, sizePx)
    }
    loadFinished = true
  }

  when {
    bitmap != null ->
      Image(
        bitmap = bitmap!!.asImageBitmap(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
      )
    !loadFinished ->
      Image(
        painter = placeholder,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
      )
    resolvedUrl.isBlank() ->
      Image(
        painter = placeholder,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
      )
    else ->
      CachedAsyncImage(
        url = url,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        sizeDp = sizeDp,
        crossfade = false,
        placeholder = placeholder,
        error = placeholder,
        fallback = placeholder,
        cacheKind = ImageCacheKind.BoardIcon,
      )
  }
}
