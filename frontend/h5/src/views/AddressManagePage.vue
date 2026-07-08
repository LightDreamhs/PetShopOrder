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
        :class="{ 'is-default': addr.isDefault }"
      >
        <div class="addr-main">
          <div class="addr-title">
            <van-icon name="location-o" class="addr-pin" />
            <span v-if="addr.label" class="addr-label">{{ addr.label }}</span>
            <span class="addr-name">{{ addr.address }}</span>
          </div>
          <div v-if="addr.detail" class="addr-detail">{{ addr.detail }}</div>
          <div class="addr-actions">
            <span
              v-if="!addr.isDefault"
              class="action-btn"
              @click="setDefault(addr)"
            >
              <van-icon name="circle" />
              设为默认
            </span>
            <span v-else class="action-btn default-tag">
              <van-icon name="passed" />
              默认地址
            </span>
            <span class="action-btn" @click="openEditDetail(addr)">
              <van-icon name="edit" />
              编辑门牌号
            </span>
            <span class="action-btn danger" @click="openDeleteConfirm(addr)">
              <van-icon name="delete-o" />
              删除
            </span>
          </div>
        </div>
      </div>

      <!-- 备案号 -->
      <IcpFooter />
    </div>

    <div v-if="addressStore.list.length > 0" class="footer-bar safe-area-bottom">
      <van-button type="primary" block round @click="showPicker = true">新增地址</van-button>
    </div>

    <!-- 地图选点弹窗 -->
    <AddressPicker v-model:show="showPicker" @confirm="handleConfirm" />

    <!-- 编辑门牌号弹窗 -->
    <van-dialog
      v-model:show="showEditDetail"
      title="编辑门牌号"
      show-cancel-button
      :before-close="handleEditConfirm"
    >
      <div class="edit-detail-body">
        <div class="edit-detail-addr">{{ editingAddress?.address }}</div>
        <van-field
          v-model="editingDetail"
          placeholder="楼栋/门牌号，如：3栋502室"
          clearable
        />
      </div>
    </van-dialog>

    <!-- 删除确认弹窗（底部弹出） -->
    <van-popup
      v-model:show="showDeleteConfirm"
      position="bottom"
      round
      closeable
      :style="{ paddingBottom: 'env(safe-area-inset-bottom)' }"
    >
      <div class="delete-confirm">
        <div class="delete-confirm-title">确认删除该地址？</div>
        <div class="delete-confirm-addr">
          <van-icon name="location-o" />
          <div class="delete-confirm-addr-text">
            <div>{{ deletingAddress?.address }}</div>
            <div v-if="deletingAddress?.detail" class="delete-confirm-detail">
              {{ deletingAddress.detail }}
            </div>
          </div>
        </div>
        <div class="delete-confirm-actions">
          <van-button block round @click="cancelDelete">取消</van-button>
          <van-button block round type="danger" :loading="deleting" @click="confirmDelete">
            确认删除
          </van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast, showToast } from 'vant'
import { useAddressStore } from '@/stores/address'
import type { UserAddress } from '@/types'
import AddressPicker from '@/components/checkout/AddressPicker.vue'
import IcpFooter from '@/components/common/IcpFooter.vue'

const router = useRouter()
const addressStore = useAddressStore()
const showPicker = ref(false)
const loading = ref(false)
const showEditDetail = ref(false)
const editingAddress = ref<UserAddress | null>(null)
const editingDetail = ref('')
const showDeleteConfirm = ref(false)
const deletingAddress = ref<UserAddress | null>(null)
const deleting = ref(false)

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

function openDeleteConfirm(addr: UserAddress) {
  deletingAddress.value = addr
  showDeleteConfirm.value = true
}

function cancelDelete() {
  showDeleteConfirm.value = false
  deletingAddress.value = null
}

async function confirmDelete() {
  const addr = deletingAddress.value
  if (!addr) return
  deleting.value = true
  try {
    await addressStore.remove(addr.id)
    showSuccessToast('已删除')
    showDeleteConfirm.value = false
    deletingAddress.value = null
  } catch {
    showToast('删除失败')
  } finally {
    deleting.value = false
  }
}

function openEditDetail(addr: UserAddress) {
  editingAddress.value = addr
  editingDetail.value = addr.detail || ''
  showEditDetail.value = true
}

async function handleEditConfirm(action: string): Promise<boolean> {
  if (action !== 'confirm') return true
  const addr = editingAddress.value
  if (!addr) return true
  try {
    await addressStore.update(addr.id, {
      label: addr.label,
      address: addr.address,
      detail: editingDetail.value.trim() || null,
      lat: addr.lat,
      lng: addr.lng,
    })
    showSuccessToast('已更新')
    return true
  } catch {
    showToast('更新失败')
    return false
  }
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
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.2s;

  &.is-default {
    background: linear-gradient(135deg, #fff9f0 0%, #fff 60%);
    border: 1px solid #ffe2b8;
    box-shadow: 0 2px 8px rgba(255, 196, 0, 0.12);
  }
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

.addr-pin {
  font-size: 16px;
  color: $primary;
  flex-shrink: 0;
}

.addr-label {
  font-size: 11px;
  color: #fff;
  background: $primary;
  padding: 2px 7px;
  border-radius: 4px;
  flex-shrink: 0;
  line-height: 1.4;
}

.addr-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.addr-detail {
  font-size: 13px;
  color: $text-secondary;
  padding-left: 22px;
}

.addr-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  padding-top: 10px;
  border-top: 1px solid #f5f5f5;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  color: $text-secondary;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 14px;
  transition: background 0.15s;

  &:active {
    background: #f5f5f5;
  }

  &.default-tag {
    color: #ed6a0c;
    background: #fff7e6;
  }

  &.danger {
    color: #ee0a24;
    margin-left: auto;

    &:active {
      background: #fff1f0;
    }
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

.edit-detail-body {
  padding: 12px 20px 4px;

  .edit-detail-addr {
    font-size: 13px;
    color: $text-secondary;
    margin-bottom: 10px;
    line-height: 1.4;
  }
}

.delete-confirm {
  padding: 20px 20px 16px;

  .delete-confirm-title {
    font-size: 16px;
    font-weight: 600;
    color: $text;
    text-align: center;
    margin-bottom: 18px;
  }

  .delete-confirm-addr {
    display: flex;
    gap: 10px;
    padding: 12px 14px;
    background: #f7f8fa;
    border-radius: 8px;
    margin-bottom: 20px;

    .van-icon {
      font-size: 18px;
      color: $primary;
      margin-top: 1px;
    }

    .delete-confirm-addr-text {
      flex: 1;
      min-width: 0;
      font-size: 14px;
      color: $text;
      line-height: 1.5;
      word-break: break-all;
    }

    .delete-confirm-detail {
      font-size: 13px;
      color: $text-secondary;
      margin-top: 2px;
    }
  }

  .delete-confirm-actions {
    display: flex;
    gap: 10px;

    .van-button {
      flex: 1;
    }
  }
}
</style>
