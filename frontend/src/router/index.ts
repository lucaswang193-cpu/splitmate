import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import { useAuthStore } from '@/stores/auth.ts'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/app' },
    {
      path: '/login',
      component: () => import('@/views/LoginView.vue'),
    },
    {
      path: '/app',
      component: () => import('@/views/AppHomeView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/register',
      component: () => import('@/views/RegisterView.vue'),
    },
    {
      path: '/groups/new',
      component: () => import('@/views/CreateGroupView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/groups/join',
      component: () => import('@/views/JoinGroupView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/groups/:groupId',
      component: () => import('@/views/GroupDetailView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/groups/:groupId/expenses/new',
      component: () => import('@/views/CreateExpenseView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/groups/:groupId/settlements/new',
      component: () => import('@/views/CreateSettlementView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/expenses/:expenseId',
      component: () => import('@/views/ExpenseDetailView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/expenses/:expenseId/edit',
      component: () => import('@/views/EditExpenseView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/account/phone-verification',
      name: 'phone-verification',
      component: () => import('@/views/PhoneVerificationView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/account/security',
      name: 'account-security',
      component: () => import('@/views/AccountSecurityView.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  // 重新整理後，使用儲存的 Token 恢復使用者資料
  if (to.meta.requiresAuth && auth.token && !auth.user) {
    try {
      await auth.fetchMe()
    } catch {
      return '/login'
    }
  }

  if (to.meta.requiresAuth && !auth.isAuthenticated) return '/login'
  if ((to.path === '/login' || to.path === '/register') && auth.isAuthenticated) {
    return '/app'
  }
})

export default router
