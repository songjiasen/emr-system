import { createRouter, createWebHistory } from 'vue-router';

const pageShell = { template: '<div />' };

const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    name: 'home',
    component: pageShell
  },
  {
    path: '/appointment',
    name: 'appointment',
    component: pageShell
  },
  {
    path: '/record',
    name: 'record',
    component: pageShell
  },
  {
    path: '/clinical',
    name: 'clinical',
    component: pageShell
  },
  {
    path: '/content',
    name: 'content',
    component: pageShell
  },
  {
    path: '/profile',
    name: 'profile',
    component: pageShell
  },
  {
    path: '/ai',
    name: 'ai',
    component: pageShell
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/home'
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;
