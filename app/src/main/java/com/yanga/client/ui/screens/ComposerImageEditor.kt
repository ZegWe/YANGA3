package com.yanga.client.ui

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.view.MotionEvent
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yanga.client.api.NgaTopicPosting
import com.yanga.client.ui.content.ComposerImageEdit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComposerImageEditor(file: PendingTopicAttachment, onSave: (PendingTopicAttachment) -> Unit, onClose: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var bitmap by remember { mutableStateOf<Bitmap?>(null) }
  var error by remember { mutableStateOf<String?>(null) }
  var saving by remember { mutableStateOf(false) }
  var mode by remember { mutableStateOf("画线") }
  var color by remember { mutableStateOf("red") }
  var size by remember { mutableFloatStateOf(10f) }
  var label by remember { mutableStateOf("标注") }
  var revision by remember { mutableIntStateOf(0) }
  val drawing = remember { ComposerDrawingView(context) { revision++ } }
  suspend fun load(uri: Uri, rotate: Int = 0, square: Boolean = false): Bitmap = withContext(Dispatchers.IO) {
    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
      val output = java.io.ByteArrayOutputStream()
      val buffer = ByteArray(8192)
      while (true) {
        val n = input.read(buffer); if (n < 0) break
        require(output.size() + n <= NgaTopicPosting.MAX_FILE_BYTES) { "图片不能超过 20 MB" }
        output.write(buffer, 0, n)
      }
      output.toByteArray()
    } ?: error("无法读取图片")
    val png = ComposerImageEdit.transform(bytes, rotate, square)
    BitmapFactory.decodeByteArray(png, 0, png.size) ?: error("图片格式不支持")
  }
  LaunchedEffect(file.uri) {
    runCatching { load(Uri.parse(file.uri), file.rotation, file.square) }.fold(
      onSuccess = { bitmap = it; drawing.source = it; drawing.invalidate() },
      onFailure = { error = it.message },
    )
  }
  val stickerPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    if (uri != null) scope.launch {
      runCatching { load(uri) }.fold(onSuccess = { drawing.sticker = it; mode = "贴画" }, onFailure = { error = it.message })
    }
  }
  fun save() {
    if (bitmap == null || saving) return
    saving = true
    val result = drawing.render()
    scope.launch {
      val saved = runCatching {
        withContext(Dispatchers.IO) {
          val output = File(context.cacheDir, "topic-edit-${UUID.randomUUID()}.png")
          output.outputStream().use { require(result.compress(Bitmap.CompressFormat.PNG, 100, it)) }
          output
        }
      }
      result.recycle()
      saving = false
      saved.fold(
        onSuccess = { onSave(file.copy(uri = Uri.fromFile(it).toString(), name = file.name.substringBeforeLast('.') + ".png", rotation = 0, square = false, error = null)) },
        onFailure = { error = it.message },
      )
    }
  }
  Dialog(onDismissRequest = { if (!saving) onClose() }, properties = DialogProperties(
    usePlatformDefaultWidth = false, dismissOnBackPress = !saving, dismissOnClickOutside = false,
  )) {
    Scaffold(
      modifier = Modifier.fillMaxSize().imePadding(),
      topBar = {
        TopAppBar(
          title = { Text("编辑图片") },
          navigationIcon = {
            IconButton(onClick = onClose, enabled = !saving) {
              Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回并放弃图片编辑")
            }
          },
          actions = {
            TextButton(onClick = drawing::undo, enabled = revision > 0 && !saving) { Text("撤销") }
            Button(onClick = ::save, enabled = bitmap != null && !saving) { Text("保存") }
            Spacer(Modifier.width(12.dp))
          },
        )
      },
    ) { padding ->
      Column(Modifier.fillMaxSize().padding(padding)) {
        Box(Modifier.weight(1f).fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
          bitmap?.let { image ->
            AndroidView(factory = { drawing }, update = {
              it.mode = mode; it.penColor = Color.parseColor(color); it.penSize = size; it.label = label; it.isEnabled = !saving
            }, modifier = Modifier.fillMaxWidth().aspectRatio(image.width.toFloat() / image.height, matchHeightConstraintsFirst = true))
          }
          if (bitmap == null && error == null) CircularProgressIndicator()
          if (saving) CircularProgressIndicator()
        }
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 2.dp) {
          Column(Modifier.fillMaxWidth().heightIn(max = 280.dp).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("标注工具", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf("画线", "箭头", "文字", "贴画").forEach { tool ->
                FilterChip(mode == tool, onClick = { mode = tool }, label = { Text(tool) }, enabled = !saving)
              }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf("red" to "红", "blue" to "蓝", "green" to "绿", "yellow" to "黄", "white" to "白", "black" to "黑").forEach { (value, name) ->
                FilterChip(color == value, onClick = { color = value }, label = { Text(name) }, enabled = !saving)
              }
            }
            Text("笔画 / 文字 / 贴画大小", style = MaterialTheme.typography.labelMedium)
            Slider(value = size, onValueChange = { size = it }, valueRange = 3f..40f, enabled = !saving)
            if (mode == "文字") OutlinedTextField(label, { label = it }, label = { Text("点击图片放置文字") }, enabled = !saving)
            if (mode == "贴画") TextButton(onClick = { stickerPicker.launch("image/*") }, enabled = !saving) { Text("选择贴画图片，然后点击放置") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text("保存为静态 PNG，最长边不超过 2048 像素；原文件不变。", style = MaterialTheme.typography.bodySmall)
          }
        }
      }
    }
  }
}

