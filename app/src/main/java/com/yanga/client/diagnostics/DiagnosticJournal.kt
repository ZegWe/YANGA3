package com.yanga.client.diagnostics

import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** App-private, bounded journal. Only call with fixed labels and derived diagnostic metadata. */
internal class DiagnosticJournal(private val directory: File) {
  private val lock = Any()
  private val recent = ArrayDeque<String>()
  private val dropped = AtomicInteger()
  private val writer = ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, ArrayBlockingQueue(256),
    { job -> Thread(job, "yanga-diagnostics").apply { isDaemon = true } },
    { _, _ -> dropped.incrementAndGet() })

  fun append(event: String) {
    val line = "${System.currentTimeMillis()} ${event.take(1000)}\n"
    synchronized(recent) {
      if (recent.size == 80) recent.removeFirst()
      recent.addLast(line)
    }
    writer.execute {
      runCatching {
        synchronized(lock) {
          directory.mkdirs()
          val current = File(directory, "events.log")
          if (current.length() >= 256 * 1024) {
            val previous = File(directory, "events-previous.log")
            previous.delete()
            if (!current.renameTo(previous)) return@synchronized
          }
          val lost = dropped.getAndSet(0)
          if (lost > 0) current.appendText("${System.currentTimeMillis()} queue_dropped count=$lost\n")
          current.appendText(line)
        }
      }
    }
  }

  /** Separate synchronous write: never wait for the normal writer when crashing. */
  fun crash(error: Throwable, mainThread: Boolean) {
    val stamp = System.currentTimeMillis()
    val report = buildString {
      appendLine("$stamp fatal java_or_kotlin main_thread=$mainThread")
      val seen = java.util.IdentityHashMap<Throwable, Boolean>()
      var cause: Throwable? = error
      repeat(8) {
        val current = cause ?: return@repeat
        if (seen.put(current, true) != null) { cause = null; return@repeat }
        appendLine("exception=${codeName(current.javaClass.name)}")
        // Messages, thread names and suppressed exception messages can contain raw data.
        current.stackTrace.take(44).forEach { frame ->
          appendLine("  at ${codeName(frame.className)}.${codeName(frame.methodName)}:${frame.lineNumber}")
        }
        cause = current.cause
      }
      appendLine("--- Recent activity (may overlap event journal) ---")
      synchronized(recent) { recent.forEach { append(it) } }
    }
    directory.mkdirs()
    // Choose the oldest slot, preserving the most recent reports even on rapid restarts.
    val files = List(3) { File(directory, "crash-$it.log") }
    val destination = files.firstOrNull { !it.exists() } ?: files.minByOrNull { it.lastModified() } ?: files.first()
    val pending = File(directory, "crash-pending.log")
    FileOutputStream(pending).use {
      it.write(report.take(125_000).toByteArray(Charsets.UTF_8))
      it.fd.sync()
    }
    // Same directory, atomic replace on Android. Keep the pending report if replacement fails.
    pending.renameTo(destination)
  }

  fun snapshot(): String {
    // Drains earlier work on the normal path; caller runs on IO, not the main thread.
    val barrier = java.util.concurrent.FutureTask<Unit> { Unit }
    runCatching { writer.execute(barrier); barrier.get(700, TimeUnit.MILLISECONDS) }
    return synchronized(lock) {
      buildString {
        appendLine("Yanga diagnostics v1 | timestamps are Unix milliseconds")
        for (name in listOf("events-previous.log", "events.log")) {
          val file = File(directory, name)
          if (file.isFile) append(file.readText())
        }
        directory.listFiles()?.filter { it.name.matches(Regex("crash-(?:[0-2]|pending)\\.log")) }
          ?.sortedBy { it.lastModified() }?.forEach {
            appendLine("--- Crash report ---")
            append(it.readText())
          }
      }
    }
  }
}

internal fun codeName(value: String): String =
  value.take(160).replace(Regex("[^A-Za-z0-9_.$<>-]"), "_")
