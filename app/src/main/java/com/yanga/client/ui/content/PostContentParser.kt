package com.yanga.client.ui.content

import com.yanga.client.api.NgaStaticUrls
import com.yanga.client.data.image.ImageUrlResolver

data class PostTextStyleRange(
  val start: Int,
  val end: Int,
  val bold: Boolean = false,
  val italic: Boolean = false,
  val underline: Boolean = false,
  val strikeThrough: Boolean = false,
  val color: String? = null,
  val sizePercent: Int? = null,
  val linkUrl: String? = null,
)

sealed class PostContentPart {
  data class Text(
    val text: String,
    val styles: List<PostTextStyleRange> = emptyList(),
  ) : PostContentPart()

  data class Quote(
    val text: String,
    val styles: List<PostTextStyleRange> = emptyList(),
  ) : PostContentPart()

  data class Image(val url: String) : PostContentPart()

  data class Emoticon(
    val code: String,
    val url: String,
    val alt: String,
  ) : PostContentPart()
}

object PostContentParser {
  private val quoteRegex = Regex("\\[quote\\]([\\s\\S]*?)\\[/quote\\]", RegexOption.IGNORE_CASE)
  private val bbCodeImageRegex = Regex("\\[img(?:\\s+[^\\]]*)?\\]([\\s\\S]*?)\\[/img\\]", RegexOption.IGNORE_CASE)
  private val bbCodeImageAttrRegex = Regex("\\[img\\s+[^\\]]*src\\s*=\\s*[\"']?([^\"'\\]]+)[\"']?[^\\]]*\\]", RegexOption.IGNORE_CASE)
  private val htmlImageRegex = Regex("<img\\s+[^>]*src\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>", RegexOption.IGNORE_CASE)
  private val relativeImageRegex = Regex("(\\.\\/mon_[^\\s\\]\"'<>]+)")
  private val emoticonRegex = Regex("\\[s:([^:\\]]+):([^\\]]+)]", RegexOption.IGNORE_CASE)
  private val tagRegex =
    Regex(
      "\\[(/?)(b|i|u|del|color|size|url|uid|tid|pid)(?:=([^\\]]+))?\\]",
      RegexOption.IGNORE_CASE,
    )
  private val layoutTagRegex =
    Regex("""[\[{]/?(align)(?:=[^\]}]+)?[\]}]""", RegexOption.IGNORE_CASE)
  private val tokenRegex =
    Regex(
      "\\[quote\\][\\s\\S]*?\\[/quote\\]|\\[img(?:\\s+[^\\]]*)?\\][\\s\\S]*?\\[/img\\]|\\[img\\s+[^\\]]*\\]|<img\\s+[^>]*src\\s*=\\s*[\"'][^\"']+[\"'][^>]*>|\\.\\/mon_[^\\s\\]\"'<>]+|\\[s:[^:\\]]+:[^\\]]+]",
      RegexOption.IGNORE_CASE,
    )

  fun parse(content: String): List<PostContentPart> {
    val normalized = decodeBasicEntities(content)
    if (normalized.isBlank()) return emptyList()

    val parts = mutableListOf<PostContentPart>()
    var cursor = 0
    for (match in tokenRegex.findAll(normalized)) {
      if (match.range.first > cursor) {
        appendText(parts, normalized.substring(cursor, match.range.first))
      }
      appendToken(parts, match.value)
      cursor = match.range.last + 1
    }

    if (cursor < normalized.length) {
      appendText(parts, normalized.substring(cursor))
    }

    return if (parts.isEmpty()) listOf(PostContentPart.Text(normalized.trim())) else parts
  }

  private fun appendToken(parts: MutableList<PostContentPart>, token: String) {
    quoteRegex.matchEntire(token)?.let { match ->
      val richText = parseRichText(match.groupValues[1])
      if (richText.text.isNotEmpty()) {
        parts += PostContentPart.Quote(richText.text, richText.styles)
      }
      return
    }

    extractImageUrl(token)?.let { url ->
      parts += PostContentPart.Image(normalizeContentImageUrl(url))
      return
    }

    emoticonRegex.matchEntire(token)?.let { match ->
      resolveEmoticon(match.groupValues[1], match.groupValues[2])?.let { parts += it }
      return
    }

    appendText(parts, token)
  }

  private fun appendText(parts: MutableList<PostContentPart>, raw: String) {
    val richText = parseRichText(raw)
    if (richText.text.isNotEmpty()) {
      parts += PostContentPart.Text(richText.text, richText.styles)
    }
  }

