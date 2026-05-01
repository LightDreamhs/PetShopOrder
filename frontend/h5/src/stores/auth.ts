import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as authApi from '@/api/auth'
import { useMemberStore } from './member'

export const useAuthStore = defineStore('auth', () => {
  const isLoggedIn = ref(false)
  const phone = ref<string | null>(null)

  async function checkAuth() {
    try {
      const data = await authApi.checkAuth()
      isLoggedIn.value = data.loggedIn
      phone.value = data.phone
    } catch {
      isLoggedIn.value = false
      phone.value = null
    }
  }

  async function sendSmsCode(phoneNum: string) {
    await authApi.sendSmsCode(phoneNum)
  }

  async function login(phoneNum: string, code: string) {
    await authApi.login(phoneNum, code)
    await checkAuth()
    const memberStore = useMemberStore()
    await memberStore.fetchProfile()
  }

  async function logout() {
    try {
      await authApi.logout()
    } finally {
      isLoggedIn.value = false
      phone.value = null
      const memberStore = useMemberStore()
      memberStore.clear()
    }
  }

  return { isLoggedIn, phone, checkAuth, sendSmsCode, login, logout }
})
