<template>
  <van-popup
    v-model:show="visible"
    position="center"
    :style="{ width: '78vw', borderRadius: '12px', overflow: 'visible', background: 'transparent' }"
  >
    <div class="ad-wrap" @click="onImageClick">
      <img :src="ad?.adImageUrl" class="ad-img" alt="广告" />
      <div class="ad-close" @click.stop="close">
        <van-icon name="cross" size="16" color="#fff" />
      </div>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface AdData {
  adImageUrl: string
  adLinkType: string
  adLinkTarget: string
}

const props = defineProps<{ show: boolean; ad: AdData | null }>()
const emit = defineEmits<{ 'update:show': [value: boolean] }>()

const visible = computed({
  get: () => props.show,
  set: (val) => emit('update:show', val),
})

function close() {
  visible.value = false
}

function onImageClick() {
  // 当前纯展示：点图即关。未来按 ad.adLinkType 扩展跳转（PRODUCT/URL）。
  // if (props.ad?.adLinkType === 'PRODUCT') router.push(...)
  // else if (props.ad?.adLinkType === 'URL') window.open(props.ad.adLinkTarget)
  close()
}
</script>

<style scoped lang="scss">
.ad-wrap {
  position: relative;
  width: 100%;
}

.ad-img {
  display: block;
  max-width: 100%;
  max-height: 65vh;
  border-radius: 12px;
}

.ad-close {
  position: absolute;
  top: -16px;
  right: -10px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  z-index: 2001;
}
</style>
