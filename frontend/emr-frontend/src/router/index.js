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
    path: '/doctors',
    name: 'doctors',
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
    path: '/fees',
    name: 'fees',
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
    path: '/clinical',
    redirect: '/prescriptions'
  },
  {
    path: '/news',
    name: 'news',
    component: pageShell
  },
  {
    path: '/messages',
    name: 'messages',
    component: pageShell
  },
  {
    path: '/content',
    redirect: '/news'
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
