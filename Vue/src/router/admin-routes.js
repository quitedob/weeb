// 管理员路由配置
// 将这些路由添加到 router/index.js 的 routes 数组中

export const adminRoutes = [
  {
    path: '/admin/role-permission',
    name: 'RolePermissionManagement',
    component: () => import(/* webpackChunkName: "admin" */ '../views/admin/RolePermissionManagement.vue'),
    meta: { 
      title: '角色权限管理', 
      requiresAuth: true,
      requiresAdmin: true  // 需要管理员权限
    }
  },
  {
    path: '/admin/level-history',
    name: 'UserLevelHistoryManagement',
    component: () => import(/* webpackChunkName: "admin" */ '../views/admin/UserLevelHistoryManagement.vue'),
    meta: { 
      title: '等级历史管理', 
      requiresAuth: true,
      requiresAdmin: true  // 需要管理员权限
    }
  }
]

// 使用说明：
// 在 router/index.js 中导入并添加到 Layout 的 children 中：
// 
// import { adminRoutes } from './admin-routes'
// 
// const routes = [
//   {
//     path: '/',
//     component: Layout,
//     redirect: '/chat',
//     children: [
//       // ... 其他路由
//       ...adminRoutes  // 添加管理员路由
//     ]
//   }
// ]
