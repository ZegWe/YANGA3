package com.yanga.client.api

import kotlin.io.encoding.Base64

data class NgaTitleStyle(
  val color: String? = null,
  val bold: Boolean = false,
  val italic: Boolean = false,
  val underline: Boolean = false,
)

object NgaTitleStyleParser {
  private val base64 = Base64.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
  fun parse(misc: String, fontBits: Int? = null, titleFont: String = ""): NgaTitleStyle {
    if (fontBits != null) return fromBits(fontBits)
    if (misc.startsWith('~')) return legacy(misc)
    // Records are a one-byte type followed by a four-byte big-endian value.
    // Type 2/3 records contain collection/board ids, not title styles.
    val bytes = runCatching { base64.decode(misc.filterNot(Char::isWhitespace)) }.getOrNull()
    if (bytes != null) {
      for (offset in 0 until bytes.size - 4 step 5) {
        if (bytes[offset].toInt() == 1) {
          var bits = 0
          for (index in offset + 1..offset + 4) bits = (bits shl 8) or (bytes[index].toInt() and 0xff)
          return fromBits(bits)
        }
      }
    }
    return legacy(titleFont)
  }

  private fun fromBits(bits: Int): NgaTitleStyle = NgaTitleStyle(
    color = when {
      bits and 4 != 0 -> "green"
      bits and 2 != 0 -> "blue"
      bits and 1 != 0 -> "red"
      bits and 8 != 0 -> "orange"
      bits and 16 != 0 -> "silver"
      else -> null
    },
    bold = bits and 32 != 0,
    italic = bits and 64 != 0,
    underline = bits and 128 != 0,
  )

  private fun legacy(value: String): NgaTitleStyle {
    val tokens = value.lowercase().split(Regex("[~\\s,]+"))
    val oldEmphasis = value == "~1~~" || value == "~~~1"
    return NgaTitleStyle(
      color = tokens.lastOrNull { it in setOf("red", "blue", "green", "orange", "silver", "sliver") }
        ?.replace("sliver", "silver"),
      bold = oldEmphasis || "b" in tokens,
      italic = oldEmphasis || "i" in tokens,
      underline = "u" in tokens,
    )
  }
}
