# NGA MD3 Kotlin Client PRD

## 1. Background

This project will create a new Android client for the NGA forum, using Kotlin and Material Design 3. The reference implementation is `Justwen/NGA-CLIENT-VER-OPEN-SOURCE`; its API usage has been extracted into `docs/api-research.md`.

This PRD intentionally stops short of deciding the app's final screen organization. The current milestone is a blank MD3 app shell plus a tested API layer, so the UI can later be organized after product direction is confirmed.

## 2. Goals

1. Build a modern Kotlin Android project that can become an NGA forum client.
2. Use Material Design 3 and Jetpack Compose for the blank app shell.
3. Implement a testable API layer covering the endpoint families documented in `docs/api-research.md`.
4. Preserve NGA-specific behavior that affects correctness: selected base domain, cookies, user agent, `X-User-Agent`, GBK encoding, wrapped JS JSON cleanup, and authenticated-operation guards.
5. Produce an implementation report and a test report before starting real UI layout work.

## 3. Non Goals

1. Do not implement the final home/navigation/feed/detail/message/profile UI in this milestone.
2. Do not require a real NGA account or live authenticated requests for automated tests.
3. Do not clone the old Java/Rx architecture; keep the new API layer Kotlin-first and coroutine-friendly.
4. Do not migrate all parsing models from the reference project in this milestone. Endpoint construction and transport behavior are the acceptance focus.
5. Do not store user credentials. Login handling is limited to cookie/session representation and WebView-login URL documentation until UI flow is designed.

## 4. Users and Jobs

| User | Job |
| --- | --- |
| Reader | Browse boards, topic lists, and thread contents after UI is implemented |
| Logged-in forum user | Post replies, comments, messages, favorites, likes, check-ins, and profile settings after UI is implemented |
| Developer | Verify request construction and response normalization without hitting real NGA production endpoints |
| Product designer / app owner | Decide navigation and screen organization after API contracts are known |

## 5. Recommended Approach

### Option A: API-first Kotlin project, then UI

Create a minimal Compose MD3 app, implement endpoint builders and a Retrofit/OkHttp-backed API client, and test the request construction with mocked HTTP. UI screens are deferred.

Trade-off: slower visible product progress, but the riskiest unknowns are captured before screen work.

### Option B: UI-first prototype with partial API

Create screens first and add only the endpoints needed by those screens.

Trade-off: faster demo, but likely rework after the complete NGA API surface is understood.

### Option C: Port reference modules directly

Copy or translate existing Java/Kotlin modules into the new app.

Trade-off: fastest API coverage on paper, but keeps legacy coupling, old Rx patterns, and UI assumptions.

Decision: Use Option A. It matches the requested sequence: API research, PRD, blank app, API implementation and tests, then wait for UI organization.

## 6. Product Scope for Current Milestone

### Blank App Shell

The app should launch into a neutral Material Design 3 Compose screen with:

1. App name.
2. Empty-state text that indicates UI organization is pending.
3. No feed, navigation drawer, tab structure, or feature-specific screen layout.

### API Layer

The API layer should expose Kotlin interfaces or repositories for these endpoint families:

| Family | Required in current milestone |
| --- | --- |
| Session/domain configuration | Yes |
| Topic list request construction | Yes |
| Thread detail request construction | Yes |
| Board search and remote board categories | Yes |
| Login URL and cookie parsing helpers | Yes |
| Topic post/reply body construction | Yes |
| Attachment upload request construction | Yes |
| Comment body construction | Yes |
| Favorite add/remove request construction | Yes |
| Like/dislike request construction | Yes |
| Report request construction | Yes |
| Notifications fetch/clear request construction | Yes |
| Check-in request construction | Yes |
| Profile/UCP request construction | Yes |
| Signature update body construction | Yes |
| Block word fetch/update request construction | Yes |
| Private message list/read/send request construction | Yes |
| Sub-board subscribe/hide request construction | Yes |
| Vote request construction | Yes |
| Avatar upload/change request construction | Yes |
| Static image URL builders | Yes |
| Full response model parsing | Partial: only normalization and small success/error helpers |

### Testing Scope

Automated tests must cover:

1. URL construction for representative read-only endpoints.
2. GBK encoding for post content, title, comments, signature, board search, messages, and block lists.
3. Cookie parsing for WebView login cookies.
4. Headers added by the HTTP client or request builder.
5. Wrapped response cleanup for `window.script_muti_get_var_store=`, `/*error fill content`, and `/*$js$*/`.
6. Authenticated endpoints returning a typed missing-session error if no cookie is present.

Tests should not call live NGA servers. Use local request builders, fake transports, or `MockWebServer`.

## 7. Functional Requirements

### FR1 Session Configuration

The API layer must represent:

1. Selected base domain.
2. Optional cookie string.
3. User agent.
4. Default `X-User-Agent: Nga_Official`.

### FR2 Request Construction

Every endpoint family listed in section 6 must have a named Kotlin function or request builder. The output must be testable without Android UI.

### FR3 Encoding

The API layer must support:

1. UTF-8 query encoding where the reference client uses UTF-8.
2. GBK URL encoding where the reference client uses GBK.
3. Form bodies with `application/x-www-form-urlencoded`.

### FR4 Auth Guard

Authenticated calls must not silently build unsafe requests when no session cookie exists. They must return or throw a typed missing-session error.

### FR5 Response Normalization

The API layer must provide a reusable normalizer for NGA wrapped JS/JSON responses.

### FR6 MD3 Blank App

The app must compile and show a blank Material Design 3 Compose shell. It must not commit to final navigation or screen hierarchy.

## 8. Nonfunctional Requirements

1. Kotlin-first source code.
2. Coroutine-ready APIs.
3. Unit-testable request logic with no Android framework dependency where practical.
4. Small, focused modules/classes.
5. No live credentials or secrets in the repository.
6. Checkpoint git commit after each completed part.

## 9. Acceptance Criteria

The milestone is complete when:

1. `docs/api-research.md` exists and is committed.
2. This PRD exists and is committed.
3. A Kotlin Android project exists and builds.
4. The app module uses Material Design 3 and Compose.
5. API request builders/client wrappers exist for the endpoint families in this PRD.
6. Unit tests pass locally.
7. `docs/api-implementation-report.md` describes the implemented interfaces and any intentional gaps.
8. `docs/test-report.md` records the verification commands and results.
9. Work stops before final UI organization, waiting for the user's layout/navigation direction.

## 10. Open Decisions for Next Phase

1. App navigation model: bottom bar, navigation rail, drawer, or adaptive combination.
2. Initial content priority: boards, favorite boards, recent topics, or search.
3. Thread reading layout and pagination behavior.
4. Login flow placement and account switching UX.
5. Message and notification prominence.
6. Offline cache behavior.
