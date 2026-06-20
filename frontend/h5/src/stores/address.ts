import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as addressApi from '@/api/address'
import type { AddressRequest, UserAddress } from '@/types'

export const useAddressStore = defineStore('address', () => {
  const list = ref<UserAddress[]>([])
  const loaded = ref(false)

  async function fetchList() {
    list.value = await addressApi.getAddressList()
    loaded.value = true
  }

  function clear() {
    list.value = []
    loaded.value = false
  }

  function getById(id: number) {
    return list.value.find((a) => a.id === id) || null
  }

  async function add(data: AddressRequest) {
    const created = await addressApi.createAddress(data)
    await fetchList()
    return created
  }

  async function update(id: number, data: AddressRequest) {
    const updated = await addressApi.updateAddress(id, data)
    await fetchList()
    return updated
  }

  async function setDefault(id: number) {
    await addressApi.setDefaultAddress(id)
    await fetchList()
  }

  async function remove(id: number) {
    await addressApi.deleteAddress(id)
    await fetchList()
  }

  return { list, loaded, fetchList, clear, getById, add, update, setDefault, remove }
})
