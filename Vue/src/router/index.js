// Vue/src/router/index.js
import { createRouter, createWebHistory } from 'vue-router';
import Layout from '../layout/Layout.vue';
import ChatWindow from '../views/chat/ChatWindow.vue';
import ContactPage from '../views/contact/ContactPage.vue';
import SettingPage from '../views/setting/SettingPage.vue';
import SearchPage from '../views/search/SearchPage.vue';
import Login from '../views/Login.vue';
import Register from '../views/Register.vue';
import Forget from '../views/Forget.vue';
import UserDetail from '../views/UserDetail.vue';
import NotFound from '../views/NotFound.vue';
import GroupsPage from '../views/Groups.vue';
import GroupDetail from '../views/group/GroupDetail.vue';

// Import article components
import ArticleMain from '../article/ArticleMain.vue';
import ArticleRead from '../article/ArticleRead.vue';
import ArticleWrite from '../article/ArticleWrite.vue';
import ArticleManage from '../article/ArticleManage.vue';

// Import notification component
import NotificationListPage from '../views/NotificationListPage.vue';
import TestNotificationPage from '../views/TestNotificationPage.vue';


const routes = [
  {
    path: '/',
    component: Layout,
    redirect: '/chat',
    children: [
      { path: '/chat', name: 'Chat', component: ChatWindow, meta: { title: '聊天', requiresAuth: true } },
      { path: '/chat/:type/:id', name: 'SpecificChat', component: ChatWindow, props: true, meta: { title: '聊天', requiresAuth: true } },
      { path: '/contact', name: 'Contact', component: ContactPage, meta: { title: '联系人', requiresAuth: true } },
      // Article Routes
      { path: '/article', name: 'ArticleMain', component: ArticleMain, meta: { title: '文章中心', requiresAuth: true } },
      { path: '/article/read/:articleId', name: 'ArticleRead', component: ArticleRead, props: true, meta: { title: '文章详情', requiresAuth: true } },
      { path: '/article/write', name: 'ArticleWrite', component: ArticleWrite, meta: { title: '发布文章', requiresAuth: true } },
      { path: '/article/manage', name: 'ArticleManage', component: ArticleManage, meta: { title: '管理文章', requiresAuth: true } },
      // Existing Group and Settings routes
      { path: '/groups', name: 'Groups', component: GroupsPage, meta: { title: '群组', requiresAuth: true } },
      { path: '/group/:groupId', name: 'GroupDetail', component: GroupDetail, props: true, meta: { title: '群组详情', requiresAuth: true } },
      { path: '/setting', name: 'Setting', component: SettingPage, meta: { title: '设置', requiresAuth: true } },
      { path: '/search', name: 'Search', component: SearchPage, meta: { title: '搜索', requiresAuth: true } },
      { path: '/notifications', name: 'Notifications', component: NotificationListPage, meta: { title: '通知中心', requiresAuth: true } },
      { path: '/test-notifications', name: 'TestNotifications', component: TestNotificationPage, meta: { title: '通知测试', requiresAuth: true } },
      { path: '/user/:userId', name: 'UserDetail', component: UserDetail, props: true, meta: { title: '用户详情', requiresAuth: true } }
    ]
  },
  { path: '/login', name: 'Login', component: Login, meta: { title: '登录' } },
  { path: '/register', name: 'Register', component: Register, meta: { title: '注册' } },
  { path: '/forget', name: 'Forget', component: Forget, meta: { title: '忘记密码' } },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: NotFound, meta: { title: '页面未找到' } }
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
});

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('jwt_token');
  if (to.meta.requiresAuth && !token) {
    console.warn('Router Guard: Auth required, no token. Redirecting to login.');
    next({ name: 'Login', query: { redirect: to.fullPath } });
  } else if ((to.name === 'Login' || to.name === 'Register') && token) {
    next({ path: '/' });
  }
  else {
    next();
  }
  const projectTitle = 'Weeb';
  document.title = to.meta.title ? `${to.meta.title} - ${projectTitle}` : projectTitle;
});

export default router;
