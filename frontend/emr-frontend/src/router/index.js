import { createRouter, createWebHistory } from 'vue-router';

const routes = [
  { path: '/', redirect: '/home' },
  { path: '/home', name: 'home', component: () => import('../views/HomePage.vue') },
  { path: '/doctors', name: 'doctors', component: () => import('../views/DoctorsPage.vue') },
  { path: '/doctor/:id', name: 'doctorDetail', component: () => import('../views/DoctorDetailPage.vue') },
  { path: '/appointment', name: 'appointment', component: () => import('../views/AppointmentsPage.vue') },
  { path: '/records', name: 'records', component: () => import('../views/RecordsPage.vue') },
  { path: '/fees', name: 'fees', component: () => import('../views/FeesPage.vue') },
  { path: '/prescriptions', name: 'prescriptions', component: () => import('../views/PrescriptionsPage.vue') },
  { path: '/tests', name: 'tests', component: () => import('../views/TestsPage.vue') },
  { path: '/news', name: 'news', component: () => import('../views/NewsPage.vue') },
  { path: '/messages', name: 'messages', component: () => import('../views/MessagesPage.vue') },
  { path: '/profile', name: 'profile', component: () => import('../views/ProfilePage.vue') },
  { path: '/:pathMatch(.*)*', redirect: '/home' }
];

export default createRouter({ history: createWebHistory(), routes });
