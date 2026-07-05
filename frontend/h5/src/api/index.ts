import axios from 'axios'
import type { ApiResponse } from '@/types'
import { showToast } from 'vant'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
  withCredentials: true,
})

// 统一处理登录态丢失：清状态并跳登录页。
// 后端 NotLoginException 被全局异常处理器包成 HTTP 200 + body code=401，
// 所以此处既兼容标准 HTTP 401，也兼容业务 code=401。
function handleUnauthorized() {
  import('@/stores/auth').then(({ useAuthStore }) => {
    const authStore = useAuthStore()
    authStore.isLoggedIn = false
    authStore.phone = null
    const redirect = window.location.pathname + window.location.search
    window.location.href = `/login?redirect=${encodeURIComponent(redirect)}`
  })
}

request.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse<unknown>
    if (res.code !== 200) {
      if (res.code === 401) {
        handleUnauthorized()
        return Promise.reject(new Error(res.message))
      }
      showToast(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return response.data
  },
  (error) => {
    if (error.response?.status === 401) {
      handleUnauthorized()
    } else {
      showToast(error.response?.data?.message || '网络错误')
    }
    return Promise.reject(error)
  },
)

export default request
