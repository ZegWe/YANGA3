# NGA Android Client API Research

Source reference: `Justwen/NGA-CLIENT-VER-OPEN-SOURCE`, cloned under `source-upstream`.

This document records the website endpoints and request conventions used by the reference Android client. The goal is to preserve API usage before building a new Kotlin + Material Design 3 client.

## Common Transport

### Domains

The original client lets users choose a forum domain:

| Domain |
| --- |
| `https://bbs.ngacn.cc` |
| `https://bbs.nga.cn` |
| `https://nga.178.com` |
| `https://nga.donews.com` |
| `https://ngabbs.com` |

Most forum endpoints are built from the selected base domain. A few legacy flows use fixed hosts such as `http://bbs.ngacn.cc`, `http://bbs.nga.cn`, `http://nga.178.com`, `https://img8.nga.cn`, and `http://app.myauth.us`.

### Headers

The Retrofit helper adds these headers to outgoing requests:

| Header | Value / Source |
| --- | --- |
| `Cookie` | Active user's NGA cookie, unless the request already has a `Cookie` header |
| `User-Agent` | Android WebView default user agent, user configurable |
| `X-User-Agent` | `Nga_Official` |

Additional request-specific headers:

| Flow | Headers |
| --- | --- |
| Captcha image | `Referer: https://bbs.ngacn.cc/nuke.php?__lib=login&__act=login_ui` |
| Password login | `Referer`, `Origin: https://bbs.ngacn.cc`, browser-like `User-Agent`, `Accept`, `Upgrade-Insecure-Requests` |
| UCP profile | `Referer: {base}/nuke.php?func=ucp&lite=jsx&...` |
| Block word update | `Referer`, `charset: GBK`, `Host: bbs.nga.cn`, `Origin`, `content-type: application/x-www-form-urlencoded` |

### Encodings and Response Shapes

| Area | Convention |
| --- | --- |
| List/read JSON | Often uses `lite=js`, `noprefix`, `__output=8`, or `v2` |
| POST body | `application/x-www-form-urlencoded`; many text fields are GBK URL-encoded |
| Legacy HTML POST | Reads server response as `gbk` and extracts `<title>` or `window.script_muti_get_var_store=` |
| Wrapped JS JSON | Many endpoints return `window.script_muti_get_var_store={...}` and sometimes include `/*error fill content` or `/*$js$*/`, which must be stripped |
| Success/error | Often returned under `data["0"]` or `error["0"]` |

## Endpoint Inventory

### Topic List

`GET {base}/thread.php`

Used to fetch forum topic lists, searches, favorites, author topics, and recommended threads.

Common query parameters:

| Parameter | Usage |
| --- | --- |
| `page` | Page number |
| `lite=js` | JS-like JSON response |
| `noprefix` | Suppresses JS prefix where supported |
| `fid` | Forum id |
| `stid` | Sub forum id for some boards |
| `authorid` | Filter by author uid |
| `searchpost` | Search replies/posts |
| `favor=1` | Favorites list |
| `content` | Content search mode |
| `author` | Author name, GBK URL-encoded; special suffix `&searchpost=1` is handled |
| `key` | Search keyword, UTF-8 URL-encoded in this code path |
| `fidgroup=user` | User-board search group |
| `recommend=1` | Recommended/essence/scored topics |
| `order_by=postdatedesc` | Used with `recommend=1` |
| `user=1` | Used with `recommend=1` |

Examples:

```text
{base}/thread.php?fid=7&page=1&lite=js&noprefix
{base}/thread.php?authorid=123&page=1&lite=js&noprefix
{base}/thread.php?key=keyword&page=1&lite=js&noprefix
```

### Article / Thread Detail

`GET {base}/read.php`

Used to fetch thread contents.

Common query parameters:

| Parameter | Usage |
| --- | --- |
| `page` | Page number |
| `tid` | Topic id |
| `pid` | Reply id; used to jump/search by reply |
| `authorid` | Filter replies by author |
| `__output=8` | JSON-like output |
| `noprefix` | Suppresses JS prefix where supported |
| `v2` | v2 response |
| `searchpost=1` | Used in generated links for reply search |

