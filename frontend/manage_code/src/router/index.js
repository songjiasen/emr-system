import { createRouter, createWebHistory } from 'vue-router';

const routes = [
  {
    path: '/',
    name: 'dashboard',
    component: { template: '<div />' }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;

