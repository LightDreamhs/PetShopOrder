import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
  withCredentials: true,
})

request.interceptors.response.use(
  (res) => {
    if (res.data.code !== 200) {
      ElMessage.error(res.data.message || '请求失败')
      return Promise.reject(new Error(res.data.message))
    }
    return res.data
  },
  (error) => {
    if (error.response?.status === 401) {
      import('@/stores/auth').then(({ useAuthStore }) => {
        const authStore = useAuthStore()
        authStore.clearAuth()
        window.location.hash = '/login'
      })
    } else if (error.config?.headers?.['X-Silent']) {
      // 静默请求，不弹错误提示
    } else {
      ElMessage.error(error.response?.data?.message || '网络错误')
    }
    return Promise.reject(error)
  },
)

export default request
