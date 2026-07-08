<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { Fold, Expand, SwitchButton } from '@element-plus/icons-vue'

interface MenuEntry {
  path: string
  title: string
  icon: string
  roles?: string[]
}

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isCollapsed = ref(false)

// 高亮：按前缀匹配当前路径，与路由表 path 对齐
const activeMenu = computed(() => {
  const path = route.path
  const matched = menuItems.value.find((item) => path === item.path || path.startsWith(item.path + '/'))
  return matched?.path ?? '/orders'
})

// 从路由表单一数据源生成菜单（避免与 router meta 双重维护）
// 仅取根布局下的子路由（有 title 且非登录页）
const menuItems = computed<MenuEntry[]>(() => {
  const rootRoute = router.options.routes.find((r) => r.path === '/' && r.children)
  const children = rootRoute?.children ?? []
  return children
    .filter((child) => Boolean(child.meta?.title) && Boolean(child.meta?.icon))
    .map((child) => ({
      path: `/${child.path}`,
      title: child.meta!.title as string,
      icon: child.meta!.icon as string,
      roles: child.meta!.roles as string[] | undefined,
    }))
    .filter((item) => !item.roles || item.roles.includes(authStore.role!))
})

function handleMenuSelect(path: string) {
  if (path !== route.path) router.push(path)
}

async function handleLogout() {
  await authStore.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="admin-layout">
    <!-- Sidebar -->
    <el-aside :width="isCollapsed ? '64px' : '220px'" class="sidebar">
      <div class="sidebar-header">
        <div class="logo-area">
          <div class="logo-icon">
            <svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="16" cy="16" r="14" fill="var(--brand-color)" opacity="0.15"/>
              <path d="M16 8c-2.5 0-4.5 1.5-5 3.5-.2.8.1 1.6.7 2.1L16 17l4.3-3.4c.6-.5.9-1.3.7-2.1C20.5 9.5 18.5 8 16 8z" fill="var(--brand-color)"/>
              <circle cx="11" cy="12" r="1.5" fill="var(--brand-color)" opacity="0.6"/>
              <circle cx="21" cy="12" r="1.5" fill="var(--brand-color)" opacity="0.6"/>
              <path d="M10 19c0 3.3 2.7 6 6 6s6-2.7 6-6" stroke="var(--brand-color)" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </div>
          <transition name="fade-text">
            <span v-show="!isCollapsed" class="logo-text">贰掌柜</span>
          </transition>
        </div>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        :collapse-transition="true"
        class="sidebar-menu"
        @select="handleMenuSelect"
      >
        <el-menu-item
          v-for="item in menuItems"
          :key="item.path"
          :index="item.path"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <div
          class="collapse-btn"
          @click="isCollapsed = !isCollapsed"
        >
          <el-icon :size="16">
            <component :is="isCollapsed ? Expand : Fold" />
          </el-icon>
          <span v-show="!isCollapsed" class="collapse-text">收起菜单</span>
        </div>
      </div>
    </el-aside>

    <!-- Main Area -->
    <el-container class="main-container">
      <!-- Header -->
      <el-header class="top-header">
        <div class="header-left">
          <h2 class="page-title">{{ route.meta.title }}</h2>
        </div>

        <div class="header-right">
          <div class="user-info">
            <div class="user-avatar">
              {{ authStore.realName?.charAt(0) || 'A' }}
            </div>
            <div class="user-detail">
              <span class="user-name">{{ authStore.realName }}</span>
              <el-tag size="small" class="role-tag" effect="plain">
                {{ authStore.roleLabel }}
              </el-tag>
            </div>
          </div>

          <el-divider direction="vertical" />

          <el-button
            text
            class="logout-btn"
            @click="handleLogout"
          >
            <el-icon><SwitchButton /></el-icon>
            退出
          </el-button>
        </div>
      </el-header>

      <!-- Content -->
      <el-main class="main-content">
        <router-view />

        <!-- 备案号 -->
        <div class="icp-footer">
          <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">蜀ICP备2026037772号-1</a>
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>

<style lang="scss" scoped>
.admin-layout {
  height: 100vh;
  overflow: hidden;
}

/* ========== Sidebar ========== */
.sidebar {
  background: #1a1a2e;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: width 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  border-right: none;
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.08);
}

.sidebar-header {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 10px;
  overflow: hidden;
  white-space: nowrap;
}

.logo-icon {
  width: 32px;
  height: 32px;
  flex-shrink: 0;

  svg {
    width: 100%;
    height: 100%;
  }
}

.logo-text {
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 1px;
}

.fade-text-enter-active,
.fade-text-leave-active {
  transition: opacity 0.2s ease;
}
.fade-text-enter-from,
.fade-text-leave-to {
  opacity: 0;
}

/* Menu */
.sidebar-menu {
  flex: 1;
  border-right: none;
  background: transparent;
  padding: 8px;

  :deep(.el-menu-item) {
    height: 44px;
    line-height: 44px;
    margin-bottom: 2px;
    border-radius: 8px;
    color: rgba(255, 255, 255, 0.6);
    font-size: 14px;

    .el-icon {
      font-size: 18px;
      color: inherit;
    }

    &:hover {
      background: rgba(255, 255, 255, 0.06);
      color: rgba(255, 255, 255, 0.9);
    }

    &.is-active {
      background: var(--el-color-primary-light-9);
      color: var(--brand-color);

      .el-icon {
        color: var(--brand-color);
      }
    }
  }

  &.el-menu--collapse {
    :deep(.el-menu-item) {
      padding: 0;
      justify-content: center;
    }
  }
}

/* Sidebar Footer */
.sidebar-footer {
  padding: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.collapse-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 36px;
  border-radius: 8px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.4);
  transition: all 0.2s;

  &:hover {
    background: rgba(255, 255, 255, 0.06);
    color: rgba(255, 255, 255, 0.7);
  }
}

.collapse-text {
  font-size: 12px;
}

/* ========== Main Container ========== */
.main-container {
  background: #f5f5f7;
}

/* ========== Header ========== */
.top-header {
  height: 64px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
}

.page-title {
  font-size: 17px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--brand-color), var(--brand-color-light));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
}

.user-detail {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.role-tag {
  border-color: var(--el-color-primary-light-7);
  color: var(--brand-color);
  background: var(--el-color-primary-light-9);
  font-size: 11px;
  height: 20px;
  padding: 0 6px;
}

.logout-btn {
  color: #666;
  font-size: 13px;

  &:hover {
    color: var(--brand-color);
  }

  .el-icon {
    margin-right: 4px;
  }
}

/* ========== Content ========== */
.main-content {
  padding: 20px;
  overflow-y: auto;
  background: #f5f5f7;
}

/* ========== 备案号 ========== */
.icp-footer {
  margin-top: 24px;
  padding: 12px 0;
  text-align: center;
  font-size: 12px;
  color: #999;

  a {
    color: inherit;
    text-decoration: none;
    transition: color 0.2s;

    &:hover {
      color: var(--brand-color);
    }
  }
}
</style>
