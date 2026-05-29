package com.yanga.client.data

import com.yanga.client.api.NgaBoardSection
import com.yanga.client.api.NgaBoardSummary
import com.yanga.client.api.NgaMessageSummary
import com.yanga.client.api.NgaNotificationSummary
import com.yanga.client.api.NgaProfileCounters
import com.yanga.client.api.NgaTopicSummary

data class LoginSessionData(
  val username: String,
  val uid: String,
  val cookie: String,
)

data class HomeReadData(
  val boards: List<NgaBoardSummary>,
  val activeTopics: List<NgaTopicSummary>,
)

data class BoardsReadData(
  val subscribedBoards: List<NgaBoardSummary>,
  val remoteSections: List<NgaBoardSection>,
)

data class MessagesReadData(
  val messages: List<NgaMessageSummary>,
)

data class ProfileReadData(
  val counters: NgaProfileCounters,
  val notifications: List<NgaNotificationSummary>,
)
