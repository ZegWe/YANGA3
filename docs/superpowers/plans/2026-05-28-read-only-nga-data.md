# Read-Only NGA Data Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace current hard-coded UI fake data with real read-only NGA API loading for Home, Boards, Messages, and Profile.

**Architecture:** Keep `NgaApi` as the request builder, add a production read-only transport, add tolerant parsers around normalized NGA JSON-like responses, then expose parsed data through a repository consumed by main UI state. UI must show explicit loading, error, empty, and login-required states instead of silently falling back to fake data.

**Tech Stack:** Android/Kotlin, Jetpack Compose, coroutines, `org.json`, JUnit4, Android Compose UI tests, existing Gradle Android test tasks.

---

## Task Order

1. Shared contracts and load states.
2. Parsers in parallel: topic list, remote board categories, message list, notifications/profile.
3. Production read-only HTTP transport.
4. Repository orchestration.
5. UI mapper and ViewModel state.
6. Home/Boards rendering from state.
7. Messages/Profile rendering from state.
8. MainScreen production wiring.
9. Env-gated live parser smoke coverage and final verification.

## File Boundaries

- API models/parsers: `app/src/main/java/com/yanga/client/api/*`
- Repository/data models: `app/src/main/java/com/yanga/client/data/*`
- UI state/mapper/ViewModel: `app/src/main/java/com/yanga/client/ui/main/*`
- Parser fixtures: `app/src/test/resources/fixtures/nga/*`
- Unit tests: `app/src/test/java/com/yanga/client/**`
- Compose tests: `app/src/androidTest/java/com/yanga/client/ui/main/MainScreenTest.kt`

## Parallelization

Task 1 is the sequential gate because it defines shared names. After Task 1:

- Parser tasks can run in parallel if only one worker owns `NgaJson.kt`; other parser workers should use existing helpers.
- Transport can run independently from parsers.
- Repository can run after parser model names and transport contracts exist.
- Home/Boards and Messages/Profile UI rendering can run in parallel because they touch separate screen files.

## Acceptance Checklist

- Home loads `topicList()` into Active discussions and remote categories into board previews.
- Boards loads `remoteBoardCategories()` and shows login-required or empty state for subscribed boards until a real authenticated source exists.
- Messages shows login prompt when logged out and `messageList()` data when logged in.
- Profile shows login prompt when logged out and `notifications()` / `profile(uid)` data when logged in.
- No screen silently falls back to fake globals.
- Loading, error, empty, and login-required states are visible.
- Parser tests use fixtures and tolerate object/array collections plus numeric/string scalar values.
- Live tests remain optional and environment-gated.
- Every accepted task ends in a commit.

## Verification Commands

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

`connectedDebugAndroidTest` may be blocked when no emulator/device is connected; record that separately from compile/unit-test failures.
