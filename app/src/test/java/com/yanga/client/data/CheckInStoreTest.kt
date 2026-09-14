package com.yanga.client.data
import org.junit.Assert.*
import org.junit.Test
class CheckInStoreTest {
  @Test fun separatesAccountsAndExpiresAtChinaMidnight() {
    val store = CheckInStore()
    val beforeMidnight = 86_400_000L - 8 * 3_600_000L - 1
    assertFalse(store.isCheckedIn("42", beforeMidnight))
    store.record("42", beforeMidnight)
    assertTrue(store.isCheckedIn("42", beforeMidnight))
    assertFalse(store.isCheckedIn("43", beforeMidnight))
    assertFalse(store.isCheckedIn("42", beforeMidnight + 1))
  }
}
