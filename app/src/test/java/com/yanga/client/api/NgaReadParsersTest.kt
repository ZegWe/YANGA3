package com.yanga.client.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NgaReadParsersTest {
  @Test
  fun topicListParserReadsPublicTopicListFixture() {
    val topics = NgaTopicListParser.parse(fixture("topic_list_public.json"))

    assertEquals(2, topics.page)
    assertTrue(topics.hasNextPage)
    assertEquals(2, topics.topics.size)
    assertEquals(
      NgaTopicSummary(
        topicId = "1001",
        boardId = "7",
        boardName = "议事厅",
        title = "公开主题",
        authorId = "42",
        authorName = "作者A",
        authorAvatarUrl = "https://img4.nga.178.com/avatars/2002/039/003/000/12345_67890.jpg?1234567890",
        replyCount = 12,
        lastPostAt = 1770001111L,
        isFavorited = true,
      ),
      topics.topics[0],
    )
    assertEquals("1002", topics.topics[1].topicId)
    assertEquals("8", topics.topics[1].boardId)
    assertEquals("酒馆", topics.topics[1].boardName)
    assertFalse(topics.topics[1].isFavorited)
    assertTrue(topics.subBoards.isEmpty())
  }

  @Test
  fun topicListParserReadsSubBoardsFromForumMetadata() {
    val topics = NgaTopicListParser.parse(fixture("topic_list_with_sub_boards.json"))

    assertEquals(3, topics.subBoards.size)
    assertEquals(
      listOf(
        NgaSubBoard(id = "448", name = "同人作品", valueId = "448", subscribeId = "21812723"),
        NgaSubBoard(id = "t7348283", name = "招募 求职 师徒", valueId = "7348283", subscribeId = "7348283"),
        NgaSubBoard(id = "517", name = "衍生讨论", valueId = "517", subscribeId = "14904011"),
      ),
      topics.subBoards,
    )
  }

  @Test
  fun boardCategoryParserReadsRemoteBoardCategoryFixture() {
    val categories = NgaBoardCategoryParser.parse(fixture("remote_board_categories.json"))

    assertEquals(2, categories.size)
    assertEquals("general", categories[0].id)
    assertEquals("综合", categories[0].name)
    assertEquals(
      NgaBoardSummary(
        boardId = "7",
        name = "议事厅",
        description = "公共讨论",
        todayTopicCount = 9,
        unreadCount = 2,
        isSubscribed = true,
        iconUrl = "https://img4.nga.178.com/ngabbs/nga_classic/f/app/7.png",
      ),
      categories[0].boards.single(),
    )
    assertEquals("2", categories[1].id)
    assertEquals("游戏", categories[1].name)
    assertEquals("10", categories[1].boards.single().boardId)
  }

  @Test
  fun boardCategoryParserReadsRemoteResultGroupsAsBoardCategories() {
    val sections =
      NgaBoardCategoryParser.parseSections(
        """
        {
          "code": 0,
          "result": [
            {
              "id": "wow",
              "name": "魔兽世界",
              "groups": [
                {
                  "id": "classic",
                  "name": "经典旧世",
                  "forums": [
                    {
                      "id": 310,
                      "name": "怀旧服讨论",
                      "stid": 321
                    }
                  ]
                }
              ]
            }
          ]
        }
        """.trimIndent(),
      )

    assertEquals(1, sections.size)
    assertEquals("wow", sections.single().id)
    assertEquals("魔兽世界", sections.single().name)
    assertEquals("classic", sections.single().groups.single().id)
    assertEquals("经典旧世", sections.single().groups.single().name)
    assertEquals("310", sections.single().groups.single().boards.single().boardId)
    assertEquals("怀旧服讨论", sections.single().groups.single().boards.single().name)
    assertEquals(
      "https://img4.nga.178.com/proxy/cache_attach/ficon/321v.png",
      sections.single().groups.single().boards.single().iconUrl,
    )
  }

  @Test
  fun boardCategoryParserParseSectionsSupportsLegacyCategoryShape() {
    val sections = NgaBoardCategoryParser.parseSections(fixture("remote_board_categories.json"))

    assertEquals(2, sections.size)
    assertEquals("general", sections[0].id)
    assertEquals("综合", sections[0].name)
    assertEquals(1, sections[0].groups.size)
    assertEquals("general", sections[0].groups[0].id)
    assertEquals("综合", sections[0].groups[0].name)
    assertEquals("7", sections[0].groups[0].boards[0].boardId)
  }

  @Test
  fun messageParserReadsMessageListFixture() {
    val messages = NgaMessageParser.parseList(fixture("message_list.json"))

    assertEquals(2, messages.size)
    assertEquals(
      NgaMessageSummary(
        messageId = "501",
        contactId = "42",
        contactName = "张三",
        subject = "问候",
        preview = "你好，最近如何",
        lastUpdatedAt = 1770003333L,
        unreadCount = 2,
      ),
      messages[0],
    )
    assertEquals("502", messages[1].messageId)
    assertEquals("李四", messages[1].contactName)
  }

  @Test
  fun accountParserReadsNotificationsAndProfileCountersFixtures() {
    val notifications = NgaAccountParser.parseNotifications(fixture("notifications.json"))
    val counters = NgaAccountParser.parseProfileCounters(fixture("profile.json"))

    assertEquals(2, notifications.size)
    assertEquals(
      NgaNotificationSummary(
        id = "701",
        title = "有人回复了你",
        preview = "公开主题 有新回复",
        createdAt = 1770005555L,
        unreadCount = 1,
      ),
      notifications[0],
    )
    assertEquals("702", notifications[1].id)
    assertEquals(
      NgaProfileCounters(
        favoriteTopics = 5,
        subscribedBoards = 3,
        unreadNotifications = 2,
        unreadMessages = 1,
      ),
      counters,
    )
  }

  private fun fixture(name: String): String =
    checkNotNull(javaClass.classLoader?.getResource("fixtures/nga/$name")) {
      "Missing fixture $name"
    }.readText()
}
