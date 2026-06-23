import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import zhCn from 'element-plus/dist/locale/zh-cn.mjs';
import 'element-plus/dist/index.css';
import App from './App.vue';
import router from './router';

const app = createApp(App);

app.use(createPinia());
app.use(router);
// 全局使用中文语言包，统一 Element Plus 空态、分页等默认文案。
app.use(ElementPlus, { locale: zhCn });
app.mount('#app');
