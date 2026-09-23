package com.yanga.client.ui.content

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

data class ComposerTool(val id: String, val label: String, val fields: List<String> = emptyList(), val hint: String = "")

object ComposerMarkup {
  val tools = listOf(
    ComposerTool("b", "粗体"), ComposerTool("u", "下划线"), ComposerTool("i", "斜体"), ComposerTool("del", "删除线"),
    ComposerTool("color", "文字颜色", listOf("颜色（名称或 #RRGGBB）")),
    ComposerTool("size", "字号", listOf("百分比（如 120）")), ComposerTool("font", "字体", listOf("字体名称")),
    ComposerTool("align", "对齐", listOf("方向（left / center / right）")),
    ComposerTool("h", "段落标题"), ComposerTool("rule", "分隔线"), ComposerTool("l", "左浮动"), ComposerTool("r", "右浮动"),
    ComposerTool("list", "无序列表", listOf("条目（每行一项）")), ComposerTool("ordered", "有序列表", listOf("条目（每行一项）")),
    ComposerTool("img", "图片链接", listOf("图片网址")), ComposerTool("album", "相册", listOf("相册标题", "图片网址（每行一张）")),
    ComposerTool("url", "超链接", listOf("网址", "链接文字（可选）")), ComposerTool("quote", "引用"),
    ComposerTool("code", "代码块", listOf("语言（可留空）", "代码")),
    ComposerTool("flash", "视频", listOf("分享网址")), ComposerTool("audio", "音频", listOf("音频网址")),
    ComposerTool("table", "表格", listOf("表格内容（列用制表符或 | 分隔）"), "可直接粘贴表格；一行对应一行数据。"),
    ComposerTool("tid", "主题链接", listOf("主题 ID", "链接文字（可选）")), ComposerTool("pid", "回复链接", listOf("回复 ID", "链接文字（可选）")),
    ComposerTool("dice", "骰子", listOf("骰子表达式（如 2d6+1d20）"), "结果由论坛发布后生成，不在预览中模拟。"),
    ComposerTool("collapse", "折叠", listOf("折叠标题（可选）")), ComposerTool("randomblock", "随机段落"),
    ComposerTool("mention", "@ 用户", listOf("用户名或 UID")),
    ComposerTool("customachieve", "自定义成就", listOf("成就名称", "说明", "图标网址（可选）")),
    ComposerTool("armory", "魔兽角色", listOf("地区（us / tw / eu）", "服务器", "角色名")),
    ComposerTool("d3armory", "暗黑角色", listOf("角色资料网址")),
    ComposerTool("iframe", "嵌入网页", listOf("网址", "高度（可选）", "宽度（可选）"), "外站是否允许嵌入由论坛决定。"),
    ComposerTool("dict", "注解", listOf("词条", "解释")),
  )

