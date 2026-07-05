<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  Document,
  Goods,
  User,
  Setting,
  UserFilled,
  Fold,
  Expand,
  SwitchButton,
  Calendar,
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isCollapsed = ref(false)
const menuKey = ref(0)
const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/orders')) return '/orders'
  if (path.startsWith('/bookings')) return '/bookings'
  if (path.startsWith('/products')) return '/products'
  if (path.startsWith('/members')) return '/members'
  if (path.startsWith('/settings')) return '/settings'
  if (path.startsWith('/accounts')) return '/accounts'
  return '/orders'
})

const menuItems = computed(() => {
  const allMenus = [
    { path: '/orders', title: '订单管理', icon: Document },
    { path: '/bookings', title: '预约管理', icon: Calendar },
    { path: '/products', title: '商品管理', icon: Goods, roles: ['BOSS', 'MANAGER'] },
    { path: '/members', title: '会员管理', icon: User, roles: ['BOSS', 'MANAGER'] },
    { path: '/settings', title: '系统配置', icon: Setting, roles: ['BOSS'] },
    { path: '/accounts', title: '账号管理', icon: UserFilled, roles: ['BOSS'] },
  ]
  return allMenus.filter(
    (item) => !item.roles || item.roles.includes(authStore.role!)
  )
})

async function handleMenuSelect(path: string) {
  if (path === route.path) return
  await router.push(path)
  // 导航完成后（无论成功还是被守卫取消），强制 el-menu 重新渲染以同步高亮
  menuKey.value++
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
              <circle cx="16" cy="16" r="14" fill="#ff5a00" opacity="0.15"/>
              <path d="M16 8c-2.5 0-4.5 1.5-5 3.5-.2.8.1 1.6.7 2.1L16 17l4.3-3.4c.6-.5.9-1.3.7-2.1C20.5 9.5 18.5 8 16 8z" fill="#ff5a00"/>
              <circle cx="11" cy="12" r="1.5" fill="#ff5a00" opacity="0.6"/>
              <circle cx="21" cy="12" r="1.5" fill="#ff5a00" opacity="0.6"/>
              <path d="M10 19c0 3.3 2.7 6 6 6s6-2.7 6-6" stroke="#ff5a00" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </div>
          <transition name="fade-text">
            <span v-show="!isCollapsed" class="logo-text">贰掌柜</span>
          </transition>
        </div>
      </div>

      <el-menu
        :key="menuKey"
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
      background: rgba(255, 90, 0, 0.15);
      color: #ff5a00;

      .el-icon {
        color: #ff5a00;
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
  background: linear-gradient(135deg, #ff5a00, #ff8a3d);
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
  border-color: rgba(255, 90, 0, 0.25);
  color: #ff5a00;
  background: rgba(255, 90, 0, 0.06);
  font-size: 11px;
  height: 20px;
  padding: 0 6px;
}

.logout-btn {
  color: #666;
  font-size: 13px;

  &:hover {
    color: #ff5a00;
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
</style>
