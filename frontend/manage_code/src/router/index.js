import { createRouter, createWebHistory } from 'vue-router';

const pageShell = { template: '<div />' };

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: pageShell
  },
  {
    path: '/appointments',
    name: 'appointments',
    component: pageShell
  },
  {
    path: '/users',
    name: 'users',
    component: pageShell
  },
  {
    path: '/records',
    name: 'records',
    component: pageShell
  },
  {
    path: '/clinical',
    name: 'clinical',
    component: pageShell
  },
  {
    path: '/workflow',
    name: 'workflow',
    component: pageShell
  },
  {
    path: '/billing',
    name: 'billing',
    component: pageShell
  },
  {
    path: '/system',
    name: 'system',
    component: pageShell
  },
  {
    path: '/ai',
    name: 'ai',
    component: pageShell
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;
