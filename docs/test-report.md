# Test Report

## Summary

Automated test contracts were added for the API request layer:

```text
app/src/test/java/com/yanga/client/api/NgaApiTest.kt
```

The tests are designed to verify request construction and response cleanup without calling live NGA servers. The latest expansion covers the logged-in/session-cookie case for every public `NgaApi` request builder, including account-mutating endpoints by inspecting request objects only.

## Test Coverage Added

| Test area | Covered by |
| --- | --- |
| Topic-list query construction | `topicListBuildsForumQuery` |
| Thread-detail query construction | `articleReadBuildsThreadDetailQuery` |
| Common headers | `commonHeadersIncludeSessionCookieAndOfficialUserAgent` |
| GBK post body encoding | `postBodyUsesGbkEncoding` |
| WebView login cookie parsing | `loginCookieParserDecodesUsernameTwice` |
| Wrapped JS response cleanup | `responseNormalizerStripsNgaJavaScriptWrapper` |
| Missing-session guard | `authenticatedRequestFailsWithoutCookie` |
| Private message GBK fields | `messagePostUsesGbkEncodedRecipientsSubjectAndContent` |
| Block-word payload format | `blockWordUpdateBuildsExpectedPayload` |
| Static image URL templates | `staticImageUrlsMatchReferenceTemplates` |
| Topic-list search/favorite/recommend variants | `topicListBuildsSearchFavoriteAndRecommendVariants` |
| Thread-detail PID lookup | `articleReadBuildsPidQuery` |
| Board search and remote categories | `boardAndRemoteCategoryRequestsMatchReferenceEndpoints` |
| Post preflight and submit requests | `topicPostInfoBuildsPreflightQuery`, `topicPostBuildsSubmitBodyWithOptionalFields` |
| Attachment metadata | `topicPostBodyIncludesAttachmentsAndStidWhenPresent`, `attachmentUploadMetadataBuildsFixedUploadEndpointAndFields` |
| Comment / favorite / like / report / notification / check-in requests | `commentPostBuildsReplyCommentBody`, `favoriteLikeReportNotificationAndCheckInRequestsMatchReference` |
| Profile / signature / block words | `profileSignatureAndBlockWordRequestsMatchReference` |
| Message list/read/send | `messageListAndReadBuildExpectedQueries`, `messagePostUsesGbkEncodedRecipientsSubjectAndContent` |
| Sub-board options | `subBoardOptionMapsSubscribeActionsByType` |
| Vote / settle | `voteBuildsVoteAndSettleQueries` |
| Avatar upload/change metadata | `avatarUploadAndChangeRequestsMatchLegacyEndpoints` |
| Thread read response parsing | `threadParserParsesReadResponsePostsAndUserMapping` |
| Live read-only API smoke tests | `NgaReadOnlyApiLiveTest` |

## Live Read-Only API Tests

`NgaReadOnlyApiLiveTest` can run real network smoke tests after a login cookie is available. It only executes `GET` requests and asserts this before opening the connection, so mutating endpoints such as post, check-in, favorite, like, signature, block-word update, vote, and avatar change are excluded.

Required environment variable:

```powershell
$env:NGA_COOKIE='ngaPassportUid=...; ngaPassportCid=...; ngaPassportUrlencodedUname=...'
```

Optional environment variables:

```powershell
$env:NGA_UID='<current user uid>'
$env:NGA_TEST_FID='7'
$env:NGA_TEST_TID='<known readable thread id>'
$env:NGA_BASE_URL='https://bbs.nga.cn'
$env:NGA_USER_AGENT='Yanga Android LiveTest'
```

Covered live read-only calls:

| API | State change |
| --- | --- |
| `topicList` | No |
| `boardSearch` | No |
| `remoteBoardCategories` | No |
| `articleRead` when `NGA_TEST_TID` is set | No |
| `notifications` | No |
| `messageList` | No |
| `profile` when `NGA_UID` is set | No |

The thread-detail live test now parses the `read.php` response through `NgaThreadParser` and asserts that a topic id, at least one post, and non-empty post content are present.

## Commands Run

### Unit test command

```powershell
.\gradlew.bat testDebugUnitTest
```

Initial result before provisioning a local JDK:

```text
ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
Please set the JAVA_HOME variable in your environment to match the location of your Java installation.
```

