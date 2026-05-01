<template>
  <van-cell
    title="配送地址"
    is-link
    :value="displayAddress || '选择配送地址'"
    @click="showPicker = true"
  />

  <van-popup v-model:show="showPicker" position="bottom" round :style="{ maxHeight: '60vh' }">
    <div class="address-picker">
      <div class="picker-header">
        <span class="picker-title">选择配送地址</span>
        <van-icon name="cross" @click="showPicker = false" />
      </div>
      <div class="picker-tip">
        <van-icon name="info-o" />
        <span>地图选点功能开发中，当前为模拟输入</span>
      </div>
      <van-cell-group inset>
        <van-field v-model="address" label="详细地址" placeholder="请输入详细地址" type="textarea" rows="2" />
        <van-field v-model="lat" label="纬度" placeholder="如 39.908823" type="number" />
        <van-field v-model="lng" label="经度" placeholder="如 116.397470" type="number" />
      </van-cell-group>
      <div class="picker-footer">
        <van-button type="primary" block round @click="confirmAddress">确认</van-button>
      </div>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

const emit = defineEmits<{
  confirm: [data: { address: string; lat: string; lng: string }]
}>()

const showPicker = ref(false)
const address = ref('')
const lat = ref('')
const lng = ref('')

const displayAddress = computed(() => address.value || '')

function confirmAddress() {
  if (!address.value) return
  emit('confirm', {
    address: address.value,
    lat: lat.value || '39.908823',
    lng: lng.value || '116.397470',
  })
  showPicker.value = false
}
</script>

<style scoped lang="scss">
.address-picker {
  padding: 16px;
}

.picker-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.picker-title {
  font-size: 16px;
  font-weight: 600;
}

.picker-tip {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: $text-muted;
  margin-bottom: 12px;
  padding: 0 4px;
}

.picker-footer {
  margin-top: 16px;
}
</style>
