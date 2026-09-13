package com.yanga.client.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

const val PROJECT_URL = "https://github.com/ZegWe/YANGA3"
data class AppRelease(val version: String, val code: Long, val url: String, val size: Long, val sha256: String, val notes: String)

/** The release APK name carries Android's monotonic versionCode, never a lexical version comparison. */
internal fun parseRelease(json: String): AppRelease? {
    val root = JSONObject(json)
    if (root.optBoolean("draft") || root.optBoolean("prerelease")) return null
    val version = root.getString("tag_name").removePrefix("v")
    require(version.matches(Regex("[0-9]+\\.[0-9]+(?:\\.[0-9]+)?"))) { "发布版本格式无效" }
    val assets = root.getJSONArray("assets")
    val pattern = Regex("yanga-${Regex.escape(version)}-([1-9][0-9]*)-release\\.apk")
    val matches = (0 until assets.length()).map { assets.getJSONObject(it) }
        .filter { pattern.matches(it.getString("name")) }
    require(matches.size == 1) { "该版本缺少唯一的正式版 APK" }
    val asset = matches.single()
    val code = pattern.matchEntire(asset.getString("name"))!!.groupValues[1].toLong()
    require(code <= 2100000000L) { "版本号超出范围" }
    val url = asset.getString("browser_download_url")
    require(url.startsWith("$PROJECT_URL/releases/download/v$version/")) { "更新下载地址无效" }
    val digest = asset.optString("digest").removePrefix("sha256:")
    require(digest.matches(Regex("[a-fA-F0-9]{64}"))) { "发布文件缺少 SHA-256 校验值，请稍后重试" }
    val size = asset.getLong("size")
    require(size in 1..300_000_000) { "更新文件大小无效" }
    return AppRelease(version, code, url, size, digest, root.optString("body").take(12000))
}

class GitHubUpdates {
    private fun connect(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15000
        readTimeout = 30000
        setRequestProperty("User-Agent", "Yanga-Android")
        setRequestProperty("Accept", "application/vnd.github+json")
    }

    suspend fun latest(): AppRelease? = withContext(Dispatchers.IO) {
        val connection = connect("https://api.github.com/repos/ZegWe/YANGA3/releases/latest")
        try {
            when (val status = connection.responseCode) {
                404 -> null
                200 -> connection.inputStream.bufferedReader().use { reader ->
                    val text = reader.readText()
                    parseRelease(text)
                }
                403, 429 -> error("GitHub 请求受限，请稍后重试")
                else -> error("检查更新失败（HTTP $status）")
            }
        } finally { connection.disconnect() }
    }

    suspend fun download(release: AppRelease, directory: File, progress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        directory.mkdirs()
        val partial = File(directory, "update.part")
        val target = File(directory, "update.apk")
        target.delete()
        val connection = connect(release.url)
        try {
            require(connection.responseCode == 200) { "下载失败（HTTP ${connection.responseCode}）" }
            val digest = MessageDigest.getInstance("SHA-256")
            var received = 0L
            connection.inputStream.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(32768)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        received += count
                        require(received <= release.size) { "下载文件大小与发布信息不符" }
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        progress((received * 100 / release.size).toInt())
                    }
                }
            }
            require(received == release.size) { "下载不完整，请重试" }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            require(actual.equals(release.sha256, ignoreCase = true)) { "文件校验失败，请重新下载" }
            check(partial.renameTo(target)) { "无法保存更新文件" }
            target
        } finally {
            connection.disconnect()
            partial.delete()
        }
    }
}
