// File path: /Vue/src/api/index.js

// Import and export API modules
import auth from './modules/auth';
import user from './modules/user';
import group from './modules/group';
import search from './modules/search';
import article from './modules/article';
import notification from './modules/notification';
import message from './modules/message';
import messageThread from './modules/messageThread';
import comment from './modules/comment';
import follow from './modules/follow';
import contact from './modules/contact';
import chat from './modules/chat';
import ai from './modules/ai';
import rolePermission from './modules/rolePermission';
import userLevel from './modules/userLevel';
import userLevelHistory from './modules/userLevelHistory';
import userLevelIntegration from './modules/userLevelIntegration';
import admin from './modules/admin';

export default {
  auth,
  user,
  group,
  search,
  article,
  notification,
  message,
  messageThread,
  comment,
  follow,
  contact,
  chat,
  ai,
  rolePermission,
  userLevel,
  userLevelHistory,
  userLevelIntegration,
  admin
};