  private fun parseRichText(raw: String): RichText {
    val value = stripLayoutTags(stripHtml(raw))
    val output = StringBuilder()
    val styles = mutableListOf<PostTextStyleRange>()
    val stack = mutableListOf<OpenStyle>()
    var cursor = 0

    for (match in tagRegex.findAll(value)) {
      if (match.range.first > cursor) {
        output.append(value.substring(cursor, match.range.first))
      }

      val isClosing = match.groupValues[1] == "/"
      val tag = match.groupValues[2].lowercase()
      val arg = match.groupValues.getOrNull(3).orEmpty()
      if (isClosing) {
        closeStyle(tag, output.length, stack, styles)
      } else {
        openStyle(tag, arg, output.length, stack)
      }
      cursor = match.range.last + 1
    }

    if (cursor < value.length) {
      output.append(value.substring(cursor))
    }
    while (stack.isNotEmpty()) {
      closeStyle(stack.last().tag, output.length, stack, styles)
    }

    val normalizedText = normalizeWhitespace(output.toString())
    if (normalizedText.leadingTrim == 0 && normalizedText.trailingTrim == 0) {
      return RichText(normalizedText.text, styles.filter { it.start < it.end })
    }

    val adjustedStyles =
      styles
        .mapNotNull { style ->
          val start = (style.start - normalizedText.leadingTrim).coerceAtLeast(0)
          val end = (style.end - normalizedText.leadingTrim).coerceAtMost(normalizedText.text.length)
          if (start < end) style.copy(start = start, end = end) else null
        }
    return RichText(normalizedText.text, adjustedStyles)
  }

  private fun openStyle(tag: String, arg: String, start: Int, stack: MutableList<OpenStyle>) {
    val style =
      when (tag) {
        "b" -> ActiveStyle(bold = true)
        "i" -> ActiveStyle(italic = true)
        "u" -> ActiveStyle(underline = true)
        "del" -> ActiveStyle(strikeThrough = true)
        "color" -> ActiveStyle(color = arg.trim().ifBlank { null })
        "size" -> ActiveStyle(sizePercent = arg.trim().removeSuffix("%").toIntOrNull())
        "url" -> ActiveStyle(linkUrl = arg.trim().ifBlank { null })
        "uid" -> ActiveStyle(linkUrl = arg.trim().ifBlank { null }?.let { "nga://user/$it" })
        "tid" -> ActiveStyle(linkUrl = arg.trim().ifBlank { null }?.let { "nga://thread/$it" })
        "pid" -> ActiveStyle(linkUrl = arg.trim().ifBlank { null }?.let { "nga://post/$it" })
        else -> ActiveStyle()
      }
    stack += OpenStyle(tag, start, style)
  }

  private fun closeStyle(
    tag: String,
    end: Int,
    stack: MutableList<OpenStyle>,
    styles: MutableList<PostTextStyleRange>,
  ) {
    val index = stack.indexOfLast { it.tag == tag }
    if (index == -1) return

    val open = stack.removeAt(index)
    if (open.start >= end) return
    val linkUrl =
      if (open.tag == "url" && open.style.linkUrl == null) {
        null
      } else {
        open.style.linkUrl
      }
    styles +=
      PostTextStyleRange(
        start = open.start,
        end = end,
        bold = open.style.bold,
        italic = open.style.italic,
        underline = open.style.underline,
        strikeThrough = open.style.strikeThrough,
        color = open.style.color,
        sizePercent = open.style.sizePercent,
        linkUrl = linkUrl,
      )
  }

  private fun extractImageUrl(token: String): String? {
    bbCodeImageRegex.matchEntire(token)?.let { match ->
      return match.groupValues[1].trim().ifBlank { null }
    }
    bbCodeImageAttrRegex.find(token)?.let { match ->
      return match.groupValues[1].trim().ifBlank { null }
    }
    htmlImageRegex.find(token)?.let { match ->
      return match.groupValues[1].trim().ifBlank { null }
    }
    relativeImageRegex.find(token)?.let { match ->
      return match.groupValues[1].trim().ifBlank { null }
    }
    return null
  }

  private fun resolveEmoticon(category: String, alt: String): PostContentPart.Emoticon? {
    val image = NgaEmoticons.resolve(category, alt) ?: return null
    val url = NgaStaticUrls.emoticonBaseUrl + image
    return PostContentPart.Emoticon(code = "[s:$category:$alt]", url = url, alt = alt)
  }

  private fun normalizeContentImageUrl(url: String): String {
    val normalized =
      url
        .replace(Regex("(http\\S+)\\.gif\\.(thumb_s|medium|thumb|thumb_ss)\\.jpg", RegexOption.IGNORE_CASE), "$1.gif")
        .replace(Regex("(http\\S+)\\.(png|jpg)\\.(thumb_s|medium|thumb|thumb_ss)\\.jpg", RegexOption.IGNORE_CASE), "$1.$2")
    return ImageUrlResolver.resolve(normalized)
  }

