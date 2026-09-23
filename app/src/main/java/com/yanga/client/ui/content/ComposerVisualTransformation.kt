package com.yanga.client.ui.content

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em

/** Show common text formatting while keeping the exact BBCode as the editing source. */
class ComposerVisualTransformation : VisualTransformation {
  override fun filter(text: AnnotatedString): TransformedText {
    val source = text.text
    val regex = Regex("\\[(/?)(b|u|i|del|color|size|font)(?:=([^]\\n]+))?]", RegexOption.IGNORE_CASE)
    val tokens = regex.findAll(source).associateBy { it.range.first }
    val result = AnnotatedString.Builder()
    val original = IntArray(source.length + 1)
    val transformed = mutableListOf(0)
    data class Open(val name: String, val start: Int, val style: SpanStyle)
    val stack = mutableListOf<Open>()
    var i = 0
    while (i < source.length) {
      val match = tokens[i]
      if (match != null) {
        val name = match.groupValues[2].lowercase()
        if (match.groupValues[1].isNotEmpty()) {
          val index = stack.indexOfLast { it.name == name }
          if (index >= 0) {
            val open = stack.removeAt(index)
            if (result.length > open.start) result.addStyle(open.style, open.start, result.length)
          }
        } else {
          val parameter = match.groupValues[3]
          val style = when (name) {
            "b" -> SpanStyle(fontWeight = FontWeight.Bold)
            "i" -> SpanStyle(fontStyle = FontStyle.Italic)
            "u" -> SpanStyle(textDecoration = TextDecoration.Underline)
            "del" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
            "font" -> SpanStyle(fontFamily = when (parameter.lowercase()) { "monospace", "courier", "courier new" -> FontFamily.Monospace; "serif", "宋体", "simsun" -> FontFamily.Serif; else -> FontFamily.SansSerif })
            "size" -> SpanStyle(fontSize = ((parameter.removeSuffix("%").toFloatOrNull() ?: 100f) / 100f).coerceIn(.5f, 3f).em)
            "color" -> SpanStyle(color = runCatching { Color(android.graphics.Color.parseColor(parameter)) }.getOrDefault(Color.Unspecified))
            else -> SpanStyle()
          }
          stack += Open(name, result.length, style)
        }
        for (j in i..match.range.last + 1) original[j] = result.length
        i = match.range.last + 1
        // Prefer insertion just inside opening tags, rather than editing their syntax.
        if (match.groupValues[1].isEmpty()) transformed[transformed.lastIndex] = i
      } else {
        original[i] = result.length
        result.append(source[i]); i++
        original[i] = result.length; transformed += i
      }
    }
    stack.forEach { if (result.length > it.start) result.addStyle(it.style, it.start, result.length) }
    return TransformedText(result.toAnnotatedString(), object : OffsetMapping {
      override fun originalToTransformed(offset: Int): Int = original[offset.coerceIn(0, source.length)]
      override fun transformedToOriginal(offset: Int): Int = transformed[offset.coerceIn(0, transformed.lastIndex)]
    })
  }
}
