import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginPage.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/',
      name: 'Home',
      component: () => import('@/views/HomePage.vue'),
      meta: { requiresAuth: true, title: '贰掌柜宠物店' },
    },
    {
      path: '/cart',
      name: 'Cart',
      component: () => import('@/views/CartPage.vue'),
      meta: { requiresAuth: true, title: '购物车' },
    },
    {
      path: '/checkout',
      name: 'Checkout',
      component: () => import('@/views/CheckoutPage.vue'),
      meta: { requiresAuth: true, title: '确认订单' },
    },
    {
      path: '/order/success',
      name: 'OrderSuccess',
      component: () => import('@/views/OrderSuccessPage.vue'),
      meta: { requiresAuth: true, title: '下单成功' },
    },
    {
      path: '/orders',
      name: 'Orders',
      component: () => import('@/views/OrderListPage.vue'),
      meta: { requiresAuth: true, title: '我的订单' },
    },
    {
      path: '/orders/:id',
      name: 'OrderDetail',
      component: () => import('@/views/OrderDetailPage.vue'),
      meta: { requiresAuth: true, title: '订单详情' },
    },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

let authChecked = false

router.beforeEach(async (to, _from, next) => {
  document.title = (to.meta.title as string) || '贰掌柜宠物店'

  if (!authChecked) {
    const { useAuthStore } = await import('@/stores/auth')
    const authStore = useAuthStore()
    await authStore.checkAuth()
    authChecked = true
  }

  const { useAuthStore } = await import('@/stores/auth')
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.name === 'Login' && authStore.isLoggedIn) {
    next({ name: 'Home' })
  } else {
    next()
  }
})

export default router
