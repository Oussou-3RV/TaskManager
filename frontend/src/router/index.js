import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppLayout from '@/components/layout/AppLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      component: AppLayout,
      children: [
        {
          path: '',
          name: 'dashboard',
          component: () => import('@/views/DashboardView.vue'),
          meta: { title: 'Dashboard', roles: ['OPERATOR', 'ADMIN', 'AUDITOR'] },
        },
        {
          path: 'tasks',
          name: 'tasks',
          component: () => import('@/views/TaskSubmitView.vue'),
          meta: { title: 'Soumettre', roles: ['OPERATOR', 'ADMIN'] },
        },
        {
          path: 'machines',
          name: 'machines',
          component: () => import('@/views/MachinesView.vue'),
          meta: { title: 'Machines', roles: ['OPERATOR', 'ADMIN', 'AUDITOR'] },
        },
        {
          path: 'rules',
          name: 'rules',
          component: () => import('@/views/RulesView.vue'),
          meta: { title: 'Règles', roles: ['OPERATOR', 'ADMIN', 'AUDITOR'] },
        },
        {
          path: 'audit',
          name: 'audit',
          component: () => import('@/views/AuditView.vue'),
          meta: { title: 'Audit', roles: ['AUDITOR', 'ADMIN'] },
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()

  if (to.meta.public) {
    if (auth.isAuthenticated) {
      return { path: '/' }
    }
    return true
  }

  if (!auth.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.meta.roles && !auth.hasRole(...to.meta.roles)) {
    return { path: '/' }
  }

  return true
})

export default router
