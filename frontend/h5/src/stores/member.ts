import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { MemberProfile } from '@/types'
import { getMemberProfile } from '@/api/member'

export const useMemberStore = defineStore('member', () => {
  const profile = ref<MemberProfile | null>(null)

  const isMember = computed(() => profile.value?.isMember ?? false)
  const discountRate = computed(() => {
    const rate = profile.value?.memberLevel?.discountRate
    return rate ? parseFloat(rate) : 1.0
  })
  const memberLevelName = computed(() => profile.value?.memberLevel?.name ?? null)
  const serviceDiscountText = computed(() => profile.value?.serviceDiscountText ?? null)

  async function fetchProfile() {
    try {
      profile.value = await getMemberProfile()
    } catch {
      profile.value = null
    }
  }

  function clear() {
    profile.value = null
  }

  return { profile, isMember, discountRate, memberLevelName, serviceDiscountText, fetchProfile, clear }
})