  fun wrap(value: TextFieldValue, open: String, close: String): TextFieldValue {
    val a = value.selection.min; val b = value.selection.max
    return TextFieldValue(value.text.replaceRange(a, b, open + value.text.substring(a, b) + close), TextRange(a + open.length, b + open.length))
  }
  fun insert(value: TextFieldValue, text: String): TextFieldValue {
    val a = value.selection.min
    return TextFieldValue(value.text.replaceRange(a, value.selection.max, text), TextRange(a + text.length))
  }
  fun build(id: String, values: List<String>, selected: String): String {
    fun v(index: Int) = values.getOrNull(index)?.trim().orEmpty()
    fun parameter(index: Int): String = v(index).also { require(!it.contains(']') && !it.contains('\n')) { "参数不能包含换行或 ]" } }
    fun url(index: Int): String = parameter(index).also { require(it.startsWith("https://") || it.startsWith("http://")) { "请输入完整的 http 或 https 网址" } }
    return when (id) {
      "b", "u", "i", "del", "h", "quote", "l", "r", "randomblock" -> "[$id]$selected[/$id]"
      "rule" -> "\n======\n"
      "color", "size", "font", "align" -> {
        val p = parameter(0)
        require(p.isNotBlank()) { "请填写参数" }
        if (id == "size") require(p.toIntOrNull()?.let { it in 50..300 } == true) { "字号范围为 50–300%" }
        if (id == "align") require(p in listOf("left", "center", "right")) { "请选择 left、center 或 right" }
        if (id == "color") require(p.matches(Regex("#[0-9a-fA-F]{6}|[a-zA-Z]+"))) { "颜色格式无效" }
        "[$id=${if (id == "size") "$p%" else p}]$selected[/$id]"
      }
      "collapse" -> "[collapse${parameter(0).takeIf(String::isNotBlank)?.let { "=$it" }.orEmpty()}]$selected[/collapse]"
      "list", "ordered" -> "[list${if (id == "ordered") "=1" else ""}]\n" + v(0).lines().filter(String::isNotBlank).joinToString("\n") { "[*]$it" } + "\n[/list]"
      "img", "flash", "audio" -> "[$id]${url(0)}[/$id]"
      "url" -> if (v(1).isBlank()) "[url]${url(0)}[/url]" else "[url=${url(0)}]${v(1)}[/url]"
      "album" -> {
        require(v(1).lines().all { it.startsWith("https://") || it.startsWith("http://") }) { "每行请填写一个图片网址" }
        "[album=${parameter(0)}]\n${v(1)}\n[/album]"
      }
      "code" -> "[code${parameter(0).takeIf(String::isNotBlank)?.let { "=$it" }.orEmpty()}]\n${values.getOrNull(1).orEmpty()}\n[/code]"
      "table" -> "[table]\n" + v(0).lines().filter(String::isNotBlank).joinToString("\n") { row ->
        "[tr]" + row.split(if ('\t' in row) '\t' else '|').joinToString("") { "[td]${it.trim()}[/td]" } + "[/tr]"
      } + "\n[/table]"
      "tid", "pid" -> {
        require(v(0).toLongOrNull()?.let { it > 0 } == true) { "请输入有效 ID" }
        if (v(1).isBlank()) "[$id]${v(0)}[/$id]" else "[$id=${v(0)}]${v(1)}[/$id]"
      }
      "dice" -> {
        require(v(0).matches(Regex("(?:[1-9]?[0-9]*d[1-9][0-9]*)(?:\\+(?:[1-9]?[0-9]*d[1-9][0-9]*))*"))) { "格式示例：2d6+1d20" }
        val dice = v(0).split('+').map { it.split('d') }
        require(dice.sumOf { it[0].ifBlank { "1" }.toIntOrNull() ?: 1000 } <= 10 && dice.all { it[1].toIntOrNull()?.let { n -> n <= 1000 } == true }) { "最多 10 个骰子，每个不超过 1000 面" }
        "[dice]${v(0)}[/dice]"
      }
      "mention" -> "[@${parameter(0).also { require(it.isNotBlank()) { "请填写用户" } }}]"
      "customachieve" -> "[customachieve][title]${v(0)}[/title]\n[txt]${v(1)}[/txt]" + (if (v(2).isBlank()) "" else "\n[img]${url(2)}[/img]") + "[/customachieve]"
      "armory" -> {
        require(v(0) in listOf("us", "tw", "eu")) { "请选择 us、tw 或 eu" }
        "[${v(0)}armory ${parameter(1)} ${parameter(2)}]"
      }
      "d3armory" -> "[url]${url(0).substringBefore('#')}#armory[/url]"
      "iframe" -> {
        require((v(1).isBlank() && v(2).isBlank()) || (v(1).toIntOrNull()?.let { it > 0 } == true && (v(2).isBlank() || v(2).toIntOrNull()?.let { it > 0 } == true))) { "尺寸请填写正整数" }
        "[iframe${if (v(1).isBlank()) "" else "=${v(1)}"}${if (v(2).isBlank()) "" else ",${v(2)}"}]${url(0)}[/iframe]"
      }
      "dict" -> "[dict][${parameter(0)}]${v(1)}[/dict]"
      else -> error("不支持的工具")
    }
  }
}
