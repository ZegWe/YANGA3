# Yanga UI Design Spec

## 1. Purpose

This document supplements `docs/prd.md` with the UI design direction for the next phase of Yanga. It turns the approved HTML mockups into an implementation-ready product and Compose design specification.

The current direction is:

1. Use a board-first NGA forum client structure.
2. Keep four primary destinations: Home, Boards, Messages, Profile.
3. Use Material Design 3 and Jetpack Compose.
4. Keep top bars free of duplicate primary navigation.
5. Hide primary bottom navigation on deep reading and editing flows.

The HTML mockups are exploratory design artifacts. This document is the source of truth for implementation.

## 2. Source Mockups

Design references are stored under:

`D:\dev\yanga3\.superpowers\brainstorm\codex-20260528154109\content`

Relevant files:

1. `board-first-detail.html` - Home page, signed-in and signed-out states, search/manage state.
2. `secondary-tabs-detail.html` - Boards, Messages, and Profile tabs.
3. `forum-board-thread-detail.html` - Forum section, board topic list, and thread reading pages.
4. `composer-pages-detail.html` - New topic, reply, and composer tools pages.

## 3. Information Architecture

### 3.1 Primary Tabs

The app has exactly four primary tabs. These are the only first-level navigation destinations.

| Tab | Role | Shows bottom navigation |
| --- | --- | --- |
| Home | Favorite boards, active topics, global search entry, signed-out prompt | Yes |
| Boards | Full forum directory, subscriptions, board search, subscribe/hide management | Yes |
| Messages | Private message conversations only | Yes |
| Profile | Account, sign-in state, notifications, favorites, settings, tools | Yes |

Top bars must not duplicate these destinations. Do not put Profile, Settings, Notifications, or full board navigation in the Home top bar. Profile owns account, settings, and notifications.

### 3.2 Deep Pages

Deep pages use a back button and do not show the primary bottom navigation.

| Page | Source | Role |
| --- | --- | --- |
| Forum section | Boards tab category item | Shows child boards and section hot topics |
| Board topic list | Home board card, Boards subscribed board, or Forum section child board | Shows pinned topics, topic list, filters, and new topic FAB |
| Thread reading | Topic item | Shows posts, floor metadata, quote blocks, bottom reply action bar |
| New topic composer | Home or board FAB | Creates a new topic |
| Reply composer | Thread bottom reply bar | Replies to a thread, optionally with quote context |
| Composer tools | Composer "More" tool | Inline expanded editor tools and validation |
| Private message detail | Messages conversation item | Private conversation detail |
| Settings/detail pages | Profile settings rows | Account and reading settings |

Lightweight in-context states such as Home search expansion and favorite board management may keep the bottom navigation if they behave as an overlay or same-tab state rather than a full-screen deep route.

## 4. Navigation Flow

```text
Home
├─ Search -> Search state / Search page
├─ Favorite board card -> Board topic list
├─ Active topic -> Thread reading
└─ FAB -> New topic composer

Boards
├─ Search -> Board search
├─ Subscribed board -> Board topic list
├─ Forum category -> Forum section
└─ Manage -> Subscribe / hide management

Forum section
├─ Child board -> Board topic list
├─ Section hot topic -> Thread reading
└─ Back -> Boards

Board topic list
├─ Topic item -> Thread reading
├─ FAB -> New topic composer
└─ Back -> Home / Boards / Forum section source

Thread reading
├─ Reply bar -> Reply composer
├─ Favorite -> Favorite current thread
├─ More -> Share, report, font size, reverse order, page jump
└─ Back -> Board topic list

Messages
├─ Conversation -> Private message detail
├─ Write message -> Message composer
└─ Block list -> Private message block management

Profile
├─ Notifications -> Notification detail
├─ Sign in / Sign out / Switch account -> Account flow
├─ Reading and appearance -> Settings
├─ Cache and history -> Settings
└─ Block words and blacklist -> Settings
```

## 5. Page Specifications

### 5.1 Home

Home is board-first. It helps users decide where to read before showing individual topics.

Required content:

