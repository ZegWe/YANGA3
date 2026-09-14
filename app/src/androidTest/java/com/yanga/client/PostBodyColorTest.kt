package com.yanga.client
import androidx.test.platform.app.InstrumentationRegistry
import com.yanga.client.api.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import kotlinx.coroutines.runBlocking
class PostBodyColorTest {
 @Test fun currentModeratorPostIsBlue() = runBlocking {
  assumeTrue(InstrumentationRegistry.getArguments().getString("liveNgaColors") == "true")
  val c=InstrumentationRegistry.getInstrumentation().targetContext
  val session=LoginSessionStore.load(c)!!
  val app=c.applicationContext as YangaApplication
  val thread=app.repository.loadThreadByAuthor(com.yanga.client.data.LoginSessionData(session.username,session.uid,session.cookie),"44191387",1,"205511").getOrThrow()
  assertEquals("blue",thread.posts.first().bodyColor)
 }
}
