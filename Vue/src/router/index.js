// Vue/src/router/index.js
import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../stores/authStore';
import { captureSession, isCurrentSession, isCurrentCredential } from '@/utils/session';

// Layout component (keep eager since it's used by all authenticated routes)
import Layout from '../layout/Layout.vue';



const routes = [
  {
    path: '/',
    component: Layout,
    redirect: '/chat',
    children: [
      {
        path: '/chat',
        name: 'Chat',
        component: () => import(/* webpackChunkName: "chat" */ '../views/chat/ChatPage.vue'),
        meta: { title: '聊天', requiresAuth: true }
      },
      {
        path: '/chat/:type/:id',
        name: 'SpecificChat',
        component: () => import(/* webpackChunkName: "chat" */ '../views/chat/ChatPage.vue'),
        props: true,
        meta: { title: '聊天', requiresAuth: true }
      },
      {
        path: '/contact',
        name: 'Contact',
        component: () => import(/* webpackChunkName: "contact" */ '@/views/contact/ContactPage.vue'),
        meta: { title: '联系人', requiresAuth: true }
      },
      // Article Routes
      {
        path: '/article',
        name: 'ArticleMain',
        component: () => import(/* webpackChunkName: "article" */ '@/views/article/ArticleMain.vue'),
        meta: { title: '文章中心', requiresAuth: true }
      },
      {
        path: '/article/read/:articleId',
        name: 'ArticleRead',
        component: () => import(/* webpackChunkName: "article" */ '@/views/article/ArticleRead.vue'),
        props: true,
        meta: { title: '文章详情', requiresAuth: true }
      },
      {
        path: '/article/write',
        name: 'ArticleWrite',
        component: () => import(/* webpackChunkName: "article" */ '@/views/article/ArticleWrite.vue'),
        meta: { title: '发布文章', requiresAuth: true }
      },
      {
        path: '/article/manage',
        name: 'ArticleManage',
        component: () => import(/* webpackChunkName: "article" */ '@/views/article/ArticleManage.vue'),
        meta: { title: '管理文章', requiresAuth: true }
      },
      {
        path: '/article/edit/:articleId',
        name: 'ArticleEdit',
        component: () => import(/* webpackChunkName: "article" */ '@/views/article/ArticleEdit.vue'),
        props: true,
        meta: { title: '编辑文章', requiresAuth: true }
      },
      // Existing Group and Settings routes
      {
        path: '/groups',
        name: 'Groups',
        component: () => import(/* webpackChunkName: "group" */ '@/views/group/Groups.vue'),
        meta: { title: '群组', requiresAuth: true }
      },
      {
        path: '/groups/manage',
        name: 'GroupManage',
        component: () => import(/* webpackChunkName: "group" */ '@/views/group/GroupPage.vue'),
        meta: { title: '群组管理', requiresAuth: true }
      },
      {
        path: '/group/:groupId',
        name: 'GroupDetail',
        component: () => import(/* webpackChunkName: "group" */ '../views/group/GroupDetail.vue'),
        props: true,
        meta: { title: '群组详情', requiresAuth: true }
      },
      {
        path: '/profile',
        name: 'Profile',
        component: () => import(/* webpackChunkName: "user" */ '../views/UserProfile.vue'),
        meta: { title: '个人资料', requiresAuth: true }
      },
      {
        path: '/level-history',
        name: 'LevelHistory',
        component: () => import(/* webpackChunkName: "user" */ '../views/UserLevelHistory.vue'),
        meta: { title: '等级历史', requiresAuth: true }
      },
      {
        path: '/setting',
        name: 'Setting',
        component: () => import(/* webpackChunkName: "settings" */ '../views/Settings.vue'),
        meta: { title: '设置', requiresAuth: true }
      },
      {
        path: '/search',
        name: 'Search',
        component: () => import(/* webpackChunkName: "search" */ '@/views/search/SearchPage.vue'),
        meta: { title: '搜索', requiresAuth: true }
      },
      {
        path: '/notifications',
        name: 'Notifications',
        component: () => import(/* webpackChunkName: "notifications" */ '../views/NotificationListPage.vue'),
        meta: { title: '通知中心', requiresAuth: true }
      },
      {
        path: '/test-notifications',
        name: 'TestNotifications',
        component: () => import(/* webpackChunkName: "notifications" */ '../views/TestNotificationPage.vue'),
        meta: { title: '通知测试', requiresAuth: true }
      },
      {
        path: '/user/:userId',
        name: 'UserDetail',
        component: () => import(/* webpackChunkName: "user" */ '../views/UserDetail.vue'),
        props: true,
        meta: { title: '用户详情', requiresAuth: true }
      },
      // 管理员路由已移除 - RBAC系统已禁用
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import(/* webpackChunkName: "auth" */ '../views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import(/* webpackChunkName: "auth" */ '../views/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/forget',
    name: 'Forget',
    component: () => import(/* webpackChunkName: "auth" */ '../views/Forget.vue'),
    meta: { title: '忘记密码' }
  },
  {
    path: '/reset-password',
    name: 'ResetPassword',
    component: () => import('../views/ResetPassword.vue'),
    meta: { title: '重置密码' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import(/* webpackChunkName: "common" */ '../views/NotFound.vue'),
    meta: { title: '页面未找到' }
  }
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
});

router.beforeEach(async (to, from, next) => {
  const token = localStorage.getItem('jwt_token');

  if (to.meta.requiresAuth) {
    if (!token) {
      next({ name: 'Login', query: { redirect: to.fullPath } });
      return;
    }

    const authStore = useAuthStore();
    let requestSession = captureSession();
    const login = { name: 'Login', query: { redirect: to.fullPath } };
    try {
      // **关键**：如果 currentUser 不存在，才去后端验证
      if (!authStore.currentUser) {
        try {
          await authStore.fetchUserInfo();
        } catch (error) {
          // A renewal can revoke the old credential while this profile read is pending.
          if (!isCurrentSession(requestSession) || isCurrentCredential(requestSession) || !authStore.accessToken) throw error;
          requestSession = captureSession();
          await authStore.fetchUserInfo();
        }
      }
      if (!isCurrentSession(requestSession)) {
        next(authStore.accessToken ? false : login);
        return;
      }
      if (!authStore.accessToken || !authStore.currentUser) {
        next(login);
        return;
      }
      next();
    } catch (error) {
      // The interceptor owns current-credential authentication failures. A late
      // response or a temporary network failure must not clear a newer session.
      next(authStore.accessToken ? false : login);
      return;
    }
  } else if ((to.name === 'Login' || to.name === 'Register') && token) {
    next({ path: '/' });
  } else {
    next();
  }

  const projectTitle = 'Weeb';
  document.title = to.meta.title ? `${to.meta.title} - ${projectTitle}` : projectTitle;
});

export default router;
