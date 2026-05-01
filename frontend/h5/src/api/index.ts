import axios from 'axios'
import type { ApiResponse } from '@/types'
import { showToast } from 'vant'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
  withCredentials: true,
})

request.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse<unknown>
    if (res.code !== 200) {
      showToast(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return response.data
  },
  (error) => {
    if (error.response?.status === 401) {
      import('@/stores/auth').then(({ useAuthStore }) => {
        const authStore = useAuthStore()
        authStore.isLoggedIn = false
        authStore.phone = null
        window.location.href = '/login'
      })
    } else {
      showToast(error.response?.data?.message || '网络错误')
    }
    return Promise.reject(error)
  },
)

export default request
