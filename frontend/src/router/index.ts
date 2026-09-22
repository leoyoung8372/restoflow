import { createRouter, createWebHistory } from 'vue-router'
import { TOKEN_KEY } from '@/api/request'

/**
 * 路由表与登录守卫。
 *
 * 页面组件用 () => import(...) 懒加载：访问到该路径时才下载对应代码，
 * 避免所有页面都打进首次加载的主包里。
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
    },
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue'),
    },
  ],
})

// 每次跳转前检查登录状态，直接读 localStorage（与 request.ts 共用同一个 key）
router.beforeEach((to) => {
  const token = localStorage.getItem(TOKEN_KEY)

  // 未登录却要访问其他页面 → 送回登录页
  if (!token && to.path !== '/login') {
    return '/login'
  }

  // 已登录却要访问登录页 → 直接进首页，避免重复登录
  if (token && to.path === '/login') {
    return '/'
  }
})

export default router
