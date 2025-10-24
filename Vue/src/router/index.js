// Vue/src/router/index.js
import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../stores/authStore';

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
        component: () => import(/* webpackChunkName: "chat" */ '../views/chat/ChatWindow.vue'),
        meta: { title: '聊天', requiresAuth: true }
      },
      {
        path: '/chat/:type/:id',
        name: 'SpecificChat',
        component: () => import(/* webpackChunkName: "chat" */ '../views/chat/ChatWindow.vue'),
        props: true,
        meta: { title: '聊天', requiresAuth: true }
      },
      {
        path: '/contact',
        name: 'Contact',
        component: () => import(/* webpackChunkName: "contact" */ '../contact/ContactPage.vue'),
        meta: { title: '联系人', requiresAuth: true }
      },
      // Article Routes
      {
        path: '/article',
        name: 'ArticleMain',
        component: () => import(/* webpackChunkName: "article" */ '../article/ArticleMain.vue'),
        meta: { title: '文章中心', requiresAuth: true }
      },
      {
        path: '/article/read/:articleId',
        name: 'ArticleRead',
        component: () => import(/* webpackChunkName: "article" */ '../article/ArticleRead.vue'),
        props: true,
        meta: { title: '文章详情', requiresAuth: true }
      },
      {
        path: '/article/write',
        name: 'ArticleWrite',
        component: () => import(/* webpackChunkName: "article" */ '../article/ArticleWrite.vue'),
        meta: { title: '发布文章', requiresAuth: true }
      },
      {
        path: '/article/manage',
        name: 'ArticleManage',
        component: () => import(/* webpackChunkName: "article" */ '../article/ArticleManage.vue'),
        meta: { title: '管理文章', requiresAuth: true }
      },
      {
        path: '/article/edit/:articleId',
        name: 'ArticleEdit',
        component: () => import(/* webpackChunkName: "article" */ '../article/ArticleEdit.vue'),
        props: true,
        meta: { title: '编辑文章', requiresAuth: true }
      },
      // Existing Group and Settings routes
      {
        path: '/groups',
        name: 'Groups',
        component: () => import(/* webpackChunkName: "group" */ '../views/Groups.vue'),
        meta: { title: '群组', requiresAuth: true }
      },
      {
        path: '/groups/manage',
        name: 'GroupManage',
        component: () => import(/* webpackChunkName: "group" */ '../group/GroupPage.vue'),
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
        path: '/setting',
        name: 'Setting',
        component: () => import(/* webpackChunkName: "settings" */ '../views/Settings.vue'),
        meta: { title: '设置', requiresAuth: true }
      },
      {
        path: '/search',
        name: 'Search',
        component: () => import(/* webpackChunkName: "search" */ '../search/SearchPage.vue'),
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
      // 管理员路由
      {
        path: '/admin',
        name: 'Admin',
        component: () => import(/* webpackChunkName: "admin" */ '../views/admin/Dashboard.vue'),
        meta: { title: '管理后台', requiresAuth: true }
      },
      {
        path: '/admin/users',
        name: 'AdminUsers',
        component: () => import(/* webpackChunkName: "admin" */ '../views/admin/UserManagement.vue'),
        meta: { title: '用户管理', requiresAuth: true }
      },
      {
        path: '/admin/roles',
        name: 'AdminRoles',
        component: () => import(/* webpackChunkName: "admin" */ '../views/admin/RoleManagement.vue'),
        meta: { title: '角色管理', requiresAuth: true }
      },
      {
        path: '/admin/permissions',
        name: 'AdminPermissions',
        component: () => import(/* webpackChunkName: "admin" */ '../views/admin/PermissionManagement.vue'),
        meta: { title: '权限管理', requiresAuth: true }
      },
      {
        path: '/admin/content',
        name: 'AdminContent',
        component: () => import(/* webpackChunkName: "admin" */ '../views/admin/ContentModeration.vue'),
        meta: { title: '内容审核', requiresAuth: true }
      },
      {
        path: '/admin/monitor',
        name: 'AdminMonitor',
        component: () => import(/* webpackChunkName: "admin" */ '../views/admin/SystemMonitor.vue'),
        meta: { title: '系统监控', requiresAuth: true }
      },
      {
        path: '/admin/user-behavior',
        name: 'AdminUserBehavior',
        component: () => import(/* webpackChunkName: "admin" */ '../views/admin/UserBehaviorMonitor.vue'),
        meta: { title: '用户行为监控', requiresAuth: true }
      },
      {
        path: '/admin/logs',
        name: 'AdminLogs',
        component: () => import(/* webpackChunkName: "admin" */ '../views/admin/SystemLogViewer.vue'),
        meta: { title: '系统日志', requiresAuth: true }
      }
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

    try {
      const authStore = useAuthStore();
      // **关键**：如果 currentUser 不存在，才去后端验证
      if (!authStore.currentUser) {
        await authStore.fetchUserInfo();
      }
      next();
    } catch (error) {
      // fetchUserInfo 失败会抛出错误，在这里捕获
      console.warn('Router Guard: Token validation failed, redirecting to login.');
      const authStore = useAuthStore();
      authStore.logoutCleanup(); // 直接调用清理函数
      next({ name: 'Login', query: { redirect: to.fullPath } });
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
