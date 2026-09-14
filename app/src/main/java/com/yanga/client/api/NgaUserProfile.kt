package com.yanga.client.api

data class NgaUserProfile(
  val uid: String,
  val username: String,
  val avatarUrl: String?,
  val signature: String,
  val postCount: Int?,
  val registeredAt: Long?,
)

object NgaUserProfileParser {
  fun parse(raw: String): NgaUserProfile {
    val root = ngaJsonRoot(raw)
    root.opt("error")?.let { throw NgaApiException(it.toString()) }
    val data = root.objectValue("data") ?: root
    val user = data.objectValue("0") ?: data.arrayValue("data")?.optJSONObject(0) ?: root.arrayValue("data")?.optJSONObject(0) ?: data
    val uid = user.stringValue("uid")
    val name = user.stringValue("username", "name")
    if (uid.toLongOrNull()?.let { it > 0 } != true || name.isBlank()) throw NgaApiException("无法读取用户资料")
    return NgaUserProfile(uid, NgaDisplayText.singleLine(name),
      NgaAvatarUrls.resolveUserAvatar(user.nullableStringValue("avatar"), uid, user.nullableStringValue("memberid", "gid")),
      user.stringValue("sign", "signature"), user.nullableIntValue("postnum", "posts"), user.nullableLongValue("regdate"))
  }
}
