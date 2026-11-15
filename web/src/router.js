import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from './api'

const routes = [
  { path: '/', name: 'home', component: () => import('./views/Home.vue') },
  { path: '/staff', name: 'staff', component: () => import('./views/Staff.vue') },
  { path: '/time', name: 'time', component: () => import('./views/Time.vue') },
  { path: '/confirm', name: 'confirm', component: () => import('./views/Confirm.vue') },
  { path: '/done/:id', name: 'done', component: () => import('./views/Done.vue') },
  { path: '/mine', name: 'mine', component: () => import('./views/Mine.vue') },
  { path: '/admin/login', name: 'login', component: () => import('./views/Login.vue') },
  { path: '/admin', name: 'admin', component: () => import('./views/Admin.vue') },
  { path: '/admin/services', name: 'serviceList', component: () => import('./views/ServiceList.vue') },
  { path: '/admin/services/new', name: 'serviceNew', component: () => import('./views/ServiceForm.vue') },
  { path: '/admin/services/:id', name: 'serviceEdit', component: () => import('./views/ServiceForm.vue') },
  { path: '/admin/staff', name: 'staffList', component: () => import('./views/StaffList.vue') },
  { path: '/admin/staff/new', name: 'staffNew', component: () => import('./views/StaffForm.vue') },
  { path: '/admin/staff/:id', name: 'staffEdit', component: () => import('./views/StaffForm.vue') },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// 后台页面统一要求登录，未登录先去登录页
router.beforeEach((to) => {
  if (to.path.startsWith('/admin') && to.path !== '/admin/login' && !getToken()) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
