import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, logout as apiLogout, getProfile } from '@/api/auth'
import type { AdminProfile, LoginParams } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const profile = ref<AdminProfile | null>(null)
  const isLoggedIn = computed(() => !!profile.value)
  const role = computed(() => profile.value?.role ?? null)
  const realName = computed(() => profile.value?.realName ?? '')
  const roleLabel = computed(() => profile.value?.roleLabel ?? '')

  async function login(params: LoginParams) {
    const res = await apiLogin(params)
    profile.value = res.data
    return res.data
  }

  /** 路由守卫用：静默检查登录态，不弹错误提示 */
  async function checkAuth(): Promise<boolean> {
    try {
      const res = await getProfile()
      profile.value = res.data
      return true
    } catch {
      profile.value = null
      return false
    }
  }

  async function logout() {
    try {
      await apiLogout()
    } catch {
      // ignore
    }
    clearAuth()
  }

  function clearAuth() {
    profile.value = null
  }

  return { profile, isLoggedIn, role, realName, roleLabel, login, checkAuth, logout, clearAuth }
})
