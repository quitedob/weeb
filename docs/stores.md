# Application stores

The maintained stores are in `Vue/src/stores`. `index.js` exports `useAuthStore`, `useChatStore`, `useNotificationStore` and `useThemeStore`. `setup.js` installs Pinia and the local persistence plugin; `Vue/src/main.js` calls `initializeStores()` after mounting the application.

## Initialization and lifetime

`initializeStores()` restores authentication, starts the token refresh timer, initializes the theme and installs application watchers. Its immediate authentication watcher starts the shared chat connection, loads account preferences and starts unread-notification polling for an existing session as well as a new login. Logout disconnects the socket, stops notification polling and clears chat and notification state.

Page navigation does not own the shared WebSocket lifetime. Contacts and notifications receive their events through the chat store's subscriptions. Storage events synchronize the token and theme across tabs; visibility and network events refresh counts or reconnect as needed.

## Authentication and account preferences

`authStore.js` holds `accessToken`, `refreshToken`, `tokenExpiry`, `currentUser`, `isRefreshing`, `refreshPromise` and `preferences`. `isLoggedIn` and `isAuthenticated` are getters based on the access token. Tokens and the current user are stored in localStorage. Account privacy and notification settings are fetched from the server.

| Operation | Current entry point |
| --- | --- |
| Log in | `authStore.login({username, password, rememberMe})` |
| Refresh the current user | `authStore.fetchUserInfo()` |
| Update cached user data | `authStore.setCurrentUser(user)` |
| Refresh the token | `authStore.refreshAccessToken()` |
| Log out through the API and clear local state | `authStore.logout()` |
| Clear local authentication without an HTTP request | `authStore.logoutCleanup()` |
| Load saved account settings | `authStore.loadPreferences()` |
| Save privacy or notification preferences for the current session | `authStore.savePrivacyPreferences(section)`, `authStore.saveNotificationPreferences(section)` |

`needsRefresh` becomes true during the final five minutes before token expiry. A timer checks it every minute; simultaneous refresh calls share a promise. The current backend refresh contract uses the existing token in the Authorization header.

Registration and profile writes use API modules directly: `api.auth.register(data)` and `api.user.updateCurrentUser(data)`. Avatar upload uses `api.user.uploadAvatar(formData)` with multipart field `file`; the response data contains `avatar`. These are not auth-store actions. The backend token validation endpoint is `POST /api/auth/validate`.

`api.user.getSettings()` reads `GET /api/users/me/settings`. `savePrivacySettings(section)` and `saveNotificationSettings(section)` send PUT requests to its `/privacy` and `/notifications` subpaths. Every field in the supplied section must be a JSON Boolean:

```json
{
  "privacy": {"onlineVisible": true, "allowMessages": true, "showFollows": true},
  "notifications": {"newMessages": true, "follows": true, "likes": true, "comments": true, "groupInvites": true}
}
```

The server derives the account from authentication. Saving one section preserves the other section. All eight preferences default to true for accounts without a saved row.

Settings uses the guarded store save actions. Session and section versions prevent late responses from a previous account or an older request from replacing current preferences; the UI only reports success for a save still belonging to the active session.

## Chat

`chatStore.js` holds `activeChatSession`, `recentSessions`, `chatMessages`, `chatPagination`, `unreadCounts` and `connectionStatus`. Message and pagination collections are objects keyed by chat ID. `currentChatId`, `messagesForCurrentChat`, `totalUnreadCount`, `isConnected` and `canLoadMoreMessages` are getters.

| Operation | Current entry point |
| --- | --- |
| Connect or disconnect the shared transport | `connectWebSocket()`, `disconnectWebSocket()` |
| Load recent conversations | `fetchRecentChats()` |
| Select a conversation | `setActiveChat(session)` |
| Fetch message history | `fetchMessagesForChat(chatId, page = 1, limit = null)` |
| Fetch the next history page | `loadMoreMessages()` |
| Send through the active conversation | `sendMessage(content, targetId, chatType = 'PRIVATE', messageType = 1)` |
| Refresh unread counts | `fetchUnreadStats()` |
| Persist a read marker | `markChatAsRead(chatId)` |
| Persist several read markers | `batchMarkAsRead(chatIds)` |