  private fun stripHtml(value: String): String =
    value
      .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
      .replace(Regex("<[^>]+>"), "")

  private fun stripLayoutTags(value: String): String =
    value.replace(layoutTagRegex, "")

  private fun decodeBasicEntities(content: String): String =
    content
      .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
      .replace("&nbsp;", " ")
      .replace("&lt;", "<")
      .replace("&gt;", ">")
      .replace("&amp;", "&")

  private fun normalizeWhitespace(value: String): NormalizedText {
    val normalized = value.replace(Regex("[ \\t\\x0B\\f\\r]+"), " ")
    val trimmed = normalized.trim()
    return NormalizedText(
      text = trimmed,
      leadingTrim = normalized.length - normalized.trimStart().length,
      trailingTrim = normalized.length - normalized.trimEnd().length,
    )
  }

  private data class RichText(
    val text: String,
    val styles: List<PostTextStyleRange>,
  )

  private data class NormalizedText(
    val text: String,
    val leadingTrim: Int,
    val trailingTrim: Int,
  )

  private data class OpenStyle(
    val tag: String,
    val start: Int,
    val style: ActiveStyle,
  )

  private data class ActiveStyle(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikeThrough: Boolean = false,
    val color: String? = null,
    val sizePercent: Int? = null,
    val linkUrl: String? = null,
  )
}

private object NgaEmoticons {
  private val ac =
    mapOf(
      "blink" to "ac0.png",
      "goodjob" to "ac1.png",
      "上" to "ac2.png",
      "中枪" to "ac3.png",
      "偷笑" to "ac4.png",
      "冷" to "ac5.png",
      "凌乱" to "ac6.png",
      "反对" to "ac7.png",
      "吓" to "ac8.png",
      "吻" to "ac9.png",
      "呆" to "ac10.png",
      "咦" to "ac11.png",
      "哦" to "ac12.png",
      "哭" to "ac13.png",
      "哭1" to "ac14.png",
      "哭笑" to "ac15.png",
      "哼" to "ac16.png",
      "喘" to "ac17.png",
      "喷" to "ac18.png",
      "嘲笑" to "ac19.png",
      "嘲笑1" to "ac20.png",
      "囧" to "ac21.png",
      "委屈" to "ac22.png",
      "心" to "ac23.png",
      "忧伤" to "ac24.png",
      "怒" to "ac25.png",
      "怕" to "ac26.png",
      "惊" to "ac27.png",
      "愁" to "ac28.png",
      "抓狂" to "ac29.png",
      "抠鼻" to "ac30.png",
      "擦汗" to "ac31.png",
      "无语" to "ac32.png",
      "晕" to "ac33.png",
      "汗" to "ac34.png",
      "瞎" to "ac35.png",
      "羞" to "ac36.png",
      "羡慕" to "ac37.png",
      "花痴" to "ac38.png",
      "茶" to "ac39.png",
      "衰" to "ac40.png",
      "计划通" to "ac41.png",
      "赞同" to "ac43.png",
      "闪光" to "ac42.png",
      "黑枪" to "ac44.png",
    )

  private val a2 =
    linkedMapOf(
      "goodjob" to "a2_02.png",
      "诶嘿" to "a2_05.png",
      "偷笑" to "a2_03.png",
      "怒" to "a2_04.png",
      "笑" to "a2_07.png",
      "那个…" to "a2_08.png",
      "哦嗬嗬嗬" to "a2_09.png",
      "舔" to "a2_10.png",
      "鬼脸" to "a2_14.png",
      "冷" to "a2_16.png",
      "大哭" to "a2_15.png",
      "哭" to "a2_17.png",
      "恨" to "a2_21.png",
      "中枪" to "a2_23.png",
      "囧" to "a2_24.png",
      "你看看你" to "a2_25.png",
      "doge" to "a2_27.png",
      "自戳双目" to "a2_28.png",
      "偷吃" to "a2_30.png",
      "冷笑" to "a2_31.png",
      "壁咚" to "a2_32.png",
      "不活了" to "a2_33.png",
      "不明觉厉" to "a2_36.png",
      "是在下输了" to "a2_51.png",
      "你为猴这么" to "a2_53.png",
      "干杯" to "a2_54.png",
      "干杯2" to "a2_55.png",
      "异议" to "a2_47.png",
      "认真" to "a2_48.png",
      "你已经死了" to "a2_45.png",
      "你这种人…" to "a2_49.png",
      "妮可妮可妮" to "a2_18.png",
      "惊" to "a2_19.png",
      "抢镜头" to "a2_52.png",
      "yes" to "a2_26.png",
      "有何贵干" to "a2_11.png",
      "病娇" to "a2_12.png",
      "lucky" to "a2_13.png",
      "poi" to "a2_20.png",
      "囧2" to "a2_22.png",
      "威吓" to "a2_42.png",
      "jojo立" to "a2_37.png",
      "jojo立2" to "a2_38.png",
      "jojo立3" to "a2_39.png",
      "jojo立4" to "a2_41.png",
      "jojo立5" to "a2_40.png",
    )

