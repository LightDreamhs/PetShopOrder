<template>
  <div class="address-manage-page">
    <van-nav-bar title="管理收货地址" left-arrow @click-left="router.back()" />

    <div class="address-content">
      <div v-if="addressStore.list.length === 0 && !loading" class="empty-state">
        <van-icon name="location-o" size="48" />
        <p>还没有收货地址</p>
        <van-button type="primary" round size="small" @click="showPicker = true">新增地址</van-button>
      </div>

      <div
        v-for="addr in addressStore.list"
        :key="addr.id"
        class="addr-card"
      >
        <div class="addr-main">
          <div class="addr-title">
            <span v-if="addr.label" class="addr-label">{{ addr.label }}</span>
            <span class="addr-name">{{ addr.address }}</span>
          </div>
          <div v-if="addr.detail" class="addr-detail">{{ addr.detail }}</div>
          <div class="addr-actions">
            <span class="action-btn" @click="setDefault(addr)">
              <van-icon :name="addr.isDefault ? 'passed' : 'circle'" />
              {{ addr.isDefault ? '默认地址' : '设为默认' }}
            </span>
            <span class="action-btn danger" @click="handleDelete(addr)">
              <van-icon name="delete-o" />
              删除
            </span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="addressStore.list.length > 0" class="footer-bar safe-area-bottom">
      <van-button type="primary" block round @click="showPicker = true">新增地址</van-button>
    </div>

    <!-- 地图选点弹窗 -->
    <AddressPicker v-model:show="showPicker" @confirm="handleConfirm" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showSuccessToast, showToast } from 'vant'
import { useAddressStore } from '@/stores/address'
import type { UserAddress } from '@/types'
import AddressPicker from '@/components/checkout/AddressPicker.vue'

const router = useRouter()
const addressStore = useAddressStore()
const showPicker = ref(false)
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    await addressStore.fetchList()
  } finally {
    loading.value = false
  }
})

async function handleConfirm() {
  await addressStore.fetchList()
}

async function setDefault(addr: UserAddress) {
  if (addr.isDefault) return
  try {
    await addressStore.setDefault(addr.id)
    showSuccessToast('已设为默认地址')
  } catch {
    showToast('操作失败')
  }
}

function handleDelete(addr: UserAddress) {
  showConfirmDialog({
    title: '删除地址',
    message: `确定删除「${addr.address}」吗？`,
  })
    .then(async () => {
      try {
        await addressStore.remove(addr.id)
        showSuccessToast('已删除')
      } catch {
        showToast('删除失败')
      }
    })
    .catch(() => {})
}
</script>

<style scoped lang="scss">
.address-manage-page {
  min-height: 100vh;
  background: $bg;
  display: flex;
  flex-direction: column;
}

.address-content {
  flex: 1;
  padding: 12px;
  padding-bottom: 80px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 60px 0;
  color: $text-muted;

  p {
    font-size: 14px;
    margin: 0;
  }
}

.addr-card {
  background: #fff;
  border-radius: 10px;
  padding: 14px;
  margin-bottom: 10px;
}

.addr-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.addr-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  color: $text;
  font-weight: 500;
}

.addr-label {
  font-size: 11px;
  color: #fff;
  background: $primary;
  padding: 1px 6px;
  border-radius: 3px;
  flex-shrink: 0;
}

.addr-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.addr-detail {
  font-size: 13px;
  color: $text-secondary;
}

.addr-actions {
  display: flex;
  gap: 20px;
  margin-top: 6px;
  padding-top: 8px;
  border-top: 1px solid #f5f5f5;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: $text-secondary;
  cursor: pointer;

  &.danger {
    color: #ee0a24;
  }
}

.footer-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 12px 16px;
  background: #fff;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);
  z-index: 50;
}
</style>
