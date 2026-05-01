<template>
  <div class="category-sidebar">
    <div
      v-for="cat in categories"
      :key="cat.id"
      class="category-item"
      :class="{ active: modelValue === cat.id }"
      @click="$emit('update:modelValue', cat.id)"
    >
      <span class="category-name">{{ cat.name }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Category } from '@/types'

defineProps<{
  categories: Category[]
  modelValue: number
}>()

defineEmits<{
  'update:modelValue': [id: number]
}>()
</script>

<style scoped lang="scss">
.category-sidebar {
  width: 82px;
  flex-shrink: 0;
  background: #f7f7f7;
  height: calc(100vh - 98px);
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;

  &::-webkit-scrollbar {
    display: none;
  }
}

.category-item {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 15px 4px;
  font-size: 13px;
  color: $text-secondary;
  position: relative;
  transition: all 0.15s ease;
  text-align: center;
  line-height: 1.3;
  cursor: pointer;

  &:active {
    background: #f0f0f0;
  }

  &.active {
    background: #fff;
    color: $primary;
    font-weight: 700;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 3px;
      height: 18px;
      background: $primary;
      border-radius: 0 3px 3px 0;
    }
  }
}

.category-name {
  word-break: keep-all;
}
</style>