Example:

```text
{base}/read.php?page=1&__output=8&noprefix&v2&tid=123456
```

### Forum Board Search

`GET http://bbs.nga.cn/forum.php`

Used to search board names.

| Parameter | Usage |
| --- | --- |
| `__output=8` | JSON-like output |
| `key` | Board name, GBK URL-encoded |

Example:

```text
http://bbs.nga.cn/forum.php?&__output=8&key={gbkEncodedBoardName}
```

### Remote Board Categories

`GET {base}/app_api.php?__lib=home&__act=category`

Used to fetch remote board category data. The client caches the raw JSON locally as `board_list_remote.json`.

### Login

The newer account module uses WebView login:

`GET https://ngabbs.com/nuke.php?__lib=login&__act=account&login`

Login is considered successful when the page pair contains the login URL and text `登录成功 是否返回首页`. Cookies are then read from Android `CookieManager`.

Required cookies:

| Cookie | Usage |
| --- | --- |
| `ngaPassportUid` | User id |
| `ngaPassportCid` | Client/session id |
| `ngaPassportUrlencodedUname` | Username; decoded twice with GBK |

Legacy Retrofit login paths still exist:

| Method | Endpoint | Notes |
| --- | --- | --- |
| `GET` | Captcha URL supplied by caller | Adds login UI referer |
| `POST` | `nuke.php?__lib=login&__act=login&raw=3` | Form body, browser-like headers |
| `POST` | `nuke.php` | Multipart login variant with referer `https://bbs.ngacn.cc/nuke/p2.htm?login` |

### Topic Posting / Replying

`POST {base}/post.php?`

Used for new topics and replies. The preflight call gets an attachment auth code:

`POST {base}/post.php?fid={fid}&lite=js[&action=...][&pid=...][&tid=...][&stid=...]`

The response is stripped of `window.script_muti_get_var_store=` and parsed for `data.auth`.

Submit body:

| Field | Usage |
| --- | --- |
| `step=2` | Required post step |
| `post_content` | GBK URL-encoded content |
| `pid` | Reply target pid |
| `tid` | Existing topic id |
| `action` | Post action such as reply/edit |
| `post_subject` | GBK URL-encoded title |
| `fid` | Forum id |
| `anony=1` | Anonymous post |
| `attachments` | Tab-prefixed uploaded attachment ids |
| `attachments_check` | Tab-prefixed attachment checks |
| `stid` | Sub forum id |

Success markers include `发贴完毕` and `@提醒每24小时不能超过50个`.

### Topic Category / Tags

`GET {base}/nuke.php`

| Parameter | Value |
| --- | --- |
| `__lib` | `topic_key` |
| `__act` | `get` |
| `fid` | Forum id |
| `__output` | `8` |

The client reads `data["0"]` as a numbered map of category names.

### Attachment Upload for Posts

`POST https://img8.nga.cn/attach.php?`

Multipart form data:

| Field | Value / Usage |
| --- | --- |
| `attachment_file1` | Image file part |
| `attachment_file1_url_utf8_name` | UTF-8 file name |
| `fid` | Forum id |
| `auth` | Auth code from post preflight |
| `func` | `upload` |
| `v2` | `1` |
| `lite` | `js` |
| `attachment_file1_auto_size` | Empty string, automatic resize |
| `attachment_file1_watermark` | Empty string, no watermark |
| `attachment_file1_dscp` | Empty string |
| `attachment_file1_img` | `1` |
| `origin_domain` | `bbs.ngacn.cc` |

The response is stripped of `window.script_muti_get_var_store=`. On success, `data.attachments`, `data.attachments_check`, and `data.url` are used. If `error_code == 9`, the original client retries after compressing the image.

### Post Comment / "贴条"

`POST {base}/post.php`

Body:

| Field | Value / Usage |
| --- | --- |
| `post_content` | Optional prefix + comment, GBK URL-encoded |
| `tid` | Topic id |
| `pid` | Reply id |
| `fid` | Forum id |
| `nojump` | `1` |
| `step` | `2` |
| `action` | `reply` |
| `comment` | `1` |
| `lite` | `htmljs` |
| `anony` | `1` when anonymous |

