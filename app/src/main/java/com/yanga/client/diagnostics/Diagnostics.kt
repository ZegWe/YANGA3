package com.yanga.client.diagnostics

import android.app.ActivityManager
import android.app.Application
import android.os.Build
import android.os.Looper
import android.os.Process
import java.io.File
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal object Diagnostics {
  @Volatile private var journal: DiagnosticJournal? = null
  private val handlingCrash = AtomicBoolean()
  private val fingerprintKey by lazy { ByteArray(32).also { SecureRandom().nextBytes(it) } }
  private val mac = object : ThreadLocal<Mac>() {
    override fun initialValue(): Mac = Mac.getInstance("HmacSHA256").apply {
      init(SecretKeySpec(fingerprintKey, "HmacSHA256"))
    }
  }

  fun install(app: Application) {
    if (journal != null) return
    journal = DiagnosticJournal(File(app.noBackupFilesDir, "diagnostics"))
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, error ->
      try {
        if (handlingCrash.compareAndSet(false, true)) {
          journal?.crash(error, thread === Looper.getMainLooper().thread)
        }
      } catch (_: Throwable) {
        // Diagnostics must never override Android's crash handling.
      } finally {
        if (previous != null) previous.uncaughtException(thread, error)
        else {
          Process.killProcess(Process.myPid())
          kotlin.system.exitProcess(10)
        }
      }
    }
    @Suppress("DEPRECATION")
    val info = app.packageManager.getPackageInfo(app.packageName, 0)
    event { "app_start version=${codeName(info.versionName.orEmpty())} sdk=${Build.VERSION.SDK_INT}" }
    if (Build.VERSION.SDK_INT >= 30) Thread({
      runCatching {
        val prefs = app.getSharedPreferences("diagnostic_exit", 0)
        val last = prefs.getLong("timestamp", 0L)
        val exits = app.getSystemService(ActivityManager::class.java)
          ?.getHistoricalProcessExitReasons(app.packageName, 0, 8).orEmpty()
        val fresh = exits.filter { it.timestamp > last }.sortedBy { it.timestamp }
        fresh.forEach { exit ->
          event { "process_exit timestamp=${exit.timestamp} reason=${exit.reason} status=${exit.status}" }
        }
        fresh.lastOrNull()?.let { prefs.edit().putLong("timestamp", it.timestamp).apply() }
      }
    }, "yanga-exit-info").apply { isDaemon = true; start() }
  }

  inline fun event(details: () -> String) {
    if (journal != null) runCatching { journal?.append(details()) }
  }

  fun snapshot(): String = journal?.snapshot().orEmpty()

  /** Equality marker only, scoped to this process; the key is never written or exported. */
  fun fingerprint(text: String): String {
    val digest = checkNotNull(mac.get()).doFinal(text.toByteArray(Charsets.UTF_8))
    val hex = "0123456789abcdef"
    return buildString(16) {
      for (index in 0 until 8) {
        val byte = digest[index].toInt() and 0xff
        append(hex[byte ushr 4])
        append(hex[byte and 0x0f])
      }
    }
  }

  fun textStats(text: String): String {
    var replacements = 0
    var controls = 0
    var privateUse = 0
    for (ch in text) {
      when {
        ch == '\uFFFD' -> replacements++
        ch.isISOControl() && ch !in "\n\t\r" -> controls++
        ch in '\uE000'..'\uF8FF' -> privateUse++
      }
    }
    return "chars=${text.length} replacement=$replacements controls=$controls private_use=$privateUse"
  }
}
