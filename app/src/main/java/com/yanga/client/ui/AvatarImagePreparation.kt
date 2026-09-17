package com.yanga.client.ui

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun prepareAvatarImage(resolver: ContentResolver, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
  fun stream() = resolver.openInputStream(uri) ?: error("无法读取所选图片，请重新选择")
  val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
  stream().use { BitmapFactory.decodeStream(it, null, bounds) }
  require(bounds.outWidth > 0 && bounds.outHeight > 0) { "无法识别图片，请选择其他图片" }
  var sample = 1
  while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1024) sample *= 2
  val decoded = stream().use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
    ?: error("图片读取失败")
  val orientation = runCatching { stream().use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) } }.getOrDefault(1)
  val matrix = Matrix().apply {
    when (orientation) {
      2 -> setScale(-1f, 1f)
      3 -> setRotate(180f)
      4 -> setScale(1f, -1f)
      5 -> { setRotate(90f); postScale(-1f, 1f) }
      6 -> setRotate(90f)
      7 -> { setRotate(-90f); postScale(-1f, 1f) }
      8 -> setRotate(-90f)
    }
  }
  val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
  if (rotated !== decoded) decoded.recycle()
  val scale = minOf(1f, 255f / rotated.width, 180f / rotated.height)
  val resized = Bitmap.createScaledBitmap(rotated, (rotated.width * scale).toInt().coerceAtLeast(1), (rotated.height * scale).toInt().coerceAtLeast(1), true)
  if (resized !== rotated) rotated.recycle()
  try {
    ByteArrayOutputStream().use { output ->
      check(resized.compress(Bitmap.CompressFormat.PNG, 100, output)) { "图片处理失败" }
      output.toByteArray()
    }
  } finally { resized.recycle() }
}