internal class ComposerDrawingView(context: Context, val changed: () -> Unit) : View(context) {
  var source: Bitmap? = null
  var sticker: Bitmap? = null
  var mode = "画线"
  var penColor = Color.RED
  var penSize = 10f
  var label = "标注"
  private data class Mark(val mode: String, val points: MutableList<PointF>, val color: Int, val width: Float, val text: String, val image: Bitmap?)
  private val marks = mutableListOf<Mark>()
  private var current: Mark? = null
  fun undo() { if (marks.isNotEmpty()) { marks.removeAt(marks.lastIndex); invalidate(); changed() } }
  fun render(): Bitmap {
    val image = requireNotNull(source)
    return Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888).also { paintAll(Canvas(it)) }
  }
  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    val image = source ?: return
    canvas.save(); canvas.scale(width.toFloat() / image.width, height.toFloat() / image.height)
    paintAll(canvas); canvas.restore()
  }
  private fun paintAll(canvas: Canvas) {
    source?.let { canvas.drawBitmap(it, 0f, 0f, null) }
    for (mark in marks + listOfNotNull(current)) {
      if (mark.points.isEmpty()) continue
      val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = mark.color; strokeWidth = mark.width; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; style = Paint.Style.STROKE }
      val start = mark.points.first(); val end = mark.points.last()
      when (mark.mode) {
        "文字" -> { paint.style = Paint.Style.FILL; paint.textSize = mark.width * 5; canvas.drawText(mark.text, start.x, start.y, paint) }
        "贴画" -> mark.image?.let { image ->
          val w = mark.width * 12; val h = w * image.height / image.width
          canvas.drawBitmap(image, null, RectF(start.x - w / 2, start.y - h / 2, start.x + w / 2, start.y + h / 2), null)
        }
        "箭头" -> {
          canvas.drawLine(start.x, start.y, end.x, end.y, paint)
          val angle = atan2(end.y - start.y, end.x - start.x)
          for (delta in listOf(-.55f, .55f)) canvas.drawLine(end.x, end.y, end.x - cos(angle + delta) * mark.width * 5, end.y - sin(angle + delta) * mark.width * 5, paint)
        }
        else -> {
          if (mark.points.size == 1) canvas.drawPoint(start.x, start.y, paint)
          else {
            val path = Path().apply { moveTo(start.x, start.y); mark.points.drop(1).forEach { lineTo(it.x, it.y) } }
            canvas.drawPath(path, paint)
          }
        }
      }
    }
  }
  override fun onTouchEvent(event: MotionEvent): Boolean {
    val image = source ?: return false
    if (!isEnabled) return false
    val point = PointF((event.x / width * image.width).coerceIn(0f, image.width.toFloat()), (event.y / height * image.height).coerceIn(0f, image.height.toFloat()))
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> { parent?.requestDisallowInterceptTouchEvent(true); current = Mark(mode, mutableListOf(point), penColor, penSize * image.width / 720f, label, sticker) }
      MotionEvent.ACTION_MOVE -> current?.let { if (it.mode == "画线" || it.mode == "箭头") it.points += point }
      MotionEvent.ACTION_UP -> { current?.let { marks += it }; current = null; parent?.requestDisallowInterceptTouchEvent(false); changed(); performClick() }
      MotionEvent.ACTION_CANCEL -> { current = null; parent?.requestDisallowInterceptTouchEvent(false) }
    }
    invalidate(); return true
  }
  override fun performClick(): Boolean { super.performClick(); return true }
}
