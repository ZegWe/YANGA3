package com.yanga.client.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun Long.toUiDateTimeString(): String {
  val date = Date(if (this < 10000000000L) this * 1000 else this)
  return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(date)
}

internal fun Long.toUiEditDateString(): String {
  val date = Date(if (this < 10000000000L) this * 1000 else this)
  return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date)
}
