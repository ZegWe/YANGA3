package com.yanga.client.api

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.Charset
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

data class NgaPasswordLoginResult(
  val session: NgaLoginCookies,
  val cookie: String,
)

class NgaPasswordLoginException(message: String) : Exception(message)

class NgaPasswordLoginClient {
  fun login(name: String, password: String, captchaId: String, captcha: String, pageId: String): NgaPasswordLoginResult {
    val encryptedPassword = encryptPassword(password.trim())
    val body = linkedMapOf(
      "__lib" to "login",
      "__output" to "1",
      "app_id" to "5004",
      "device" to "",
      "trackid" to "",
      "__act" to "login",
      "__ngaClientChecksum" to "",
      "name" to name.trim(),
      "type" to inferLoginType(name),
      "password" to encryptedPassword,
      "rid" to captchaId,
      "captcha" to captcha.trim(),
      "prid" to pageId,
      "__inchst" to "UTF-8",
    ).toFormBody()

    val connection = (URL("https://bbs.nga.cn/nuke.php").openConnection() as HttpURLConnection).apply {
      requestMethod = "POST"
      connectTimeout = 15_000
      readTimeout = 15_000
      doOutput = true
      setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
      setRequestProperty("Referer", "https://bbs.nga.cn/nuke.php?__lib=login&__act=login_ui")
      setRequestProperty("User-Agent", "Yanga Android")
      setRequestProperty("X-User-Agent", "Nga_Official")
    }

    connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

    val response = connection.readText()
    val json = JSONObject(NgaResponseNormalizer.normalize(response))
    json.opt("error")?.let { error -> throw NgaPasswordLoginException(error.firstMessage()) }

    val account = json.opt("data").findAccountObject()
    val uid = account?.optString("uid").orEmpty()
    val cid = account?.optString("token").orEmpty()
      .ifBlank { account?.optString("cid").orEmpty() }
    val username = account?.optString("username").orEmpty()
      .ifBlank { account?.optString("name").orEmpty() }
      .ifBlank { name.trim() }

    if (uid.isBlank() || cid.isBlank()) {
      throw NgaPasswordLoginException("登录响应缺少 uid 或 token")
    }

    val encodedUsername = NgaEncoding.urlEncodeGbk(NgaEncoding.urlEncodeGbk(username))
    val cookie = "ngaPassportUid=$uid; ngaPassportCid=$cid; ngaPassportUrlencodedUname=$encodedUsername"
    return NgaPasswordLoginResult(
      session = NgaLoginCookies(uid = uid, cid = cid, username = username),
      cookie = cookie,
    )
  }

  private fun encryptPassword(password: String): String {
    val keyBytes = Base64.decode(PUBLIC_KEY, Base64.DEFAULT)
    val publicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(keyBytes))
    val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
    cipher.init(Cipher.ENCRYPT_MODE, publicKey)
    return Base64.encodeToString(cipher.doFinal(password.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
  }

  private fun inferLoginType(name: String): String =
    when {
      name.contains('@') -> "mail"
      name.all(Char::isDigit) && name.length <= 9 -> "id"
      name.matches(Regex("""\d+[- ]\d+""")) -> "phone"
      else -> ""
    }

  private fun Map<String, String>.toFormBody(): String =
    entries.joinToString("&") { (key, value) ->
      "${key.urlEncode()}=${value.urlEncode()}"
    }

  private fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())

  private fun Any.firstMessage(): String =
    when (this) {
      is JSONArray -> optString(0, "登录失败")
      is JSONObject -> optString("0", "登录失败")
      else -> toString().ifBlank { "登录失败" }
    }

  private fun Any?.findAccountObject(): JSONObject? =
    when (this) {
      is JSONArray -> optJSONObject(3) ?: optJSONObject(1)
      is JSONObject -> optJSONObject("3") ?: optJSONObject("1")
      else -> null
    }

  private fun HttpURLConnection.readText(): String {
    val stream = if (responseCode in 200..399) inputStream else errorStream
    return BufferedReader(InputStreamReader(stream, Charset.forName("GBK"))).use { it.readText() }
  }

  private companion object {
    const val PUBLIC_KEY =
      "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyKzZWDimCN1OCprqWUhF" +
        "UPhcwxDE62/BFVP6LtQHJu+65dm4YNmDvzitmcfaXW9YbhXnd4oP7j+6vpcgJQ+p" +
        "3ucySo1ZnqO0Bb2JKEtxpCmxe7IYXhFEkJqHpFYBTiAxQz2n2mX4JZy/ehBUSMjz" +
        "gzd0NdG6Ai1C42oCzYltUOjNWZUNHn1nqpElSWHnUWqkdN8+5ISP/ZMKiQdFANkE" +
        "qDGw3/34qyF+E/hVgrGF4/CcWNP/LJCdB6DYtx7VPlQZF0tP1s+q/++rC4rQ2wmV" +
        "l2V8zGh1j7ojZbt62hVjy6byK1E/2XYo97ZtL4KDW7F5jJMvSDRFR7901UR8hCdf" +
        "4wIDAQAB"
  }
}
