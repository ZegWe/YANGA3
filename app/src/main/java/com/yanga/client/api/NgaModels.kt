package com.yanga.client.api

enum class NgaHttpMethod {
  GET,
  POST,
}

data class NgaSession(
  val baseUrl: String = NgaDomains.BBS_NGA_CN,
  val cookie: String? = null,
  val userAgent: String = "Yanga Android",
) {
  val normalizedBaseUrl: String = baseUrl.trimEnd('/')
}

object NgaDomains {
  const val BBS_NGACN_CC = "https://bbs.ngacn.cc"
  const val BBS_NGA_CN = "https://bbs.nga.cn"
  const val NGA_178 = "https://nga.178.com"
  const val NGA_DONEWS = "https://nga.donews.com"
  const val NGABBS = "https://ngabbs.com"

  val supported = listOf(BBS_NGACN_CC, BBS_NGA_CN, NGA_178, NGA_DONEWS, NGABBS)
}

data class NgaRequest(
  val method: NgaHttpMethod,
  val url: String,
  val query: Map<String, String> = emptyMap(),
  val headers: Map<String, String> = emptyMap(),
  val body: NgaFormBody = NgaFormBody(),
) {
  val bodyMap: Map<String, String>
    get() = body.asMap()
}

data class NgaHttpResponse(
  val code: Int,
  val text: String,
  val isSuccessful: Boolean = code in 200..399,
)

class NgaApiException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

fun NgaRequest.fullUrl(): String {
  if (query.isEmpty()) return url
  return url + "?" + query.entries.joinToString("&") { (key, value) ->
    if (value.isEmpty()) key else "$key=$value"
  }
}

data class NgaFormBody(
  val fields: List<String> = emptyList(),
) {
  fun asMap(): Map<String, String> =
    fields.mapNotNull { field ->
      val index = field.indexOf('=')
      if (index < 0) null else field.substring(0, index) to field.substring(index + 1)
    }.toMap()
}

class MissingNgaSessionException : IllegalStateException("NGA session cookie is required")

data class NgaLoginCookies(
  val uid: String,
  val cid: String,
  val username: String,
) {
  companion object {
    private const val TAG_UID = "ngaPassportUid"
    private const val TAG_CID = "ngaPassportCid"
    private const val TAG_USERNAME = "ngaPassportUrlencodedUname"

    fun parse(cookies: String): NgaLoginCookies? {
      val cookieMap = cookies
        .split(';')
        .map { it.trim() }
        .mapNotNull {
          val index = it.indexOf('=')
          if (index < 0) null else it.substring(0, index) to it.substring(index + 1)
        }
        .toMap()

      val uid = cookieMap[TAG_UID].orEmpty()
      val cid = cookieMap[TAG_CID].orEmpty()
      val username = cookieMap[TAG_USERNAME]
        ?.let { NgaEncoding.urlDecodeGbk(NgaEncoding.urlDecodeGbk(it)) }
        .orEmpty()

      return if (uid.isBlank() || cid.isBlank() || username.isBlank()) {
        null
      } else {
        NgaLoginCookies(uid = uid, cid = cid, username = username)
      }
    }
  }
}
