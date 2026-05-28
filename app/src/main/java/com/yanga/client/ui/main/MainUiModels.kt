package com.yanga.client.ui.main

enum class MainTab(val label: String) {
  Home("Home"),
  Boards("Boards"),
  Messages("Messages"),
  Profile("Profile"),
}

data class BoardPreview(
  val name: String,
  val metadata: String,
  val marker: String,
  val badge: String? = null,
)

data class TopicPreview(
  val title: String,
  val board: String,
  val replies: String,
  val lastActive: String,
  val authorInitial: String,
)

data class MessagePreview(
  val contact: String,
  val preview: String,
  val time: String,
  val badge: String? = null,
)

data class SettingsPreview(
  val icon: String,
  val title: String,
  val subtitle: String,
  val badge: String? = null,
)

internal val favoriteBoards =
  listOf(
    BoardPreview("艾泽拉斯议事厅", "最近活跃 · 12 分钟前", "战", "128"),
    BoardPreview("游戏综合讨论", "今日 42 个主题", "游", "42"),
    BoardPreview("二次元国家地理", "收藏板块", "星"),
    BoardPreview("程序员专版", "今日 16 个主题", "码"),
  )

internal val activeTopics =
  listOf(
    TopicPreview("关于新版客户端首页信息密度的讨论", "水区", "178 回复", "3 分钟前", "N"),
    TopicPreview("MD3 动态色在论坛阅读场景下是否适合", "开发", "46 回复", "12 分钟前", "Y"),
    TopicPreview("长帖阅读页分页与楼层跳转方案", "客户端", "91 回复", "28 分钟前", "C"),
  )

internal val subscribedBoards =
  listOf(
    BoardPreview("艾泽拉斯议事厅", "fid 7 · 128 条新回复", "战", "128"),
    BoardPreview("游戏综合讨论", "今日 42 主题 · 已订阅", "游", "42"),
    BoardPreview("程序员专版", "技术讨论 · 已订阅", "码"),
  )

internal val forumCategories =
  listOf(
    BoardPreview("游戏专区", "32 个板块 · 魔兽 / 手游 / 主机", "游"),
    BoardPreview("网事杂谈", "18 个板块 · 生活 / 情感 / 科技", "茶"),
    BoardPreview("创作与技术", "9 个板块 · 开发 / 设计 / 硬件", "创"),
  )

internal val privateMessages =
  listOf(
    MessagePreview("夜航船", "你之前说的 Compose 阅读页分页方案，我觉得可以按楼层锚点做...", "刚刚", "2"),
    MessagePreview("Moderator", "关于你收藏板块的同步问题，可能和 Cookie 过期有关。", "12:40"),
    MessagePreview("小透明", "收到，我晚点整理一下测试账号能复现的步骤。", "昨天"),
  )

internal val notificationRows =
  listOf(
    SettingsPreview("回", "Reply alerts", "8 条未读", "8"),
    SettingsPreview("星", "Favorite topic updates", "4 个主题有新回复", "4"),
  )

internal val settingsRows =
  listOf(
    SettingsPreview("阅", "Reading and appearance", "字体、主题、图片加载"),
    SettingsPreview("缓", "Cache and history", "最近阅读、离线缓存"),
    SettingsPreview("屏", "Block words", "过滤内容和用户"),
  )
