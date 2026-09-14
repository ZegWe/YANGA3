package com.yanga.client.data

import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class CheckInStore(private val preferences: SharedPreferences? = null) {
  private val days = mutableMapOf<String, String>()
  fun isCheckedIn(uid: String, now: Long): Boolean = (preferences?.getString(uid, null) ?: days[uid]) == day(now)
  fun record(uid: String, now: Long) {
    val date = day(now)
    days[uid] = date
    preferences?.edit()?.putString(uid, date)?.apply()
  }
  private fun day(now: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
    timeZone = TimeZone.getTimeZone("Asia/Shanghai")
  }.format(Date(now))
}