  private val ng =
    linkedMapOf(
      "呲牙笑" to "ng_1.png",
      "奸笑" to "ng_2.png",
      "问号" to "ng_3.png",
      "茶" to "ng_4.png",
      "笑指" to "ng_5.png",
      "燃尽" to "ng_6.png",
      "晕" to "ng_7.png",
      "扇笑" to "ng_8.png",
      "寄" to "ng_9.png",
      "别急" to "ng_10.png",
      "doge" to "ng_11.png",
      "丧" to "ng_12.png",
      "汗" to "ng_13.png",
      "叹气" to "ng_15.png",
      "吃饼" to "ng_16.png",
      "吃瓜" to "ng_17.png",
      "吐舌" to "ng_18.png",
      "哭" to "ng_19.png",
      "喘" to "ng_20.png",
      "心" to "ng_21.png",
      "喷" to "ng_22.png",
      "困" to "ng_24.png",
      "大哭" to "ng_25.png",
      "大惊" to "ng_26.png",
      "害怕" to "ng_27.png",
      "惊" to "ng_28.png",
      "暴怒" to "ng_30.png",
      "气愤" to "ng_31.png",
      "热" to "ng_32.png",
      "瓜不熟" to "ng_33.png",
      "瞎" to "ng_34.png",
      "色" to "ng_35.png",
      "斜眼" to "ng_37.png",
      "问号大" to "ng_38.png",
    )

  private val pg =
    linkedMapOf(
      "战斗力" to "pg01.png",
      "哈啤" to "pg02.png",
      "满分" to "pg03.png",
      "衰" to "pg04.png",
      "拒绝" to "pg05.png",
      "心" to "pg06.png",
      "严肃" to "pg07.png",
      "吃瓜" to "pg08.png",
      "嘣" to "pg09.png",
      "嘣2" to "pg10.png",
      "冻" to "pg11.png",
      "谢" to "pg12.png",
      "哭" to "pg13.png",
      "响指" to "pg14.png",
      "转身" to "pg15.png",
    )

  private val pst =
    listOf(
      "举手",
      "亲",
      "偷笑",
      "偷笑2",
      "偷笑3",
      "傻眼",
      "傻眼2",
      "兔子",
      "发光",
      "呆",
      "呆2",
      "呆3",
      "呕",
      "呵欠",
      "哭",
      "哭2",
      "哭3",
      "嘲笑",
      "基",
      "宅",
      "安慰",
      "幸福",
      "开心",
      "开心2",
      "开心3",
      "怀疑",
      "怒",
      "怒2",
      "怨",
      "惊吓",
      "惊吓2",
      "惊呆",
      "惊呆2",
      "惊呆3",
      "惨",
      "斜眼",
      "晕",
      "汗",
      "泪",
      "泪2",
      "泪3",
      "泪4",
      "满足",
      "满足2",
      "火星",
      "牙疼",
      "电击",
      "看戏",
      "眼袋",
      "眼镜",
      "笑而不语",
      "紧张",
      "美味",
      "背",
      "脸红",
      "脸红2",
      "腐",
      "星星眼",
      "谢",
      "醉",
      "闷",
      "闷2",
      "音乐",
      "黑脸",
      "鼻血",
    ).mapIndexed { index, code -> code to "pt${index.toString().padStart(2, '0')}.png" }.toMap()

  private val dt =
    listOf(
      "ROLL",
      "上",
      "傲娇",
      "叉出去",
      "发光",
      "呵欠",
      "哭",
      "啃古头",
      "嘲笑",
      "心",
      "怒",
      "怒2",
      "怨",
      "惊",
      "惊2",
      "无语",
      "星星眼",
      "星星眼2",
      "晕",
      "注意",
      "注意2",
      "泪",
      "泪2",
      "烧",
      "笑",
      "笑2",
      "笑3",
      "脸红",
      "药",
      "衰",
      "鄙视",
      "闲",
      "黑脸",
    ).mapIndexed { index, code -> code to "dt${(index + 1).toString().padStart(2, '0')}.png" }.toMap()

  private val groups = mapOf("ac" to ac, "a2" to a2, "ng" to ng, "pg" to pg, "pst" to pst, "dt" to dt)

  fun resolve(category: String, code: String): String? = groups[category.lowercase()]?.get(code)
}
