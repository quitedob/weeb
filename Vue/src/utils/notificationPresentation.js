// The server emits lower-case social events and upper-case contact/group events.
const aliases = { FOLLOW: 'NEW_FOLLOWER', LIKE: 'ARTICLE_LIKE', COMMENT: 'ARTICLE_COMMENT' }

export const notificationType = type => {
  const key = String(type || '').toUpperCase()
  return aliases[key] || key
}

const labels = {
  ARTICLE_LIKE: ['文章点赞', '有人点赞了你的文章', '👍'],
  ARTICLE_COMMENT: ['评论', '有人评论了你的文章', '💬'],
  ARTICLE_FAVORITE: ['文章收藏', '有人收藏了你的文章', '⭐'],
  COMMENT_MENTION: ['评论提及', '有人在评论中提到了你', '💬'],
  NEW_FOLLOWER: ['新关注者', '有人关注了你', '👤'],
  FRIEND_REQUEST: ['好友申请', '你收到一条好友申请', '🤝'],
  FRIEND_ACCEPTED: ['好友申请通过', '你的好友申请已通过', '✅'],
  FRIEND_REJECTED: ['好友申请拒绝', '你的好友申请被拒绝', '❌'],
  CONTACT_UPDATED: ['联系人更新', '你的联系人已更新', '🔄'],
  CHAT_MESSAGE: ['聊天消息', '你有一条新消息', '💬'],
  PRIVATE_MESSAGE: ['私信', '你收到一条私信', '✉️'],
  GROUP_MESSAGE: ['群组消息', '你有一条新群组消息', '👥'],
  GROUP_INVITE: ['群组邀请', '你收到一条群组邀请', '📨'],
  GROUP_JOIN: ['群组成员', '群组有新成员加入', '🎉'],
  GROUP_LEAVE: ['群组成员', '有成员退出了群组', '👋'],
  GROUP_APPLICATION: ['入群申请', '你收到一条入群申请', '📨'],
  GROUP_APPLICATION_APPROVED: ['入群申请通过', '你的入群申请已通过', '✅'],
  GROUP_APPLICATION_REJECTED: ['入群申请拒绝', '你的入群申请被拒绝', '❌'],
  SYSTEM: ['系统通知', '你有一条系统通知', '🔔'],
}

export const getNotificationTypeText = type => labels[notificationType(type)]?.[0] || '通知'
export const getNotificationText = notification => notification.content || labels[notificationType(notification.type)]?.[1] || '你有一条新通知'
export const getNotificationIcon = type => labels[notificationType(type)]?.[2] || '📢'

export function getNotificationRoute(notification) {
  const type = notificationType(notification.type)
  const entityId = notification.entityId
  const actorId = notification.actorId
  switch (type) {
    case 'ARTICLE_LIKE':
    case 'ARTICLE_COMMENT':
    case 'ARTICLE_FAVORITE':
    case 'COMMENT_MENTION':
      return entityId ? `/article/read/${entityId}` : '/notifications'
    case 'NEW_FOLLOWER':
    case 'FRIEND_ACCEPTED':
      return actorId ? `/user/${actorId}` : '/notifications'
    case 'FRIEND_REQUEST':
      return '/contact?tab=requests'
    case 'CONTACT_UPDATED':
      return '/contact'
    case 'CHAT_MESSAGE':
    case 'PRIVATE_MESSAGE':
      return entityId ? `/chat/private/${entityId}` : '/chat'
    case 'GROUP_MESSAGE':
      return entityId ? `/chat/group/${entityId}` : '/chat'
    case 'GROUP_INVITE':
    case 'GROUP_JOIN':
    case 'GROUP_APPLICATION':
    case 'GROUP_APPLICATION_APPROVED':
    case 'GROUP_APPLICATION_REJECTED':
      return entityId ? `/group/${entityId}` : '/groups'
    default:
      return '/notifications'
  }
}
