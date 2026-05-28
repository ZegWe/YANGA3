package com.yanga.client.data

import com.yanga.client.api.NgaBoardCategory
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
  val boards: List<NgaBoardCategory>,
  val activeTopics: List<NgaTopicSummary>,
)

data class BoardsReadData(
  val subscribedBoards: List<NgaBoardCategory>,
  val remoteCategories: List<NgaBoardCategory>,
)

data class MessagesReadData(
  val messages: List<NgaMessageSummary>,
)

data class ProfileReadData(
  val counters: NgaProfileCounters,
  val notifications: List<NgaNotificationSummary>,
)
