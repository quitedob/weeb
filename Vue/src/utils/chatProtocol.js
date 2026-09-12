import { MESSAGE_STATUS, normalizeMessage } from './messageStatus';

export const sameId = (left, right) => left != null && right != null && String(left) === String(right);
export const isPersistedId = id => /^[1-9]\d*$/.test(String(id));
export function compareIds(left, right) {
  const a = String(left ?? 0), b = String(right ?? 0);
  return a.length - b.length || a.localeCompare(b);
}

export function highestMessageId(messages = []) {
  return messages.reduce((cursor, message) => isPersistedId(message.id) && compareIds(message.id, cursor) > 0 ? String(message.id) : cursor, '0');
}

export function canonicalMessage(source, chatId, currentUserId) {
  const message = source?.data && source.id == null && source.messageId == null ? source.data : source;
  if (!message) return null;
  const id = message.id ?? message.messageId;
  const sharedChatId = message.sharedChatId ?? message.chatId ?? chatId;
  if (id == null || sharedChatId == null) return null;
  const content = message.content ?? message.msgContent ?? '';
  const senderId = message.senderId ?? message.fromId ?? message.fromUserId;
  let fileData = message.fileData;
  if (message.messageType === 2 && !fileData && typeof content === 'string') {
    try { fileData = JSON.parse(content); } catch { /* Keep the original file message content. */ }
  }
  return normalizeMessage({
    ...message, id, messageId: id, chatId: sharedChatId, sharedChatId,
    senderId, fromId: senderId,
    content, msgContent: typeof content === 'object' ? content?.content ?? '' : content,
    timestamp: message.timestamp ?? message.createdAt ?? message.sendTime,
    isFromMe: senderId != null ? sameId(senderId, currentUserId) : message.isFromMe === true,
    messageType: message.messageType ?? 1,
    fileData,
    reactions: Array.isArray(message.reactions) ? message.reactions : [],
    reactionVersion: message.reactionVersion ?? 0,
    status: message.status ?? MESSAGE_STATUS.SENT
  });
}

export function mergeMessage(messages, incoming) {
  const index = messages.findIndex(message => sameId(message.id, incoming.id)
    || (incoming.clientMessageId && message.clientMessageId === incoming.clientMessageId
      && (sameId(message.fromId ?? message.senderId, incoming.fromId ?? incoming.senderId)
        || (message.isFromMe && incoming.isFromMe))));
  if (index < 0) messages.push(incoming);
  else {
    const previous = messages[index];
    const merged = { ...previous, ...incoming };
    if (previous.isRecalled) merged.isRecalled = previous.isRecalled;
    if (isPersistedId(incoming.id)) {
      delete merged.tempId;
      delete merged.sendPayload;
      merged.status = previous.status >= MESSAGE_STATUS.SENT && previous.status <= MESSAGE_STATUS.READ
        ? Math.max(previous.status, incoming.status) : incoming.status;
    }
    if (compareIds(previous.reactionVersion ?? 0, incoming.reactionVersion ?? 0) > 0) {
      merged.reactions = previous.reactions;
      merged.reactionVersion = previous.reactionVersion;
    }
    messages[index] = merged;
  }
  messages.sort((a, b) => {
    const ap = isPersistedId(a.id), bp = isPersistedId(b.id);
    return ap && bp ? compareIds(a.id, b.id) : ap ? -1 : bp ? 1 : 0;
  });
  return index < 0;
}
