  Mode: Full audit (explicitly requested). Read-only —no files were modified.
  Verified by me: backend mvn -o compile → exit 0; frontend vite build → ✓ built in 13.87s. Both halves build. Every
  finding below was re-checked against source; nothing is reported from the workers' word alone.

  ---
  1. The system does build —but ./mvnw cannot run

  ┌───────────────────────────────────────────┬─────────────────────────────────────────────────────────────────────┐
  │                   Check                   │                               Result                                │
  ├───────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────┤
  │ mvn -o compile (system Maven 3.9.9 / JDK  │ ✅ exit 0                                                           │
  │ 17)                                       │                                                                     │
  ├───────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────┤
  │ npx vite build                            │ ✅ 13.87 s                                                          │
  ├───────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────┤
  │ ./mvnw compile                            │ ❌ ./.mvn/wrapper/maven-wrapper.properties: No such file or         │
  │                                           │ directory                                                           │
  └───────────────────────────────────────────┴─────────────────────────────────────────────────────────────────────┘

  .mvn/ does not exist and is excluded by .gitignore:7 and :36. git log --all -- .mvn returns nothing —the wrapper
  scripts mvnw / mvnw.cmd were committed broken from the first commit. Anyone cloning this repo and running the
  documented Maven command is blocked. Either commit .mvn/wrapper/maven-wrapper.properties (+ .jar) or delete both
  wrapper scripts.

  ---
  2. HIGH —Security defects

  2.1 Admin is decided by the username string →free privilege escalation

  src/main/java/com/web/service/Impl/UserTypeSecurityServiceImpl.java:191-199
  return username.startsWith("admin_") || username.startsWith("root_") ||
         username.startsWith("sys_") || username.equals("admin") ||
         username.equals("root") || username.equals("system") ||
         username.contains("_admin");
  This feeds isAdmin(), which gates /api/admin/** at security/SecurityConfig.java:89-93 and grants ROLE_ADMIN at
  UserSecurityServiceImpl.java:133. Registration is public (/api/auth/** is permitAll, SecurityConfig.java:77) and
  SecurityConstants.java:34 permits ^[a-zA-Z0-9_]{3,50}$.

  Anyone can register evil_admin and obtain admin. This unlocks the only two @PreAuthorize-protected controllers in the
  codebase, and the admin bypasses in GroupServiceImpl:81, ArticleServiceImpl:106,
  UnifiedMessageServiceImpl:663/750/775, GroupPermissionService:108/126/167/225. Fix: persist a real role column; delete
  every username-pattern check.

  2.2 Only 14 endpoints out of ~22 controllers have any method-level authorization

  @PreAuthorize —only in RateLimitController (9×)and WebSocketMonitorController (5×)
  @Secured / @RolesAllowed —zero occurrences
  @EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true) is switched on
  (SecurityConfig.java:34), so the annotation would work —it is simply absent everywhere else. Consequences, all
  confirmed:

  Endpoint: POST /api/users/{userId}/ban
  File:line: StandardUserController.java:233
  Impact: any user bans any account
  ────────────────────────────────────────
  Endpoint: POST /api/users/{userId}/unban
  File:line: :251
  Impact: any user unbans
  ────────────────────────────────────────
  Endpoint: POST /api/users/{userId}/reset-password
  File:line: :269
  Impact: any user resets the admin's password
  ────────────────────────────────────────
  Endpoint: DELETE /api/articles/{id}/admin
  File:line: ArticleCenterController.java:492
  Impact: any user deletes any article
  ────────────────────────────────────────
  Endpoint: POST /api/articles/{id}/addcoin?amount=
  File:line: :191
  Impact: @RequestParam Double amount, no sign/range check →mint or debit freely
  ────────────────────────────────────────
  Endpoint: /api/content-reports/{pending,process,batch-process,statistics}
  File:line: ContentReportController.java:62,83,115,177,251,274
  Impact: any user moderates content, reads reporter identities

  2.3 Chat: read-IDOR, write forgery, and a stub subscription check

  - Read any private conversation. ChatController.java:101 GET /api/chats/{sharedChatId}/messages →
  ChatServiceImpl.java:510-521 →messageMapper.selectMessagesBySharedChatId. No participant check. sharedChatId is a
  Long —increment the integer and read everyone's DMs. (Contrast deleteChatBySharedChatId:742, which does check
  ownership.)
  - Forge messages into any chat. WebSocketMessageController.java:347-348 takes chatId straight from the client payload;
  ChatServiceImpl.java:239-258 looks it up and inserts with no participant check. chat_list.id is deterministic —
  String.valueOf(sharedChatId) + "_" + userId (ChatServiceImpl.java:478,492) —so "5_7" is guessable.
  - Subscribe to any room. Config/SpringWebSocketConfig.java:247-251
  private boolean hasChatRoomAccess(String username, String roomId) {
      return true; // 简化实现，实际需要查询数据库
  }
  - /ws/**, /topic/**, /app/**, /user/** are all permitAll (SecurityConfig.java:79-86). Real-time eavesdropping on any
  chat or group.

  2.4 Committed credentials and private keys

  git ls-files confirms these are tracked, and no .gitignore rule covers them:

  ┌───────────────────────────────────────────────┬─────────────────────────────────────────────────┐
  │                   File:line                   │                     Secret                      │
  ├───────────────────────────────────────────────┼─────────────────────────────────────────────────┤
  │ src/main/resources/application.yml:15         │ MySQL password: [REDACTED: MySQL password] │
  ├───────────────────────────────────────────────┼─────────────────────────────────────────────────┤
  │ :110                                          │ weeb.password: [REDACTED: group password] │
  ├───────────────────────────────────────────────┼─────────────────────────────────────────────────┤
  │ :123                                          │ jwt.secret: [REDACTED: JWT signing key] │
  ├───────────────────────────────────────────────┼─────────────────────────────────────────────────┤
  │ :142                                          │ DeepSeek api-key: [REDACTED: DeepSeek API key] │
  ├───────────────────────────────────────────────┼─────────────────────────────────────────────────┤
  │ src/main/resources/es/http.p12, transport.p12 │ PKCS#12 keystores with private keys             │
  └───────────────────────────────────────────────┴─────────────────────────────────────────────────┘

  No application-dev.yml / application-prod.yml exists, so switching spring.profiles.active=prod still loads all of
  them. The JWT key is the worst: anyone with repo access can mint a valid token for any userId. Rotate all four, move
  to env vars, purge history.

  2.5 Passwords are returned by the API

  model/User.java:21 —private String password; with no @JsonIgnore, and Mapper/UserMapper.xml:75,102 selects u.password
  into the result map. StandardAuthController.java:78,102 (register/login) and StandardUserController.java:150,163 (by
  id / by username) return it. BCrypt hashes of every user, including admins, are disclosed to any authenticated caller.
  Fix: @JsonIgnore, or map to a response DTO.

  2.6 Logout and password-change do not invalidate tokens

  util/JwtUtil.java:268-290 —isTokenBlacklisted() return false;, blacklistToken() and blacklistAllUserTokens() are
  empty bodies. StandardAuthController.java:125 (logout) and :237 (change password) call these no-ops. With
  jwt.expiration: 86400000, every stolen token stays valid for 24 h —including across a password change. Fix: Redis
  deny-list keyed by jti with TTL.

  2.7 Password-reset token is a full access token, and it is logged

  AuthServiceImpl.java:461-468 —generateToken(...) then log.warn("…令牌:{}", resetToken). The reset credential is
  byte-identical to a 24 h access token, carries no scope/purpose claim, is never single-use, and lands in
  logs/weeb.log. Fix: dedicated short-TTL single-use nonce; never log it.

  2.8 XSS —marked() piped into v-html with no sanitizer

  dompurify is a declared dependency (Vue/package.json) but is applied at exactly one of 12 v-html sites
  (article/ArticleRead.vue:210). Unprotected:

  ┌───────────────────────────────────────────────────────────────────────────┬─────────────────────────────────────┐
  │                                 File:line                                 │               Content               │
  ├───────────────────────────────────────────────────────────────────────────┼─────────────────────────────────────┤
  │ article/ArticleWrite.vue:104 ←:151 marked(form.articleContent)           │ live write-preview                  │
  ├───────────────────────────────────────────────────────────────────────────┼─────────────────────────────────────┤
  │                                                                           │ loads existing content from the     │
  │ article/ArticleEdit.vue:109 ←:168                                        │ server →stored XSS fires in the    │
  │                                                                           │ editor                              │
  ├───────────────────────────────────────────────────────────────────────────┼─────────────────────────────────────┤
  │ article/ArticleModeration.vue:149                                         │ renders other users' content for a  │
  │                                                                           │ moderator                           │
  ├───────────────────────────────────────────────────────────────────────────┼─────────────────────────────────────┤
  │ auth/HelpCenter.vue:322, components/message/MessageThread.vue:55,         │ currently unreferenced, but         │
  │ components/article/ArticleVersions.vue:88,98,102                          │ landmines                           │
  └───────────────────────────────────────────────────────────────────────────┴─────────────────────────────────────┘

  Fix: route every marked() result through DOMPurify.sanitize() as ArticleRead.vue already does.

  ---
  3. HIGH —Core functions that are broken

  3.1 The frontend tests the wrong success code, everywhere

  The canonical envelope is ApiResponse{code,message,data}, where code: 0 = success (common/ApiResponse.java:20-22). The
  interceptor at api/axiosInstance.js:66-91 rejects any code !== 0 and returns the envelope. So a successful call
  always arrives with code === 0. These call sites test something else:

  ┌─────────────────────────────────────────┬──────────────────────────┬────────────────────────────────────────────┐
  │                File:line                │           Test           │                   Result                   │
  ├─────────────────────────────────────────┼──────────────────────────┼────────────────────────────────────────────┤
  │                                         │ if (response.success) — │ every successful signup shows 注册成功 as  │
  │ views/Register.vue:219                  │ always undefined         │ a red error and never redirects to /login. │
  │                                         │                          │  Registration is effectively broken.       │
  ├─────────────────────────────────────────┼──────────────────────────┼────────────────────────────────────────────┤
  │                                         │                          │ like / comment / delete-comment /          │
  │ article/ArticleRead.vue:246,307,330,383 │ response.code === 200    │ favourite all report failure after         │
  │                                         │                          │ succeeding; list never refreshes           │
  ├─────────────────────────────────────────┼──────────────────────────┼────────────────────────────────────────────┤
  │ views/Settings.vue:255,295              │ response.code === 200    │ profile save shows "更新失败: 操作成功"    │
  │                                         │                          │ and the store never updates                │
  ├─────────────────────────────────────────┼──────────────────────────┼────────────────────────────────────────────┤
  │ views/UserDetail.vue:281                │ response.data.code ===   │ reads into the payload after the envelope  │
  │                                         │ 200                      │ was already unwrapped →undefined          │
  └─────────────────────────────────────────┴──────────────────────────┴────────────────────────────────────────────┘

  3.2 Follow, avatar, and group-approval call endpoints that do not exist

  Call: POST /api/user/{id}/follow
  Evidence: views/UserDetail.vue:279
  Backend reality: base path is /api/users (StandardUserController.java:29-31) →404
  ────────────────────────────────────────
  Call: POST /api/users/avatar
  Evidence: api/modules/user.js:105
  Backend reality: grep for /avatar in src/main/java returns nothing →404. Only avatar write is PUT /api/users/me.
  ────────────────────────────────────────
  Call: POST .../applications/{id}/approve and /reject
  Evidence: api/modules/group.js:174,187
  Backend reality: backend exposes only PUT /api/groups/{gid}/applications/{aid} (StandardGroupController.java:265) →
    405. Group owners can never approve a join request.
  ────────────────────────────────────────
  Call: GET /api/user-follows/count/*
  Evidence: api/modules/user.js:80,85
  Backend reality: user-follows appears nowhere in src/main/java →404
  ────────────────────────────────────────
  Call: GET /api/users/{id}/following|followers
  Evidence: api/modules/user.js:64-71
  Backend reality: handlers are stubs returning the string "获取关注列表成功" (StandardUserController.java:351,368)

  3.3 Global WebSocket dies when you leave the chat page

  views/chat/ChatPage.vue:1412-1418 —onUnmounted calls chatStore.disconnectWebSocket(). The STOMP connection is an
  app-wide singleton (stores/index.js). Navigate /chat →/article and real-time messages, the unread badge, and
  NotificationBell silently stop for the rest of the session.

  3.4 Two schedulers race on the same rows at 02:00

  scheduled/ContactCleanupTask.java:29    @Scheduled(cron = "0 0 2 * * ?")  →hard-deletes expired contacts
  task/ContactRequestCleanupTask.java:32  @Scheduled(cron = "0 0 2 * * ?")  →marks the same rows EXPIRED
  Same minute, same rows, one deletes and one updates; outcome is order-dependent. The delete path is also an unbounded
  selectList full-table scan.

  ---
  4. Structure —the things you asked about

  4.1 Root of the repository

  ┌─────────────────────────────────┬───────────────────────────────────────────────────────────────────────────────┐
  │              Item               │                                    Verdict                                    │
  ├─────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────┤
  │ nul —46 bytes, content is      │ Delete. nul is a reserved Windows device name; a shell redirect > nul created │
  │ /usr/bin/bash: line 1: del:     │  it. It's untracked and .gitignore:66 already covers it. Remove via extended  │
  │ command not found               │ path: rm "//?/D:/java/project/weeb/nul"                                       │
  ├─────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────┤
  │                                 │ Delete both. Dead duplicate declaring only element-plus/less/less-loader.     │
  │ package.json, package-lock.json │ There is no root node_modules, nothing resolves them. The real manifest is    │
  │                                 │ Vue/package.json.                                                             │
  ├─────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────┤
  │ mvnw, mvnw.cmd                  │ Broken (§1)—fix or delete                                                   │
  ├─────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────┤
  │                                 │ Copy-paste from unrelated projects: fishtts_cloned_voices/, livetalking.log,  │
  │ .gitignore                      │ rag_data/, mem0_db/, chroma_db/. Also duplicate entries —.idea/ at lines     │
  │                                 │ 5/34/37, .mvn/ at 7/36, *.log twice. Needs a rewrite.                         │
  └─────────────────────────────────┴───────────────────────────────────────────────────────────────────────────────┘

  4.2 Backend folder naming —three separate problems

  a) Capitalized Java packages. Config/ (20 files), Controller/ (23), service/Impl/ (41). Every file declares e.g.
  package com.web.Config;. These are the only capitalized packages in a tree where constant, util, mapper, service are
  all lowercase. Target: config, controller, service.impl.

  ▎ ⚠️Case-only renames silently no-op on Windows/macOS. Use two steps: git mv Config config_tmp && git mv config_tmp
  ▎ config.

  b) Duplicate packages that should merge. These are near-empty duplicates of an existing package:

  ┌────────────┬─────────────────────────────────────────────┬────────────┐
  │  Package   │                    File                     │ Merge into │
  ├────────────┼─────────────────────────────────────────────┼────────────┤
  │ constants/ │ GroupRoleConstants.java                     │ constant/  │
  ├────────────┼─────────────────────────────────────────────┼────────────┤
  │ utils/     │ SqlFileLoader.java                          │ util/      │
  ├────────────┼─────────────────────────────────────────────┼────────────┤
  │ schedule/  │ ExpiredClearTask.java —100 % commented out │ delete     │
  ├────────────┼─────────────────────────────────────────────┼────────────┤
  │ scheduled/ │ ContactCleanupTask.java                     │ task/      │
  ├────────────┼─────────────────────────────────────────────┼────────────┤
  │ runner/    │ empty directory                             │ delete     │
  └────────────┴─────────────────────────────────────────────┴────────────┘

  So you have constant and constants, util and utils, and schedule and scheduled and task. Classifying
  sched/task/schedule purely by suffix is guesswork for a newcomer.

  c) resources/Mapper/ —capital M. Coupled to application.yml:29 mapper-locations: classpath*:/Mapper/*.xml. Java side
  is correctly lowercase (@MapperScan("com.web.mapper"), WeebApplication.java:14). Rename to mapper/ + update the yml.
  Missing this yields runtime "Invalid bound statement", not a compile error.

  4.3 Backend dead code and stray files

  util/ResultUtil.java (0 refs, superseded by ApiResponse), service/DebugService.java (interface, no impl),
  service/OfflineMessageService.java, service/TokenRefreshService.java, service/ArticleModerationService.java (all 0
  refs), schedule/ExpiredClearTask.java, vo/user/UpdateUserVo.java.bak, empty runner/, and src/backend.md sitting loose
  under src/.

  4.4 Backend naming

  - Standard* prefix is redundant —StandardAuthController, StandardGroupController, StandardUserController. No
  non-Standard twins exist to disambiguate. Drop it.
  - VOs that lost their domain: vo/chatList/{CreateVo,DeleteVo,ReadVo}.java,
  vo/video/{AcceptVo,AnswerVo,CandidateVo,HangupVo,InviteVo,OfferVo}.java. Unsearchable in a shared vo/ tree. Also the
  package vo/chatList is the only camelCase package segment (contrast vo/userlevel).
  - Config/SecurityConstants.java is a constants class in Config/ while constant/ exists.

  4.5 Frontend structure

  a) Vue/src/Chat/ is a 100 % dead 200 KB duplicate of Vue/src/views/chat/. Nothing imports it (grep for @/Chat/,
  ../Chat/ →zero hits outside itself); the router resolves ../views/chat/ChatPage.vue (router/index.js:19,25). It also
  contains the only git-tracked .backup file in the tree (ChatPage.vue.backup, 67 KB).

  ▎ Caveat: the dead copy is newer (Nov 7) than the routed one and holds the only paginated emoji-picker implementation.
  ▎ Diff before deleting.

  b) Two constant folders, different files, one dead. Vue/Constant/emoji/ is live (@constant alias →vite.config.js:39);
  Vue/src/constant/emoji/ is dead. MD5s differ (d4d6ad60…vs 7fb1dfea…)—they are not copies, so don't merge, just
  delete the dead one and re-point the alias at src/constant.

  c) 11 dead Apple* design-system components (~91 KB), zero import sites: AppleTable (15.6 KB), AppleTabs (10.9),
  AppleTag (7.7), ApplePagination (9.7), AppleGrid (3.9), AppleOption (2.3), AppleDropdownItem (2.9), plus AppleTabPane,
  AppleDropdown, AppleSelect, AppleCol which are reachable only from other dead files. Live ones: AppleButton (8
  sites), AppleCard (5), AppleInput, AppleModal, AppleSwitch, AppleTextarea, AppleMessage.

  d) Three MessageThread.vue files, all dead —components/message/MessageThread.vue,
  components/message/MessageThreadDisplay.vue, Chat/components/MessageThread.vue. Grep for messageThread in templates
  finds nothing. The threaded-reply feature was abandoned.

  e) Orphan folders. src/value/, src/video/ (and src/video/Video.vue:19 links to /usermain, a route that doesn't exist),
  src/auth/ (dead twin of src/views/), src/views/admin/ (empty, yet router/admin-routes.js:8,18 imports two files from
  it that don't exist).

  f) Mixed homes for routed pages. The router imports some pages from src/<feature>/ and others from src/views/. The
  group feature is scattered across four locations: src/group/{GroupPage,GroupDetailPage}.vue, src/views/Groups.vue,
  src/views/group/GroupDetail.vue, src/views/GroupManagement.vue. And Chat is PascalCase while every sibling feature
  folder is lowercase.

  g) Toolchain ghosts. vite.config.js:29-33 configures a less preprocessor and the dead root package.json declares
  less/less-loader —but Vue/package.json has no less, and the only lang="less" file is the dead ChatPage.vue. One root
  package.json is load-bearing for a toolchain the live project doesn't have.

  h) Docs and artifacts in the wrong place. Vue/frontend.md (47 KB) in the Vite build root; Vue/src/stores/README.md
  loose in a source folder; Vue/public/debug-commands.js (11.7 KB) is copied verbatim into dist/ and ships to
  production. jsconfig.json:4 declares only @/* —the @constant alias is missing, so IDEs can't resolve it.

  i) Test harness with zero tests. vitest + jsdom + @vue/test-utils are installed, vitest.config.js exists,
  test/test:run scripts exist —and find -name "*.spec.js" -o -name "*.test.js" returns nothing.

  ---
  5. UX / UI

  ┌─────┬─────────────────────────────────────────────────────────────────────────────────────────────────┬──────────┐
  │  #  │                                             Finding                                             │ Severity │
  ├─────┼─────────────────────────────────────────────────────────────────────────────────────────────────┼──────────┤
  │     │ No theme toggle is reachable. ThemeToggle.vue is imported only by views/ThemeDemo.vue, which    │          │
  │ 5.1 │ isn't routed. themeStore.js implements light/dark/system and main.css ships a                   │ MED      │
  │     │ [data-theme="dark"] block —the user simply has no way to switch.                               │          │
  ├─────┼─────────────────────────────────────────────────────────────────────────────────────────────────┼──────────┤
  │     │ AppleSwitch is keyboard-inaccessible. AppleSwitch.vue:2 is a <div @click> with no role,         │          │
  │ 5.2 │ tabindex, or key handler. It's live on every privacy and notification toggle in                 │ MED      │
  │     │ views/Settings.vue:55,62,69,85,92,99,106,113. A keyboard or screen-reader user cannot change    │          │
  │     │ any setting. Same pattern in AppleOption, AppleSelect, AppleTag.                                │          │
  ├─────┼─────────────────────────────────────────────────────────────────────────────────────────────────┼──────────┤
  │     │ AppleModal is not a dialog. AppleModal.vue:5 is a plain <div>; focusModal() calls .focus() on a │          │
  │ 5.3 │  non-focusable element (:209-215) —a silent no-op. No role="dialog", no aria-modal, no focus   │ MED      │
  │     │ trap (Tab walks behind the overlay). Escape and focus-restore are correct, which makes the gap  │          │
  │     │ conspicuous.                                                                                    │          │
  ├─────┼─────────────────────────────────────────────────────────────────────────────────────────────────┼──────────┤
  │     │ Dark mode is broken where it matters most. views/chat/ChatPage.vue hardcodes 93 hex colours in  │          │
  │ 5.4 │ scoped styles (next: Chat/ChatPage.vue 68, views/UserLevelHistory.vue 27), while the rest of    │ MED      │
  │     │ the app uses var(--apple-*) (132 uses in assets/main.css).                                      │          │
  ├─────┼─────────────────────────────────────────────────────────────────────────────────────────────────┼──────────┤
  │ 5.5 │ ~35 native alert()/confirm() calls bypass the Apple design system on primary flows              │ LOW      │
  │     │ (views/UserDetail.vue:282, views/chat/ChatPage.vue:883, views/Settings.vue:284-290).            │          │
  ├─────┼─────────────────────────────────────────────────────────────────────────────────────────────────┼──────────┤
  │ 5.6 │ Stale page shell —index.html:2 has empty lang="" and <title>Vite App</title>.                  │ LOW      │
  └─────┴─────────────────────────────────────────────────────────────────────────────────────────────────┴──────────┘

  Verified NOT problems (checked, clean): send button is correctly :disabled="!canSendMessage" and forms carry :loading
  —no double-submit on a live form. MessageList.vue / VirtualMessageList.vue contain no v-html —chat messages are
  escaped. All router/index.js imports resolve. notificationStore.js:45,72 matches the backend's keys. persistPlugin.js
  correctly overwrites storage on logout —no cross-user token leak. <img> tags on routed pages carry alt.

  ---
  6. Documentation reliability

  Only 5 .md files are tracked. Treat them as claims, not truth:

  Document: src/backend.md (788 L)
  Verdict: Mostly reliable. ~120 endpoints spot-checked; 12 of 12 sampled base paths verified. Errors: claims 22
    controllers, actual 23 (:30,:595,:772); documents a phantom POST /api/users/avatar (:72) that doesn't exist; its own

    summary lists 24 sections and double-counts Contact.
  ────────────────────────────────────────
  Document: Vue/frontend.md (1345 L)
  Verdict: Unreliable —do not trust. Self-contradicts three ways on the Apple component count (24 / 19 / actual 23).
    Documents 18 backend API paths that do not exist (§2above is largely downstream of it). Claims ESLint + Prettier —
    neither is installed (zero occurrences in Vue/package.json, no config files). Documents 2 admin pages whose
  directory
     views/admin/ is empty.
  ────────────────────────────────────────
  Document: src/main/resources/sql/README.md
  Verdict: Table inventory accurate (28 ✓). Documents a migration/ directory that does not exist. Internal count errors:

    "文章模块 8张" then lists 9; "系统管理 4张" then lists 3.
  ────────────────────────────────────────
  Document: src/main/resources/es/README.md
  Verdict: Fine as an ops guide, except line 1 is corrupted: git add .# Elasticsearch 配置指南. Also documents GET
    /api/search/users?q= but the controller requires keyword (SearchController.java:284) —its own curl example would
    fail.
  ────────────────────────────────────────
  Document: Vue/src/stores/README.md
  Verdict: Genuinely good. Its useXxxStore convention is honored by all 5 stores.
  ────────────────────────────────────────
  Document: .kiro/
  Verdict: Empty directory, zero files. .claude/ holds only a permissions file. No CLAUDE.md anywhere.

  ---
  7. Recommended fix order

  Do first —security (each is independent):
  1. Rotate all committed secrets; purge from history; move to env vars. (§2.4)
  2. Replace username-pattern admin detection with a real role column. (§2.1)
  3. Add @PreAuthorize("hasRole('ADMIN')") to the 6 unguarded admin-shaped endpoints. (§2.2)
  4. Add participant checks to getChatMessagesBySharedChatId, the WS send path, and hasChatRoomAccess. (§2.3)
  5. @JsonIgnore on User.password. (§2.5)
  6. DOMPurify.sanitize() on the marked() sites. (§2.8)

  Then —function is broken for real users:
  7. code === 0 in Register.vue:219, ArticleRead.vue ×4,Settings.vue ×2,UserDetail.vue:281. (§3.1)
  8. Fix the 5 phantom endpoint calls + message.js reaction-param transport. (§3.2)
  9. Move WebSocket lifecycle out of ChatPage.vue into the layout. (§3.3)
  10. Merge the duplicate 02:00 contact schedulers. (§3.4)

  Then —structure, lowest risk first (one commit each):
  11. Delete: nul, root package.json/package-lock.json, dead code + .bak + empty runner/, Vue/src/Chat/,
  Vue/src/constant/, 11 dead Apple*, 3 MessageThread*, Vue/src/value/, Vue/src/utils/elementMigrationHelper.js.
  12. Fix or commit mvnw; rewrite .gitignore.
  13. Merge constants→constant,utils→util,scheduled→task;move SecurityConstants to constant/.
  14. resources/Mapper/ →mapper/ + update application.yml:29.
  15. Create docs/; move src/backend.md, Vue/frontend.md, Vue/src/stores/README.md there.
  16. Last, alone: case-only package renames (Config/Controller/Impl) via two-step git mv, plus the features/
  consolidation.

  Two things to confirm with you before deleting: the dead Vue/src/Chat/ tree holds the only paginated emoji-picker
  implementation, and the dead MessageThread components are the only threaded-replies implementation. Both look
  abandoned, but they are real product surface —worth a look before they go.

  ---
  Scope covered: 538 tracked files —all of src/main/java (323 Java files), all of Vue/src, root config, all 5 tracked
  docs.
  Not covered: no runtime/browser execution (no app was started; no database, Redis, or Elasticsearch was contacted).
  Frontend findings are static-analysis + build-verified; every API-contract mismatch is confirmed on both sides by
  source, not by a live request. Accessibility findings are from source inspection, not screen-reader testing.