1. Top area with app title and status subtitle only.
2. Global search entry for boards, topics, and users.
3. Filter chips: Favorite, Hot topics, Favorites, History.
4. Favorite board grid with two columns.
5. Active topic list.
6. Signed-out prompt card when unauthenticated.
7. FAB for new topic only when the user is signed in or can be prompted to sign in.
8. Bottom navigation.

Home must not contain profile, settings, or notification buttons in the top bar.

### 5.2 Boards

Boards owns full forum discovery and management.

Required content:

1. Title and subtitle.
2. Board search.
3. Filter chips for All, Subscribed, Game, Life, and future categories.
4. Subscribed boards section.
5. Full forum category section.
6. Management entry for subscribe, hide, and ordering.
7. Bottom navigation.

### 5.3 Messages

Messages is for private messages only. It must not contain forum replies, favorite updates, or system notifications.

Required content:

1. Private message search.
2. Filter chips: All, Unread, Sent, Blocked.
3. Conversation list with avatar, contact, preview, unread count, and time.
4. Quick actions: write private message and block list.
5. Bottom navigation.

### 5.4 Profile

Profile owns account, notifications, favorites, settings, and tools.

Required content:

1. Profile card with avatar, username, UID, and session state.
2. Sign-in, sign-out, switch account, and check-in actions.
3. Counters for favorite topics, subscribed boards, and new notifications.
4. Notification center for reply alerts and favorite topic updates.
5. Settings list for reading appearance, cache/history, block words, and blacklist.
6. Bottom navigation.

### 5.5 Forum Section

Forum section is a deep page reached from Boards.

Required content:

1. Back top bar with section title, board count, and activity summary.
2. Context search limited to the section.
3. Summary card explaining the section.
4. Filter chips for All, Subscribed, and section categories.
5. Child board list.
6. Section hot topics.
7. No primary bottom navigation.

### 5.6 Board Topic List

Board topic list is a deep page reached from Home, Boards, or Forum section.

Required content:

1. Back top bar with board name, fid, and new reply count.
2. Context search limited to the board.
3. Filter chips: All, Essence, Latest reply, Favorites.
4. Pinned topic section.
5. Topic list with tags, title, author, reply count, and last active time.
6. FAB for new topic.
7. No primary bottom navigation.

### 5.7 Thread Reading

Thread reading prioritizes readable long-form forum content.

Required content:

1. Back top bar with thread page and reply count.
2. Thread title card.
3. Filter chips: Author only, All, Images only, Jump floor.
4. Post list with author, avatar, floor number, time, content, and quote blocks.
5. More menu for share, report, font size, reverse order, and page jump.
6. Bottom reply action bar with reply entry, favorite, and jump/top action.
7. No primary bottom navigation.

### 5.8 New Topic Composer

New topic composer is a deep editing page.

Required content:

1. Back/cancel action.
2. Top publish button.
3. Target board context card.
4. Topic category selector.
5. Title input.
6. Body editor.
7. Attachment grid.
8. Options for anonymous posting, signature, and other board-supported settings.
9. Bottom editor toolbar.
10. Draft save state.
11. Captcha and validation states when required.
12. No primary bottom navigation.

### 5.9 Reply Composer

Reply composer is a deep editing page.

Required content:

1. Back/cancel action.
2. Top reply button.
3. Thread context card.
4. Optional quote card.
5. Reply mode chips: Quote reply, Plain reply, Author only context.
6. Body editor.
7. Attachment grid.
8. Options for anonymous reply and notifying the replied user.
9. Bottom editor toolbar.
10. Draft save state.
11. Captcha and validation states when required.
12. No primary bottom navigation.

## 6. Compose Component Plan

### 6.1 App-Level Components

1. `YangaAppScaffold` - App-level Material theme, safe drawing, and navigation host.
2. `MainTabScaffold` - Container for Home, Boards, Messages, and Profile.
3. `YangaNavigationBar` - Phone bottom navigation.
4. `YangaNavigationRail` - Wide-screen replacement for bottom navigation.
5. `YangaTopBar` - Page title, subtitle, back action, more menu, and publish actions.
6. `ContextSearchBar` - Global or scoped search entry.
7. `FilterChipRow` - Horizontal chip filters.
8. `SectionHeader` - Section title with optional trailing action.