The response is parsed from `window.script_muti_get_var_store=`. `data.__MESSAGE["3"] == 200` and message containing `发贴完毕` means success.

### Topic Favorite / Bookmark

Add favorite:

`POST {base}/nuke.php?__lib=topic_favor&lite=js&noprefix&__act=topic_favor&action=add&tid={tid}[&pid={pid}]`

Remove favorite:

`POST {base}/nuke.php`

Field map:

| Field | Value |
| --- | --- |
| `__lib` | `topic_favor` |
| `__act` | `topic_favor` |
| `__output` | `8` |
| `action` | `del` |
| `page` | Favorite list page |
| `tidarray` | `tid` or `tid_pid` |

Success is detected by response text containing `操作成功`.

### Like / Dislike

`POST {base}/nuke.php`

Field map:

| Field | Value |
| --- | --- |
| `__lib` | `topic_recommend` |
| `__act` | `add` |
| `raw` | `3` |
| `pid` | Reply id, default `0` |
| `__output` | `8` |
| `value` | `1` for support, `-1` for oppose |
| `tid` | Topic id |

The client displays `data["0"]`.

### Report Post

`POST {base}/nuke.php`

The dialog prepares both query and field maps:

| Field | Value / Usage |
| --- | --- |
| `__lib` | `log_post` |
| `__act` | `report` |
| Additional report fields | Built by report dialog, includes target and reason data |

Response model:

| Field | Usage |
| --- | --- |
| `error["0"]` | Error message |
| `data["0"]` | Success message |
| `time` | Server timestamp |

### Notifications

Fetch all notifications:

`GET {base}/nuke.php?__lib=noti&__output=8&__act=get_all`

Used for recent replies and combined notification/message lists.

Clear notifications:

`POST {base}/nuke.php?__lib=noti&raw=3&__act=del`

### Check In

`POST {base}/nuke.php?__lib=check_in&__act=check_in&lite=js`

The client extracts `{"0":"..."}` from the response. `签到成功` and `今天已经签到` both update the local last-check-in timestamp.

### User Profile / UCP

Display page:

`GET http://bbs.ngacn.cc/nuke.php?func=ucp&{params}`

JSON data:

`GET {base}/nuke.php?__lib=ucp&__act=get&lite=js&noprefix&{params}`

Headers include:

```text
Referer: {base}/nuke.php?func=ucp&lite=jsx&{params}
```

The JSON parser strips common malformed fragments and reads `data["0"]` for user profile, reputation, admin forums, sign, avatar, mute state, and metadata.

### Signature

`POST {base}/nuke.php`

Field map:

| Field | Value |
| --- | --- |
| `__lib` | `set_sign` |
| `__act` | `set` |
| `raw` | `3` |
| `lite` | `js` |
| `charset` | `gbk` |
| `uid` | User id |
| `sign` | Signature, GBK URL-encoded |

Success is detected by response text containing `操作成功`.

### Block Words / Blocked Users

Fetch remote block list:

`POST {base}/nuke.php`

Field map:

| Field | Value |
| --- | --- |
| `__lib` | `ucp` |
| `__act` | `get_block_word` |
| `__output` | `8` |
| `uid` | Current user id |

Headers:

```text
Referer: {base}/nuke.php?func=ucp&uid={uid}
```

Response `data["0"]` is split by line:

1. Version or marker
2. Space-separated filter words
3. Space-separated user ids

Update remote block list:

`POST {base}/nuke.php`

Field map:

| Field | Value |
| --- | --- |
| `__lib` | `ucp` |
| `__act` | `set_block_word` |
| `__output` | `8` |
| `data` | GBK URL-encoded payload |

Payload before URL encoding:

```text
1\r\n{word1 word2}\r\n{uid1 uid2}
```

### Private Messages

List messages:

`GET {base}/nuke.php`

Query map:

| Field | Value |
| --- | --- |
| `__lib` | `message` |
| `__act` | `message` |
| `act` | `list` |
| `lite` | `js` |
| `page` | Page number |

Read message thread:

