package com.yanga.client.data

import android.content.SharedPreferences

/** Confirmed votes only; records are isolated by account and survive process restarts. */
class PollVoteStore(private val preferences: SharedPreferences? = null) {
  private val votes = mutableMapOf<String, List<Int>>()

  @Synchronized
  fun selection(uid: String, tid: String): List<Int>? {
    val key = "$uid:$tid"
    return preferences?.getString(key, null)?.split(',')?.mapNotNull(String::toIntOrNull)
      ?: votes[key]
  }

  @Synchronized
  fun record(uid: String, tid: String, ids: List<Int>) {
    val key = "$uid:$tid"
    val selection = ids.distinct().toList()
    votes[key] = selection
    preferences?.edit()?.putString(key, selection.joinToString(","))?.apply()
  }
}