### 6.2 Reusable Content Components

1. `BoardCard` - Favorite board grid item.
2. `BoardListItem` - Board row for subscriptions, child boards, and search results.
3. `ForumCategoryItem` - Forum category row.
4. `TopicCompactItem` - Compact topic item for Home and hot topic lists.
5. `TopicListItem` - Board topic list row.
6. `TopicTag` - Topic tag chip.
7. `ReplyCountBadge` - Reply count indicator.
8. `MessageThreadItem` - Private message conversation row.
9. `ProfileCard` - Account and session card.
10. `CounterGrid` - Profile counters.
11. `SettingsRow` - Settings and quick action row.
12. `StateCard` - Informational card such as login prompt or section summary.
13. `ErrorCard` - Error and validation card.
14. `EmptyState` - Empty list state.
15. `LoadingState` - Loading indicator.
16. `QuoteBlock` - Quoted content inside post or reply composer.

### 6.3 Reading and Editing Components

1. `ThreadTitleCard` - Thread title and metadata.
2. `PostItem` - A single floor/post.
3. `PostHeader` - Author, avatar, time, and floor number.
4. `ThreadBottomActionBar` - Reply entry, favorite, jump/top controls.
5. `ComposerScreen` - Unified new topic and reply editor.
6. `ComposerTopBar` - Cancel/back, title, subtitle, publish/reply action.
7. `ComposerContextCard` - Target board or replied thread context.
8. `TitleField` - New topic title input.
9. `BodyEditorField` - Main editor input.
10. `ComposerToolBar` - Formatting, emoji, image, link/mention, more.
11. `ComposerMoreToolsPanel` - Heading, divider, code block, poll, preview.
12. `AttachmentGrid` - Attachment slots and upload states.
13. `CaptchaBlock` - Captcha image, input, refresh.
14. `ComposerOptionsCard` - Anonymous, signature, notify replied user.
15. `ValidationErrorCard` - Publish-blocking validation errors.

### 6.4 Authentication Components

1. `AuthGate` - Intercepts protected actions and starts login prompt.
2. `PasswordLoginScreen` - Existing login screen, later aligned with this design system.
3. `CaptchaImage` - Existing captcha image component, enhanced with loading and error states.
4. `LoginPromptCard` - Reusable signed-out prompt.
5. `LoginSessionCard` - Signed-in session summary.

## 7. State Model

### 7.1 Authentication

1. `Unauthenticated` - Public browsing allowed; protected actions prompt login.
2. `Authenticated` - Shows username, UID, favorites, messages, notifications, and posting actions.
3. `SessionExpired` - Cookie expired; prompt re-login.
4. `SwitchingAccount` - Account switch in progress.
5. `LoggingOut` - Logout in progress.

### 7.2 Loading

1. `InitialLoading`
2. `Refreshing`
3. `LoadingMore`
4. `Submitting`
5. `UploadingAttachment`
6. `CaptchaLoading`
7. `SearchLoading`

### 7.3 Empty States

1. `NoFavoriteBoards`
2. `NoSubscribedBoards`
3. `NoTopics`
4. `NoMessages`
5. `NoNotifications`
6. `NoSearchResults`
7. `NoDrafts`
8. `NoAttachments`

### 7.4 Error States

1. `NetworkError`
2. `ServerError`
3. `AuthRequired`
4. `SessionExpiredError`
5. `PermissionDenied`
6. `RateLimited`
7. `ValidationError`
8. `AttachmentUploadError`
9. `CaptchaError`
10. `ContentDeletedOrHidden`

### 7.5 Search

1. `SearchCollapsed`
2. `SearchFocused`
3. `SearchHasQuery`
4. `SearchLoading`
5. `SearchResults`
6. `SearchEmpty`
7. `SearchHistory`
8. `SearchScope` - Global, forum section, board, or private messages.

### 7.6 Drafts and Composer