`GET {base}/nuke.php`

Query map:

| Field | Value |
| --- | --- |
| `__lib` | `message` |
| `__act` | `message` |
| `act` | `read` |
| `lite` | `js` |
| `mid` | Message id |
| `page` | Page number |

Send or reply to message:

`POST {base}/nuke.php`

Query map:

| Field | Value |
| --- | --- |
| `__lib` | `message` |
| `__act` | `message` |
| `lite` | `js` |
| `charset` | `gbk` |
| `act` | Message action from caller |

Field map:

| Field | Usage |
| --- | --- |
| `mid` | Existing message thread id |
| `to` | Recipients, Chinese comma normalized to comma, GBK URL-encoded |
| `subject` | GBK URL-encoded subject |
| `content` | GBK URL-encoded content |

Success markers: `发送完毕 ...`, ` @提醒每24小时不能超过50个`, `操作成功`.

### Sub Board Subscribe / Hide

`POST http://bbs.ngacn.cc/nuke.php`

Query:

| Field | Value |
| --- | --- |
| `__lib` | `user_option` |
| `__act` | `set` |
| `raw` | `3` |
| `type` | Sub-board type |
| `__output` | `8` |
| `fid` | Parent forum id |
| `add` or `del` | Board id or thread id |

Action mapping:

| Type | Subscribe action | Unsubscribe/hide action |
| --- | --- | --- |
| `1` | `del` | `add` |
| Other | `add` | `del` |

Success is detected by response text containing `成功`.

### Vote

The WebView vote asset calls the native proxy, which forwards to:

`POST {base}/nuke.php?__lib=vote&raw=3&lite=js&__act={action}&tid={tid}&voteid={ids}`

Actions:

| Action | Usage |
| --- | --- |
| `vote` | Submit vote |
| `settle` | Settle/close vote |

The proxy uses `HttpPostClient` with the active cookie.

### Avatar Upload and Change

Legacy avatar image upload:

`POST http://app.myauth.us/api/attach.php?`

Multipart fields:

| Field | Value |
| --- | --- |
| `v2` | `1` |
| `attachment_file1_watermark` | Empty |
| `attachment_file1_dscp` | Empty |
| `attachment_file1_url_utf8_name` | Filename |
| `fid` | `-7` |
| `func` | `upload` |
| `attachment_file1_img` | `1` |
| `origin_domain` | `bbs.ngacn.cc` |
| `lite` | `js` |
| `attachment_file1` | Image file |

Change avatar:

`POST http://nga.178.com/nuke.php?`

Body:

| Field | Value |
| --- | --- |
| `lite` | `js` |
| `noprefix` | Present |
| `func` | `avatar` |
| `icon` | Avatar image URL, GBK URL-encoded |
| `__ngaClientChecksum` | Optional checksum field from `AvatarPostAction` |

Success marker: `操作成功 你可能需要重新登录以显示新的头像`.

### Static Image URLs

| Resource | URL Template |
| --- | --- |
| Board icon by fid | `http://img4.nga.178.com/ngabbs/nga_classic/f/app/{fid}.png` |
| Board icon by stid | `https://img4.nga.178.com/proxy/cache_attach/ficon/{stid}v.png` |
| Relative image fix | `./mon_` becomes `http://img6.nga.178.com/attachments/mon_` |
| Emoticons | `https://img4.nga.178.com/ngabbs/post/smile/` |

## Implementation Notes for the New Kotlin API Layer

1. Keep request construction testable by separating endpoint builders from the HTTP client.
2. Preserve GBK encoding behavior for post bodies, signatures, comments, private messages, board search, and block-list updates.
3. Use a central `NgaSession` for selected domain, cookie, and user-agent.
4. Normalize wrapped responses by stripping `window.script_muti_get_var_store=`, `/*error fill content...`, and `/*$js$*/` before JSON parsing.
5. Support unauthenticated read-only endpoints first; authenticated operations should return typed "missing cookie" errors instead of attempting requests silently.
6. Avoid fixed HTTP hosts where a selected HTTPS base domain can work. Keep legacy fixed hosts only for flows that are known to depend on them.