Resolution at the time: used a temporary local JDK, then reran the command with temporary `JAVA_HOME`.

```powershell
$env:JAVA_HOME='<JDK_HOME>'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest
```

Final result:

```text
> Task :app:testDebugUnitTest
BUILD SUCCESSFUL in 3m
24 actionable tasks: 24 executed
```

Status: passed.

### Android CLI project inspection

```powershell
android describe --project_dir=<PROJECT_DIR>
```

Initial result:

```text
gradlew found and is executable.
Copying init.gradle.kts to <PROJECT_DIR>\.gradle\init.gradle.kts
Running gradlew dumpModels...
Error during describe: Cannot run program "<PROJECT_DIR>\gradlew" (in directory "<PROJECT_DIR>"): CreateProcess error=193, %1 is not a valid Win32 application
```

Status: not usable on this Windows shell because Android CLI invoked the Unix `gradlew` script instead of `gradlew.bat`. Direct `gradlew.bat` verification was used instead.

### Debug build command

```powershell
$env:JAVA_HOME='<JDK_HOME>'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleDebug
```

Result:

```text
> Task :app:assembleDebug
BUILD SUCCESSFUL in 21s
36 actionable tasks: 18 executed, 18 up-to-date
```

Status: passed. Gradle emitted one non-fatal packaging note: `libandroidx.graphics.path.so` could not be stripped and was packaged as-is.

### Static search checks

```powershell
rg -n "com\.example\.yanga|MyApplicationTheme|FAKE_DATA|Hello|MainNavigation" app
```

Result: no matches.

```powershell
rg -n "com\.example\.yanga|MyApplicationTheme|FAKE_DATA|Hello|\$1|\$\*/" app\src\main\java app\src\test\java app\src\androidTest\java
```

Result: no matches.

Status: passed.

### Java runtime environment search

```powershell
where.exe java
where.exe winget
where.exe choco
```

Result:

```text
INFO: Could not find files for the given pattern(s).
```

Status: no Java runtime or common Windows package manager was available on `PATH`.

Additional broad search:

```powershell
Get-ChildItem -Path C:\ -Recurse -Filter java.exe -ErrorAction SilentlyContinue
Get-ChildItem -Path C:\ -Recurse -Directory -ErrorAction SilentlyContinue |
  Where-Object { $_.FullName -match '(?i)(jdk|jbr|java|temurin|zulu|corretto)' }
```

Result: no `java.exe` was found. Directory matches were unrelated Java source, certificate, or editor support folders, not a JDK/JRE installation.

Status: this was the original blocker. It was resolved by using a temporary JDK and `JAVA_HOME`.

## Current Verification Status

| Requirement | Evidence | Status |
| --- | --- | --- |
| API tests exist | `NgaApiTest.kt` | Complete |
| API implementation exists | `com.yanga.client.api` package | Complete |
| No live NGA calls in tests | Tests only inspect request objects/helpers | Complete |
| Gradle unit tests run | `.\gradlew.bat testDebugUnitTest` with JDK 17+ | Passed |
| Android project builds | `.\gradlew.bat assembleDebug` with JDK 17+ | Passed |

## Required Local Follow-up

To rerun verification on this machine, set `JAVA_HOME` to any JDK 17+ installation:

```powershell
$env:JAVA_HOME='<JDK_HOME>'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

No app UI organization work should start until the requested UI direction is provided.

## Latest Run Attempt

After expanding the authenticated request-construction tests and adding live read-only smoke tests, this command was attempted again:

```powershell
.\gradlew.bat testDebugUnitTest
```

Current result:

```text
ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
```

Status: blocked by missing local JDK on `PATH`/`JAVA_HOME`; no live NGA requests were made.

Later, with Android Studio's bundled JBR and the cookie read from the logged-in emulator app, the full test suite was run successfully:

```powershell
.\gradlew.bat testDebugUnitTest
```

Result:

```text
BUILD SUCCESSFUL in 2s
```

JUnit summary:

| Suite | Tests | Skipped | Failures | Errors |
| --- | ---: | ---: | ---: | ---: |
| `NgaApiTest` | 26 | 0 | 0 | 0 |
| `NgaReadOnlyApiLiveTest` | 4 | 0 | 0 | 0 |
