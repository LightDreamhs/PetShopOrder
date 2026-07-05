import { createRouter, createWebHashHistory } from 'vue-router'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginPage.vue'),
      meta: { title: '登录', noLayout: true },
    },
    {
      path: '/',
      redirect: '/orders',
      component: () => import('@/layouts/AdminLayout.vue'),
      children: [
        {
          path: 'orders',
          name: 'Orders',
          component: () => import('@/views/OrderPage.vue'),
          meta: { title: '订单管理', icon: 'Document' },
        },
        {
          path: 'bookings',
          name: 'Bookings',
          component: () => import('@/views/BookingBoardPage.vue'),
          meta: { title: '预约管理', icon: 'Calendar' },
        },
        {
          path: 'products',
          name: 'Products',
          component: () => import('@/views/ProductPage.vue'),
          meta: { title: '商品管理', icon: 'Goods', roles: ['BOSS', 'MANAGER'] },
        },
        {
          path: 'members',
          name: 'Members',
          component: () => import('@/views/MemberPage.vue'),
          meta: { title: '会员管理', icon: 'User', roles: ['BOSS', 'MANAGER'] },
        },
        {
          path: 'settings',
          name: 'Settings',
          component: () => import('@/views/SystemConfigPage.vue'),
          meta: { title: '系统配置', icon: 'Setting', roles: ['BOSS'] },
        },
        {
          path: 'accounts',
          name: 'Accounts',
          component: () => import('@/views/AdminUserPage.vue'),
          meta: { title: '账号管理', icon: 'UserFilled', roles: ['BOSS'] },
        },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  document.title = `${to.meta.title || '管理后台'} - 贰掌柜宠物店`

  if (to.path === '/login') return true

  const { useAuthStore } = await import('@/stores/auth')
  const authStore = useAuthStore()

  if (!authStore.isLoggedIn) {
    const ok = await authStore.checkAuth()
    if (!ok) return '/login'
  }

  const requiredRoles = to.meta.roles as string[] | undefined
  if (requiredRoles && !requiredRoles.includes(authStore.role!)) {
    return '/orders'
  }

  return true
})

export default router
