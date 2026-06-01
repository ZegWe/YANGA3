package com.yanga.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
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
  colorFilter: ColorFilter? = null,
  onLoading: ((AsyncImagePainter.State.Loading) -> Unit)? = null,
  onSuccess: ((AsyncImagePainter.State.Success) -> Unit)? = null,
  onError: ((AsyncImagePainter.State.Error) -> Unit)? = null,
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
        colorFilter = colorFilter,
        onLoading = onLoading,
        onSuccess = onSuccess,
        onError = onError,
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
        colorFilter = colorFilter,
        onLoading = onLoading,
        onSuccess = onSuccess,
        onError = onError,
      )
    }
  }
}

@Composable
fun CachedPostImage(
  url: String,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
) {
  var imageState by remember(url) { mutableStateOf(PostImageState.Loading) }
  var aspectRatio by remember(url) { mutableStateOf<Float?>(null) }

  BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
    val imageHeight = postImageHeight(containerWidth = maxWidth, aspectRatio = aspectRatio)
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(imageHeight)
          .clip(MaterialTheme.shapes.medium)
          .background(MaterialTheme.colorScheme.surfaceContainerLow)
          .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
          .semantics { contentDescription = "Post image" },
      contentAlignment = Alignment.Center,
    ) {
      when (imageState) {
        PostImageState.Loading ->
          CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        PostImageState.Error ->
          Text(
            text = "Image failed to load",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        PostImageState.Success -> Unit
      }
      CachedAsyncImage(
        url = url,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit,
        onLoading = { imageState = PostImageState.Loading },
        onSuccess = { state ->
          val drawable = state.result.drawable
          aspectRatio = drawable.intrinsicAspectRatio()
          imageState = PostImageState.Success
        },
        onError = {
          aspectRatio = null
          imageState = PostImageState.Error
        },
      )
    }
  }
}

internal fun postImageHeight(
  containerWidth: Dp,
  aspectRatio: Float?,
  minHeight: Dp = 120.dp,
  maxHeight: Dp = 520.dp,
): Dp {
  val ratio = aspectRatio?.takeIf { it > 0f } ?: return minHeight
  return (containerWidth / ratio)
    .coerceAtLeast(minHeight)
    .coerceAtMost(maxHeight)
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

private enum class PostImageState {
  Loading,
  Success,
  Error,
}
