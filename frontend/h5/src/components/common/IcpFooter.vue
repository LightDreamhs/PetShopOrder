<template>
  <div class="icp-footer">
    <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">
      蜀ICP备2026037772号-1
    </a>
  </div>
</template>

<script setup lang="ts">
// 工信部 ICP 备案号展示，全站页脚统一悬挂
</script>

<style scoped lang="scss">
.icp-footer {
  padding: 12px 0 20px;
  text-align: center;
  font-size: 11px;
  line-height: 1.6;

  a {
    color: $text-muted;
    text-decoration: none;

    &:active {
      color: $primary;
    }
  }
}
</style>
