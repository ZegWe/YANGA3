package com.yanga.client.ui

import com.yanga.client.data.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private val session = LoginSessionData("Test", "1", "cookie")
  @Before fun setUp() = Dispatchers.setMain(dispatcher)
  @After fun tearDown() = Dispatchers.resetMain()

  private class Repository : NgaReadOnlyRepository by DefaultNgaReadOnlyRepository() {
    var query: suspend (LoginSessionData?) -> Result<Boolean> = { Result.success(true) }
    var action: suspend () -> Result<String> = { Result.success("签到成功") }
    var queries = 0
    var actions = 0
    override suspend fun loadCheckInStatus(session: LoginSessionData?): Result<Boolean> {
      queries++
      return query(session)
    }
    override suspend fun checkIn(session: LoginSessionData?): Result<String> {
      actions++
      return action()
    }
    override suspend fun loadProfile(session: LoginSessionData?): Result<ProfileReadData> = Result.failure(IllegalStateException("offline"))
  }

  @Test fun everyRefreshQueriesServerAndFailureIsUnknown() = runTest(dispatcher) {
    val repo = Repository()
    val vm = ProfileViewModel(repo)
    vm.refresh(session)
    assertNull(vm.state.value.checkedIn)
    assertTrue(vm.state.value.checkInStatusLoading)
    advanceUntilIdle()
    assertEquals(true, vm.state.value.checkedIn)
    repo.query = { Result.success(false) }
    vm.refresh(session)
    advanceUntilIdle()
    assertEquals(false, vm.state.value.checkedIn)
    repo.query = { Result.failure(IllegalStateException("offline")) }
    vm.refresh(session)
    advanceUntilIdle()
    assertNull(vm.state.value.checkedIn)
    assertNotNull(vm.state.value.checkInStatusError)
    assertFalse(vm.state.value.checkInStatusLoading)
    assertEquals(3, repo.queries)
    assertEquals(0, repo.actions)
  }

  @Test fun oldAccountResponseCannotOverwriteNewAccountOrLogout() = runTest(dispatcher) {
    val old = CompletableDeferred<Result<Boolean>>()
    val repo = Repository().apply { query = { if (it?.uid == "1") old.await() else Result.success(false) } }
    val vm = ProfileViewModel(repo)
    vm.refresh(session)
    runCurrent()
    vm.refresh(session.copy(uid = "2"))
    runCurrent()
    old.complete(Result.success(true))
    advanceUntilIdle()
    assertEquals(false, vm.state.value.checkedIn)
    vm.refresh(null)
    advanceUntilIdle()
    assertNull(vm.state.value.checkedIn)
    assertEquals(2, repo.queries)
  }

  @Test fun checkInRequeriesAndDiscardsEarlierStatusResponse() = runTest(dispatcher) {
    val old = CompletableDeferred<Result<Boolean>>()
    val action = CompletableDeferred<Result<String>>()
    val repo = Repository().apply {
      query = { if (queries == 1) old.await() else Result.success(true) }
      this.action = { action.await() }
    }
    val vm = ProfileViewModel(repo)
    vm.refresh(session)
    runCurrent()
    vm.checkIn(session)
    vm.checkIn(session)
    runCurrent()
    vm.refresh(session) // Profile refresh must not allow a second concurrent check-in.
    vm.checkIn(session)
    runCurrent()
    assertEquals(1, repo.actions)
    action.complete(Result.success("今天已经签到"))
    runCurrent()
    assertEquals(true, vm.state.value.checkedIn)
    old.complete(Result.success(false))
    advanceUntilIdle()
    assertEquals(true, vm.state.value.checkedIn)
    assertEquals(2, repo.queries)
    assertEquals("今天已经签到", vm.state.value.checkInMessage)
  }

  @Test fun successfulActionDoesNotReplaceFailedStatusQueryWithLocalSuccess() = runTest(dispatcher) {
    val repo = Repository().apply { query = { Result.failure(IllegalStateException("offline")) } }
    val vm = ProfileViewModel(repo)
    vm.checkIn(session)
    advanceUntilIdle()
    assertNull(vm.state.value.checkedIn)
    assertNotNull(vm.state.value.checkInStatusError)
    assertEquals("签到成功", vm.state.value.checkInMessage)
  }

  @Test fun logoutDiscardsPendingCheckInAndDoesNotQueryOldAccount() = runTest(dispatcher) {
    val action = CompletableDeferred<Result<String>>()
    val repo = Repository().apply { this.action = { action.await() } }
    val vm = ProfileViewModel(repo)
    vm.checkIn(session)
    runCurrent()
    vm.refresh(null)
    action.complete(Result.success("签到成功"))
    advanceUntilIdle()
    assertNull(vm.state.value.checkedIn)
    assertNull(vm.state.value.checkInMessage)
    assertFalse(vm.state.value.checkInRunning)
    assertEquals(0, repo.queries)
  }

  @Test fun endpointChangeDiscardsPendingStatus() = runTest(dispatcher) {
    val old = CompletableDeferred<Result<Boolean>>()
    val repo = Repository().apply { query = { old.await() } }
    val vm = ProfileViewModel(repo)
    vm.refresh(session)
    runCurrent()
    vm.setEndpoint("https://ngabbs.com")
    repo.query = { Result.success(false) }
    vm.refresh(session)
    runCurrent()
    old.complete(Result.success(true))
    advanceUntilIdle()
    assertEquals(false, vm.state.value.checkedIn)
  }
}
