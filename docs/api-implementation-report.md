# NGA API Implementation Report

Implementation commit: `96098ee feat: implement NGA API request layer`

## Scope

The current implementation provides a pure Kotlin request construction layer under:

```text
app/src/main/java/com/yanga/client/api/
```

It does not perform live network calls. This is intentional for the current milestone: endpoint behavior is captured in typed request objects first, so it can be tested without a real NGA account or production server calls. A Retrofit/OkHttp transport can be attached after the UI and runtime architecture are finalized.

## Implemented Files

| File | Purpose |
| --- | --- |
| `NgaModels.kt` | Session, supported domains, HTTP method, request/body models, missing-session error, WebView login cookie parser |
| `NgaEncoding.kt` | UTF-8 and GBK URL encode/decode helpers |
| `NgaResponseNormalizer.kt` | Cleans NGA wrapped JS/JSON response strings |
| `NgaStaticUrls.kt` | Board icon, stid icon, relative image, and emoticon URL helpers |
| `NgaApi.kt` | Request builders for documented NGA endpoint families |

## Endpoint Family Coverage

| Family | Implementation |
| --- | --- |
| Session/domain configuration | `NgaSession`, `NgaDomains` |
| Common headers | `NgaApi.commonHeaders()` adds `Cookie`, `User-Agent`, `X-User-Agent` |
| Topic list | `NgaApi.topicList()` |
| Thread detail | `NgaApi.articleRead()` |
| Board search | `NgaApi.boardSearch()` |
| Remote board categories | `NgaApi.remoteBoardCategories()` |
| WebView login URL | `NgaApi.loginPage()` |
| Login cookie parsing | `NgaLoginCookies.parse()` |
| Post preflight | `NgaApi.topicPostInfo()` |
| Topic/reply body | `NgaApi.topicPostBody()`, `NgaApi.topicPost()` |
| Attachment upload metadata | `NgaApi.attachmentUploadMetadata()` |
| Comment / "贴条" | `NgaApi.commentPost()` |
| Favorite add/remove | `NgaApi.favoriteAdd()`, `NgaApi.favoriteRemove()` |
| Like/dislike | `NgaApi.like()` |
| Report | `NgaApi.report()` |
| Notifications fetch/clear | `NgaApi.notifications()`, `NgaApi.clearNotifications()` |
| Check-in | `NgaApi.checkIn()` |
| Profile/UCP | `NgaApi.profile()` |
| Signature update | `NgaApi.signature()` |
| Block words fetch/update | `NgaApi.blockWords()`, `NgaApi.updateBlockWords()` |
| Private message list/read/send | `NgaApi.messageList()`, `NgaApi.messageRead()`, `NgaApi.messagePost()` |
| Sub-board subscribe/hide | `NgaApi.subBoardOption()` |
| Vote/settle | `NgaApi.vote()` |
| Avatar upload/change | `NgaApi.avatarUploadMetadata()`, `NgaApi.avatarChange()` |
| Static images | `NgaStaticUrls` |

## Important Behaviors Preserved

1. Domain is selected through `NgaSession.baseUrl` and normalized without a trailing slash.
2. Cookie is optional at session level, but authenticated operations can be wrapped with `authenticated { ... }` to return `MissingNgaSessionException` when absent.
3. GBK URL encoding is used for post bodies, signatures, messages, comments, board search, and block-word payloads.
4. UTF-8 URL encoding is used for topic keyword search, matching the reference client's topic-list path.
5. `NgaResponseNormalizer` strips:
   - `window.script_muti_get_var_store=`
   - `/*error fill content...`
   - `/*$js$*/`
   - malformed numeric `content` / `subject` fragments copied from the reference parser behavior.
6. Static image URL templates match the reference project.

## Intentional Gaps

1. No live HTTP execution yet.
2. No multipart binary upload body builder yet; current upload functions expose metadata fields and endpoint/header intent.
3. No full NGA response model parsing yet, except login cookies and response normalization.
4. No final UI navigation or screen-specific repository contracts yet.
5. Auth guards are available as a wrapper, but not forced on every authenticated builder until repository/service boundaries are introduced.

## Next Step After UI Direction

Once the app interface organization is provided, the API layer should be wrapped in feature repositories, for example:

1. `TopicRepository`
2. `ThreadRepository`
3. `AccountRepository`
4. `MessageRepository`
5. `ProfileRepository`
6. `BoardRepository`

Those repositories should own live transport calls, parsing, pagination, and UI-facing state.
