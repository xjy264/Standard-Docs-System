import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import './styles/main.css'
import App from './App.vue'
import router from './router'
import { setAuthInvalidNavigator } from './api/http'

dayjs.locale('zh-cn')

setAuthInvalidNavigator(() => {
  if (router.currentRoute.value.path !== '/login') {
    router.replace('/login').catch(() => undefined)
  }
})

createApp(App).use(createPinia()).use(router).use(ElementPlus, { locale: zhCn }).mount('#app')
