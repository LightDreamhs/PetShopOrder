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

  // Mock 数据：开发环境按需加载
  if (import.meta.env.VITE_USE_MOCK === 'true') {
    const { setupMock } = await import('./mock')
    await setupMock()
  }

  app.mount('#app')
}

bootstrap()