1. `DraftIdle`
2. `DraftDirty`
3. `DraftSaving`
4. `DraftSaved`
5. `DraftSaveFailed`
6. `DraftRestored`
7. `DraftDiscardConfirm`
8. `NewTopic`
9. `ReplyTopic`
10. `QuoteReply`
11. `PlainReply`
12. `PreviewMode`
13. `MoreToolsExpanded`
14. `KeyboardVisible`
15. `PublishReady`
16. `PublishBlocked`

### 7.7 Captcha and Attachments

Captcha states:

1. `CaptchaNotRequired`
2. `CaptchaRequired`
3. `CaptchaLoading`
4. `CaptchaLoaded`
5. `CaptchaLoadFailed`
6. `CaptchaInvalid`
7. `CaptchaRefreshing`

Attachment states:

1. `AttachmentEmpty`
2. `AttachmentPicking`
3. `AttachmentPending`
4. `AttachmentUploading`
5. `AttachmentUploaded`
6. `AttachmentFailed`
7. `AttachmentRemoving`
8. `AttachmentBlockedSubmit`

### 7.8 Lists and Filters

1. `PageIndex`
2. `HasNextPage`
3. `PinnedExpanded`
4. `PinnedCollapsed`
5. `SortLatestReply`
6. `SortLatestPost`
7. `FilterEssence`
8. `FilterFavorites`
9. `FilterUnread`
10. `FilterImagesOnly`
11. `OnlyAuthor`

## 8. Material Design 3 Mapping

| Element | Material3 Compose component | Requirement |
| --- | --- | --- |
| App theme | `MaterialTheme` | Use `colorScheme`, `typography`, and `shapes`; avoid hard-coded colors and text sizes. |
| Primary navigation | `NavigationBar`, `NavigationBarItem` | Phone bottom navigation for Home, Boards, Messages, Profile only. |
| Wide navigation | `NavigationRail` | Use on wide layouts without changing information architecture. |
| Deep page app bars | `TopAppBar`, `CenterAlignedTopAppBar` | Show back button; no primary bottom navigation. |
| Icon actions | `IconButton` | 48dp minimum touch target and content descriptions. |
| Search | `SearchBar`, `DockedSearchBar` | Home search is global; deep search is scoped. |
| Filters | `FilterChip`, `AssistChip` | Selected state uses `primaryContainer`; inactive state uses outline. |
| Cards | `Card`, `ElevatedCard` | Use tonal surfaces and restrained elevation. |
| Rows | `ListItem` | Use leading avatar/icon, headline, supporting text, trailing badge/chevron. |
| Avatars | `Surface` with circular shape | Fallback to initial or icon if no avatar. |
| Badges | `Badge`, `BadgedBox` | Use for unread counts and new replies. |
| New topic | `FloatingActionButton` | Use on Home and Board topic list only. |
| Thread reply bar | `BottomAppBar` or custom bottom bar | Fixed bottom reply entry in thread reading page. |
| Editor fields | `TextField`, `OutlinedTextField` | Title for new topic only; body for new topic and reply. |
| Primary actions | `Button` | Publish, login, and check-in. |
| Secondary actions | `OutlinedButton`, `TextButton` | Later, switch account, logout. |
| Toggles | `Switch` | Anonymous, notify replied user. |
| Menus | `DropdownMenu`, `ExposedDropdownMenuBox` | Category, sort, page jump, more menu. |
| Editor tools | `BottomAppBar` with `IconButton` | Formatting, emoji, image, link/mention, more. |
| Errors | Error-state `TextField`, `Card` with error colors | Validation, captcha, upload errors. |
| Loading | `CircularProgressIndicator`, `LinearProgressIndicator` | Page loading and upload progress. |

## 9. Visual System

### 9.1 Color

1. Android 12+ should use `dynamicLightColorScheme` and `dynamicDarkColorScheme`.
2. Non-dynamic fallback should replace the template purple palette with a warm neutral direction: warm background, off-white surfaces, amber/brown-gold primary, and a restrained green tertiary.
3. Use `background` for app background.
4. Use `surface`, `surfaceContainer`, and `surfaceContainerHigh` for content layers.
5. Use tonal containers for login prompts, section summaries, captcha prompts, and thread title cards.
6. Use `error` and `errorContainer` for validation and publishing errors.
7. Text colors must come from `onSurface`, `onSurfaceVariant`, `onPrimaryContainer`, or equivalent scheme roles.

