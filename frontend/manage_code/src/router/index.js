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
    redirect: '/departments'
  },
  {
    path: '/departments',
    name: 'departments',
    component: pageShell
  },
  {
    path: '/managed-users',
    name: 'managed-users',
    component: pageShell
  },
  {
    path: '/records',
    name: 'records',
    component: pageShell
  },
  {
    path: '/triage',
    name: 'triage',
    component: pageShell
  },
  {
    path: '/admissions',
    name: 'admissions',
    component: pageShell
  },
  {
    path: '/discharges',
    name: 'discharges',
    component: pageShell
  },
  {
    path: '/templates',
    name: 'templates',
    component: pageShell
  },
  {
    path: '/clinical',
    redirect: '/orders'
  },
  {
    path: '/orders',
    name: 'orders',
    component: pageShell
  },
  {
    path: '/prescriptions',
    name: 'prescriptions',
    component: pageShell
  },
  {
    path: '/tests',
    name: 'tests',
    component: pageShell
  },
  {
    path: '/workflow',
    redirect: '/workflow-tasks'
  },
  {
    path: '/workflow-tasks',
    name: 'workflow-tasks',
    component: pageShell
  },
  {
    path: '/workflow-audits',
    name: 'workflow-audits',
    component: pageShell
  },
  {
    path: '/archive-applications',
    name: 'archive-applications',
    component: pageShell
  },
  {
    path: '/archives',
    name: 'archives',
    component: pageShell
  },
  {
    path: '/billing',
    name: 'billing',
    component: pageShell
  },
  {
    path: '/system',
    redirect: '/news'
  },
  {
    path: '/news',
    name: 'news',
    component: pageShell
  },
  {
    path: '/carousels',
    name: 'carousels',
    component: pageShell
  },
  {
    path: '/messages',
    name: 'messages',
    component: pageShell
  },
  {
    path: '/syslogs',
    name: 'syslogs',
    component: pageShell
  },
  {
    path: '/config',
    name: 'config',
    component: pageShell
  },
  {
    path: '/menus',
    name: 'menus',
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
