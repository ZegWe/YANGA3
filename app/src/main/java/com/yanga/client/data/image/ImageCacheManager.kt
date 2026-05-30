package com.yanga.client.data.image

import android.content.Context
import android.graphics.Bitmap
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okio.Path.Companion.toOkioPath

enum class ImageCacheKind {
  BoardIcon,
  Content,
}

@OptIn(ExperimentalCoilApi::class)
class ImageCacheManager(
  context: Context,
) {
  val imageLoader: ImageLoader =
    ImageLoader.Builder(context)
      .diskCache {
        DiskCache.Builder()
          .directory(context.cacheDir.resolve(DISK_CACHE_DIR_NAME).toOkioPath())
          .maxSizeBytes(MAX_DISK_CACHE_BYTES)
          .build()
      }
      .eventListener(
        object : coil.EventListener {
          override fun onSuccess(request: ImageRequest, result: SuccessResult) {
            recordCachedFile(request, result.diskCacheKey)
          }
        },
      )
      .build()

  private val index = ImageCacheIndex(context.filesDir.resolve(INDEX_FILE_NAME))
  private val decodedBoardIcons =
    DecodedBoardIconCache(context.filesDir.resolve(DECODED_BOARD_ICON_DIR_NAME))
  private val contentImageRawCache =
    ContentImageRawCache(
      cacheDir = context.filesDir.resolve(CONTENT_IMAGE_RAW_DIR_NAME),
      indexFile = context.filesDir.resolve(CONTENT_IMAGE_INDEX_FILE_NAME),
      userAgent = USER_AGENT,
    )

  fun getDecodedBoardIcon(rawUrl: String, sizePx: Int): Bitmap? {
    val url = ImageUrlResolver.resolve(rawUrl)
    if (url.isBlank()) return null
    return decodedBoardIcons.get(url, sizePx)
  }

  fun hasDecodedBoardIcon(rawUrl: String, sizePx: Int): Boolean {
    val url = ImageUrlResolver.resolve(rawUrl)
    if (url.isBlank()) return false
    return decodedBoardIcons.has(url, sizePx)
  }

  fun getRawContentFile(rawUrl: String): File? = contentImageRawCache.get(rawUrl)

  fun hasRawContentCache(rawUrl: String): Boolean = contentImageRawCache.has(rawUrl)

  fun isInMemory(rawUrl: String): Boolean {
    val url = ImageUrlResolver.resolve(rawUrl)
    if (url.isBlank()) return false
    return imageLoader.memoryCache?.get(MemoryCache.Key(url)) != null ||
      decodedBoardIcons.has(url, BOARD_ICON_PREFETCH_PX)
  }

  fun hasDiskCache(rawUrl: String): Boolean = getCachedFile(rawUrl) != null

  fun isCached(rawUrl: String): Boolean =
    isInMemory(rawUrl) ||
      hasDiskCache(rawUrl) ||
      hasRawContentCache(rawUrl) ||
      hasDecodedBoardIcon(rawUrl, BOARD_ICON_PREFETCH_PX)

  fun buildRequest(
    context: Context,
    rawUrl: String,
    crossfade: Boolean = true,
    sizePx: Int? = null,
    cacheKind: ImageCacheKind = ImageCacheKind.Content,
  ): ImageRequest =
    when (cacheKind) {
      ImageCacheKind.BoardIcon -> buildBoardIconRequest(context, rawUrl, crossfade, sizePx)
      ImageCacheKind.Content -> buildContentImageRequest(context, rawUrl, crossfade, sizePx)
    }

  fun buildBoardIconRequest(
    context: Context,
    rawUrl: String,
    crossfade: Boolean = true,
    sizePx: Int? = null,
  ): ImageRequest {
    val url = ImageUrlResolver.resolve(rawUrl)
    return ImageRequest.Builder(context)
      .data(url)
      .apply { if (sizePx != null) size(sizePx) }
      .diskCacheKey(url)
      .memoryCacheKey(url)
      .memoryCachePolicy(CachePolicy.ENABLED)
      .diskCachePolicy(CachePolicy.ENABLED)
      .networkCachePolicy(CachePolicy.ENABLED)
      .crossfade(crossfade)
      .listener(
        onSuccess = { _, result ->
          getCachedFile(url)?.let { file -> index.put(url, file.absolutePath) }
          val size = sizePx ?: BOARD_ICON_PREFETCH_PX
          decodedBoardIcons.storeFromDrawable(url, size, result.drawable)
        },
        onError = { _, _: ErrorResult -> },
      )
      .build()
  }

  fun buildContentImageRequest(
    context: Context,
    rawUrl: String,
    crossfade: Boolean = true,
    sizePx: Int? = null,
    data: Any? = null,
  ): ImageRequest {
    val url = ImageUrlResolver.resolve(rawUrl)
    val model = data ?: getRawContentFile(url) ?: url
    return ImageRequest.Builder(context)
      .data(model)
      .apply { if (sizePx != null) size(sizePx) }
      .memoryCacheKey(url)
      .memoryCachePolicy(CachePolicy.ENABLED)
      .diskCachePolicy(CachePolicy.DISABLED)
      .networkCachePolicy(
        if (model is File) {
          CachePolicy.DISABLED
        } else {
          CachePolicy.ENABLED
        },
      )
      .crossfade(crossfade)
      .build()
  }

  suspend fun ensureRawContentCached(rawUrl: String): File? =
    withContext(Dispatchers.IO) {
      contentImageRawCache.get(rawUrl) ?: contentImageRawCache.download(rawUrl)
    }

  private fun buildDiskWarmRequest(context: Context, url: String): ImageRequest =
    ImageRequest.Builder(context)
      .data(url)
      .size(BOARD_ICON_PREFETCH_PX)
      .diskCacheKey(url)
      .memoryCacheKey(url)
      .diskCachePolicy(CachePolicy.READ_ONLY)
      .networkCachePolicy(CachePolicy.DISABLED)
      .memoryCachePolicy(CachePolicy.ENABLED)
      .crossfade(false)
      .build()

  fun prefetchBoardIcon(context: Context, rawUrl: String) {
    val url = ImageUrlResolver.resolve(rawUrl)
    if (url.isBlank()) return
    getCachedFile(url)?.let { file -> index.put(url, file.absolutePath) }
    imageLoader.enqueue(
      buildBoardIconRequest(context, url, crossfade = false, sizePx = BOARD_ICON_PREFETCH_PX),
    )
  }

  fun prefetch(context: Context, rawUrl: String) {
    prefetchBoardIcon(context, rawUrl)
  }

  suspend fun prefetchContentImages(
    context: Context,
    rawUrls: Collection<String>,
    maxConcurrent: Int = PREFETCH_MAX_CONCURRENT,
  ) {
    val urls = rawUrls.map(ImageUrlResolver::resolve).filter { it.isNotBlank() }.distinct()
    if (urls.isEmpty()) return
    coroutineScope {
      val semaphore = Semaphore(maxConcurrent.coerceIn(1, urls.size))
      urls
        .map { url ->
          async {
            semaphore.withPermit {
              withContext(Dispatchers.IO) {
                contentImageRawCache.download(url)
              }
            }
          }
        }
        .awaitAll()
    }
  }

  suspend fun prefetchAll(
    context: Context,
    rawUrls: Collection<String>,
    maxConcurrent: Int = PREFETCH_MAX_CONCURRENT,
  ) {
    prefetchContentImages(context, rawUrls, maxConcurrent)
  }

  /**
   * Loads icons that already exist on disk into the in-memory cache. Does not
   * request network; uncached URLs are left for display-time loading.
   */
  suspend fun warmDiskCachedIcons(
    context: Context,
    rawUrls: Collection<String>,
    maxConcurrent: Int = PREFETCH_MAX_CONCURRENT,
  ) {
    val urls =
      rawUrls
        .map(ImageUrlResolver::resolve)
        .filter { it.isNotBlank() }
        .distinct()
        .filter { url -> !isInMemory(url) && hasDiskCache(url) }
    if (urls.isEmpty()) return

    val concurrency = maxConcurrent.coerceIn(1, urls.size)
    coroutineScope {
      val semaphore = Semaphore(concurrency)
      urls
        .map { url ->
          async {
            semaphore.withPermit {
              runCatching {
                val result = imageLoader.execute(buildDiskWarmRequest(context, url))
                if (result is SuccessResult) {
                  decodedBoardIcons.storeFromDrawable(
                    url,
                    BOARD_ICON_PREFETCH_PX,
                    result.drawable,
                  )
                }
              }
            }
          }
        }
        .awaitAll()
    }
  }

  fun getCachedFile(rawUrl: String): File? {
    val url = ImageUrlResolver.resolve(rawUrl)
    if (url.isBlank()) return null

    index.getByUrl(url)?.let { path ->
      val file = File(path)
      if (file.exists()) return file
    }

    val diskCache = imageLoader.diskCache ?: return null
    return diskCache.openSnapshot(url)?.use { snapshot ->
      snapshot.data.toFile().also { file -> index.put(url, file.absolutePath) }
    }
  }

  fun getCachedFileByName(fileName: String): File? {
    if (fileName.isBlank()) return null
    index.getByFileName(fileName)?.let { path ->
      val file = File(path)
      if (file.exists()) return file
    }

    val cacheDir = cacheDirectory() ?: return null
    return cacheDir.walkTopDown()
      .firstOrNull { entry -> entry.isFile && entry.name.contains(fileName) }
  }

  fun cacheDirectory(): File? = imageLoader.diskCache?.directory?.toFile()

  private fun recordCachedFile(request: ImageRequest, diskCacheKey: String?) {
    val url = (request.data as? String)?.let(ImageUrlResolver::resolve).orEmpty()
    val key = diskCacheKey ?: url
    if (key.isBlank()) return
    getCachedFile(url.ifBlank { key })?.let { file -> index.put(url.ifBlank { key }, file.absolutePath) }
  }

  suspend fun fetchDecodedBoardIcon(
    context: Context,
    rawUrl: String,
    sizePx: Int,
  ): Bitmap? {
    val url = ImageUrlResolver.resolve(rawUrl)
    if (url.isBlank()) return null
    decodedBoardIcons.get(url, sizePx)?.let { return it }
    val result =
      imageLoader.execute(
        buildBoardIconRequest(context, url, crossfade = false, sizePx = sizePx),
      )
    if (result is SuccessResult) {
      decodedBoardIcons.storeFromDrawable(url, sizePx, result.drawable)
      return decodedBoardIcons.get(url, sizePx)
    }
    return null
  }

  private companion object {
    const val DISK_CACHE_DIR_NAME = "image_cache"
    const val DECODED_BOARD_ICON_DIR_NAME = "board_icon_decoded"
    const val CONTENT_IMAGE_RAW_DIR_NAME = "content_image_raw"
    const val INDEX_FILE_NAME = "image_cache_index.json"
    const val CONTENT_IMAGE_INDEX_FILE_NAME = "content_image_index.json"
    const val MAX_DISK_CACHE_BYTES = 250L * 1024 * 1024
    const val PREFETCH_MAX_CONCURRENT = 6
    const val BOARD_ICON_PREFETCH_PX = 126
    const val USER_AGENT = "Yanga Android"
  }
}