History pages start at 1, with a default store batch size of 50. Sending creates a temporary message; server confirmations reconcile its status. A conversation's shared ID, a user's chat-list row ID and a target user ID are distinct values. Use the IDs returned by the chat APIs rather than constructing one from another.

Conversation creation, deletion, recall and reactions are API-module operations:

| API helper | HTTP contract |
| --- | --- |
| `api.chat.createChat({targetId})` | `POST /api/chats` |
| `api.chat.deleteChat(chatId)` | `DELETE /api/chats/{chatId}` |
| `api.chat.recallMessage(messageId)` | `DELETE /api/chats/messages/{messageId}` |
| `api.chat.addReaction(messageId, reactionType)` | `POST /api/chats/messages/{messageId}/react?reactionType=...` |

### STOMP destinations

The client sends private messages to `/app/chat/private` and group messages to `/app/chat.sendMessage`, with group room IDs formatted as `group_<groupId>`. The server determines the sender from the authenticated principal and checks conversation membership.

The shared connection subscribes to these authenticated destinations:

```text
/user/queue/private
/user/queue/chat-list-update
/user/queue/message-status
/user/queue/read-receipt
/user/queue/group-member-change
/user/queue/group-info-change
/user/queue/reaction-change
/user/queue/errors
/user/queue/notifications
/user/queue/contacts
```

Both private and group message deliveries arrive on the private user queue. Do not insert a username into these subscription paths or subscribe to wildcard topics. The server's `convertAndSendToUser(username, ...)` addressing is different from the client's subscription syntax.

## Notifications

`notificationStore.js` holds `notifications`, `unreadCount`, `currentPage`, `totalPages`, `pageSize`, `isLoading`, `lastFetchTime` and the refresh interval. Notifications use `isRead`; the backend model also provides `id`, `type`, `entityType`, `entityId`, `actorId`, `recipientId` and `createdAt`.

```javascript
const notifications = useNotificationStore();
await notifications.fetchNotifications(1, 10);
await notifications.markAsRead(notificationId);
await notifications.markAllAsRead();
await notifications.deleteReadNotifications();
```

`fetchNotifications(page = 1, pageSize = 10)` replaces the first page and appends subsequent pages. `fetchUnreadCount()` reads the count separately. `startAutoRefresh(interval = 30000)` polls that count, and `stopAutoRefresh()` cancels it. Incoming socket events call `addNotification(notification)`.

The HTTP module uses `GET /api/notifications`, `GET /api/notifications/unread-count`, `POST /api/notifications/{id}/read`, `POST /api/notifications/read-all` and `DELETE /api/notifications/read`. The store does not implement a timed 30-day retention policy.

## Theme

`themeStore.js` is a setup-style Pinia store. It exports `isDark`, `currentTheme`, `userPreference`, `initTheme()`, `setTheme(theme)` and `toggleTheme()`. `setTheme` accepts `light`, `dark` or `system`; `currentTheme` resolves the actual light/dark appearance. `initTheme()` reads `theme-preference` from localStorage and observes the system color scheme. There is no separate public `watchSystemTheme()` action.

The header's theme control uses this store. Theme tokens are defined in `Vue/src/assets/apple-style.css`, imported by `main.css`; the store updates the root `data-theme` attribute and body theme classes.

## Errors, tests and retained prototypes

API modules return `{code, message, data}` through `axiosInstance.js`; success is `code === 0`. Business errors reject. HTTP 401 or the backend's unauthorized code clears local authentication without a nested logout request. HTTP 403 and ordinary business failures preserve the session. Mutating HTTP requests are not automatically retried. Component-level loading and error feedback still belong to the calling view.

Run `npm run test:run` from `Vue/`. The store lifecycle and API-contract suites cover restored-session startup, connection reuse, subscription destinations and error handling. See [the remediation record](remediation-plan.md) for completed verification and remaining integration work.

There is no exported `newChatStore` in the maintained store registry. Unrouted thread UI prototypes are archived under `docs/legacy/thread-prototypes`. `Vue/src/api/modules/messageThread.js` and backend `/api/threads` handlers remain, but the SQL create scripts do not provide `message_threads` or `thread_participants`; this is not a supported feature of a fresh deployment.
