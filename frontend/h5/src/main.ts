import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import router from './router'
import App from './App.vue'
import './assets/styles/global.scss'

async function bootstrap() {
  const app = createApp(App)

  const pinia = createPinia()
  pinia.use(piniaPluginPersistedstate)
  app.use(pinia)
  app.use(router)

  // 多店进店识别：解析 ?s={code} 并校验当前店，须在首屏请求前完成
  const { useShopStore } = await import('@/stores/shop')
  await useShopStore().init()

  app.mount('#app')
}

bootstrap()
