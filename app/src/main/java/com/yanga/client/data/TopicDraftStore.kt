package com.yanga.client.data

import android.content.Context
import android.util.Base64
import com.yanga.client.api.TopicAttachment
import com.yanga.client.api.TopicPostOptions
import com.yanga.client.api.TopicUploadOptions
import java.io.*

data class TopicDraft(
  val title: String, val content: String, val attachments: List<TopicAttachment>,
  val owner: String?, val options: TopicPostOptions, val uploadOptions: TopicUploadOptions,
) : Serializable

/** Private, account/board-scoped drafts, separate from login preferences. */
class TopicDraftStore(context: Context, account: String, fid: Int, scope: String = "") {
  private val preferences = context.getSharedPreferences("topic_drafts", Context.MODE_PRIVATE)
  private val key = "v1:$account:$fid$scope"
  fun saveReplyTarget(target: com.yanga.client.api.ReplyTarget?) {
    if (target == null) return
    val json = org.json.JSONObject().put("tid", target.tid).put("pid", target.pid)
      .put("label", target.label).put("quote", target.quote).put("mode", target.mode.name)
    preferences.edit().putString("$key:target", json.toString()).apply()
  }
  fun loadReplyTarget(): com.yanga.client.api.ReplyTarget? = runCatching {
    val json = org.json.JSONObject(preferences.getString("$key:target", null) ?: return null)
    com.yanga.client.api.ReplyTarget(json.getString("tid"), json.getString("pid"), json.getString("label"),
      json.getString("quote"), com.yanga.client.api.ReplyMode.valueOf(json.getString("mode")))
  }.getOrNull()
  fun load(): TopicDraft? = runCatching {
    val encoded = preferences.getString(key, null) ?: return null
    ObjectInputStream(ByteArrayInputStream(Base64.decode(encoded, Base64.NO_WRAP))).use { it.readObject() as? TopicDraft }
  }.getOrNull()
  fun save(draft: TopicDraft) {
    val bytes = ByteArrayOutputStream().use { buffer ->
      ObjectOutputStream(buffer).use { it.writeObject(draft) }
      buffer.toByteArray()
    }
    preferences.edit().putString(key, Base64.encodeToString(bytes, Base64.NO_WRAP)).apply()
  }
  fun clear() { preferences.edit().remove(key).remove("$key:target").apply() }
}