### 9.2 Typography

1. Page titles use `titleLarge` or `headlineSmall`.
2. List titles use `titleSmall` or `bodyLarge`.
3. Metadata uses `bodySmall` or `labelSmall`.
4. Thread reading content needs more line height than dense lists.
5. Chinese text should use system sans fonts; do not hard-code web fonts in Compose.
6. Do not use negative letter spacing.
7. Text must support system font scaling.

### 9.3 Shape

1. Search bars and reply entry use full or extra-large rounded shapes.
2. Cards use medium or large shapes.
3. FAB uses the MD3 default shape.
4. Chips use MD3 chip shapes and keep stable height.
5. Avatars are circular.
6. Board icons may use rounded squares.

### 9.4 Tonal Surfaces

1. Prefer tonal elevation, borders, and surface color differences over heavy shadows.
2. Bottom navigation and editor toolbars use `surfaceContainer` or `surfaceContainerHigh`.
3. Deep page summary cards may use low-emphasis `primaryContainer` variants.
4. Dark theme must be derived from ColorScheme rather than hard-coded light colors.

## 10. Responsive Design

1. Phones use `NavigationBar`.
2. Wide screens may use `NavigationRail`.
3. The navigation model remains unchanged across screen sizes.
4. Reading content must not stretch indefinitely on wide screens; constrain line width.
5. Chips and metadata must wrap or scroll without overflow.
6. Long topic titles can wrap or truncate depending on context.
7. Bottom editor toolbars must account for IME insets.
8. All pages must respect safe drawing and system bar insets.

## 11. Accessibility

1. All `IconButton`, FAB, navigation item, favorite, jump, and more-menu actions need content descriptions.
2. Touch targets must be at least 48dp.
3. Text must support system font scaling.
4. Unread counts and validation errors cannot rely on color alone.
5. Error cards must explain the fixable problem.
6. Keyboard navigation and screen reader order should match the visual hierarchy.
7. Search fields and editor fields need clear labels.

## 12. Acceptance Criteria

1. The app has exactly four primary tabs: Home, Boards, Messages, Profile.
2. Home top bar does not contain board, account, notification, or settings actions.
3. Profile owns account, notifications, favorites, and settings.
4. Messages contains private messages only.
5. Forum section, board topic list, thread reading, new topic composer, and reply composer hide the primary bottom navigation.
6. Board topic list uses a FAB for new topic.
7. Thread reading uses a bottom reply action bar instead of a FAB.
8. Composer pages use top publish/reply action and bottom editor toolbar.
9. Signed-out users can browse public content.
10. Protected actions prompt login.
11. Loading, empty, network error, session expired, captcha, draft, and attachment upload states are represented.
12. Publish validation identifies concrete issues such as empty title, short body, captcha error, or unfinished attachment upload.
13. The UI uses Material3 components and ColorScheme roles rather than hard-coded ad hoc styling.
14. Phone and wide-screen layouts preserve the same information architecture.
15. Thread reading keeps author, floor number, timestamp, quotes, and body content readable.

## 13. Implementation Notes

1. The first implementation pass should create reusable display components and fake data previews before connecting live API calls.
2. `MainScreen.kt` currently combines login shell and password login. Future UI work should split route/state handling from pure screen components.
3. Existing `LoginSessionUiState` can seed Profile and `AuthGate` behavior.
4. Existing `PasswordLoginScreen` and `CaptchaImage` can be retained, then restyled to match this spec.
5. Existing API builders map naturally to planned pages:
   - Board and topic list requests feed Home, Boards, Forum section, and Board topic list.
   - Thread detail parsing feeds Thread reading.
   - Comment and topic post requests feed Reply and New topic composers.
   - Favorite, notification, check-in, profile, private message, block word, and attachment APIs feed Profile, Messages, and Composer flows.

