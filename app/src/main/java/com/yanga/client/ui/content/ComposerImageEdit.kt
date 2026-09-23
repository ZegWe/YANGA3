package com.yanga.client.ui.content

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import java.io.ByteArrayOutputStream

/** Bounded decode for the optional rotate/center-crop editor; original bytes are untouched otherwise. */
object ComposerImageEdit {
  fun transform(bytes: ByteArray, rotation: Int, square: Boolean): ByteArray {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "此文件不能作为静态图片编辑" }
    var sample = 1
    while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) sample *= 2
    var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
      ?: error("无法解码图片")
    try {
      if (square) {
        val edge = minOf(bitmap.width, bitmap.height)
        val next = Bitmap.createBitmap(bitmap, (bitmap.width - edge) / 2, (bitmap.height - edge) / 2, edge, edge)
        if (next !== bitmap) bitmap.recycle()
        bitmap = next
      }
      if (rotation % 360 != 0) {
        val next = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
        if (next !== bitmap) bitmap.recycle()
        bitmap = next
      }
      return ByteArrayOutputStream().use { output ->
        require(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "图片转换失败" }
        output.toByteArray()
      }
    } finally { bitmap.recycle() }
  }
}
