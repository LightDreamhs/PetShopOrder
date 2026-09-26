<template>
  <span v-if="visible" class="member-price-badge">
    <van-icon name="crown-o" size="10" />
    <span>会员价 ¥{{ memberPrice }}<template v-if="from"> 起</template></span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useMemberStore } from '@/stores/member'

const props = defineProps<{
  productType: 'GOODS' | 'SERVICE'
  memberPrice: string | null
  originalPrice: string
  from?: boolean
}>()

const memberStore = useMemberStore()

// 仅 GOODS 展示固定会员价：非会员 + 已设会员价 + 会员价低于售价
const visible = computed(
  () =>
    props.productType === 'GOODS' &&
    !memberStore.isMember &&
    !!props.memberPrice &&
    parseFloat(props.memberPrice) < parseFloat(props.originalPrice),
)
</script>

<style scoped lang="scss">
.member-price-badge {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  align-self: center;
  flex-shrink: 0;
  margin-left: 4px;
  padding: 2px 6px;
  background: $primary-light;
  color: $primary;
  font-size: 10.5px;
  font-weight: 600;
  border-radius: 4px;
  white-space: nowrap;
}
</style>
