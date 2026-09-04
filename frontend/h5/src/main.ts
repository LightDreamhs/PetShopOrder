import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import router from './router'
import App from './App.vue'
import './assets/styles/global.scss'

// Vant 函数式 API（非模板组件）的样式必须手动引入，否则组件无样式（如图片预览失去全屏布局）。
// 参见 Vant 官方文档「快速上手 → 引入函数式组件样式」。
import 'vant/es/toast/style'
import 'vant/es/dialog/style'
import 'vant/es/notify/style'
import 'vant/es/image-preview/style'

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
