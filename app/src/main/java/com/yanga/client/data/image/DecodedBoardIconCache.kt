package com.yanga.client.data.image

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import kotlin.math.min

/**
 * Caches board icons as decoded [Bitmap]s at a fixed display size so repeat
 * loads skip image decoding. Disk files store raw ARGB pixels (not PNG/WebP).
 */
internal class DecodedBoardIconCache(
  private val cacheDir: File,
) {
  private val memoryCache =
    object : LruCache<String, Bitmap>(MEMORY_MAX_BYTES) {
      override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount

      override fun entryRemoved(
        evicted: Boolean,
        key: String,
        oldValue: Bitmap,
        newValue: Bitmap?,
      ) {
        if (evicted && !oldValue.isRecycled) {
          oldValue.recycle()
        }
      }
    }

  fun get(url: String, sizePx: Int): Bitmap? {
    val key = cacheKey(url, sizePx)
    synchronized(memoryCache) {
      memoryCache.get(key)?.takeUnless { it.isRecycled }?.let { return it }
    }
    return loadFromDisk(key)?.also { putMemory(key, it) }
  }

  fun has(url: String, sizePx: Int): Boolean = get(url, sizePx) != null

  fun store(url: String, sizePx: Int, source: Bitmap) {
    val resolvedUrl = ImageUrlResolver.resolve(url)
    if (resolvedUrl.isBlank()) return
    val scaled = prepareBitmap(source, sizePx) ?: return
    val key = cacheKey(resolvedUrl, sizePx)
    putMemory(key, scaled)
    saveToDisk(key, scaled)
  }

  fun storeFromDrawable(url: String, sizePx: Int, drawable: Drawable) {
    val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return
    store(url, sizePx, bitmap)
  }

  private fun putMemory(key: String, bitmap: Bitmap) {
    synchronized(memoryCache) {
      memoryCache.put(key, bitmap)
    }
  }

  private fun cacheKey(url: String, sizePx: Int): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest("$url@$sizePx".toByteArray(Charsets.UTF_8))
    return hash.joinToString("") { byte -> "%02x".format(byte) }
  }

  private fun diskFile(key: String): File {
    cacheDir.mkdirs()
    return cacheDir.resolve("$key.bin")
  }

  private fun saveToDisk(key: String, bitmap: Bitmap) {
    val file = diskFile(key)
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    val buffer = ByteBuffer.allocate(HEADER_BYTES + pixels.size * 4).order(ByteOrder.LITTLE_ENDIAN)
    buffer.putInt(MAGIC)
    buffer.putInt(bitmap.width)
    buffer.putInt(bitmap.height)
    pixels.forEach { argb -> buffer.putInt(argb) }
    file.writeBytes(buffer.array())
  }

  private fun loadFromDisk(key: String): Bitmap? {
    val file = diskFile(key)
    if (!file.exists() || file.length() < HEADER_BYTES) return null
    return runCatching {
      val bytes = file.readBytes()
      val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
      if (buffer.int != MAGIC) return null
      val width = buffer.int
      val height = buffer.int
      if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION) return null
      val pixelCount = width * height
      if (bytes.size < HEADER_BYTES + pixelCount * 4) return null
      val pixels = IntArray(pixelCount)
      for (index in 0 until pixelCount) {
        pixels[index] = buffer.int
      }
      Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }.getOrNull()
  }

  private fun prepareBitmap(source: Bitmap, sizePx: Int): Bitmap? {
    if (source.isRecycled) return null
    val target = sizePx.coerceAtLeast(1)
    val scale = min(target.toFloat() / source.width, target.toFloat() / source.height)
    val width = (source.width * scale).toInt().coerceAtLeast(1)
    val height = (source.height * scale).toInt().coerceAtLeast(1)
    return if (width == source.width && height == source.height) {
      source.copy(Bitmap.Config.ARGB_8888, false)
    } else {
      Bitmap.createScaledBitmap(source, width, height, true)
    }
  }

  private companion object {
    const val MAGIC = 0x59424943 // YBIC
    const val HEADER_BYTES = 12
    const val MAX_DIMENSION = 512
    const val MEMORY_MAX_BYTES = 32 * 1024 * 1024
  }
}
