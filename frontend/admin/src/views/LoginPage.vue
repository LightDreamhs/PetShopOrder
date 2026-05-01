<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  username: '',
  password: '',
})
const loading = ref(false)

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await authStore.login({ username: form.username, password: form.password })
    ElMessage.success('登录成功')
    router.push('/orders')
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <!-- 背景装饰 -->
    <div class="bg-orb bg-orb-1" />
    <div class="bg-orb bg-orb-2" />
    <div class="bg-orb bg-orb-3" />

    <!-- 网格纹理 -->
    <div class="bg-grid" />

    <div class="login-container">
      <!-- Logo 区域 -->
      <div class="login-brand">
        <div class="brand-icon">
          <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="24" cy="24" r="22" fill="#ff5a00" fill-opacity="0.12"/>
            <path d="M24 10c-3.5 0-6.5 2-7.2 5-.3 1.2.1 2.4 1 3.2L24 24l6.2-5.8c.9-.8 1.3-2 1-3.2C30.5 12 27.5 10 24 10z" fill="#ff5a00"/>
            <circle cx="16" cy="17" r="2" fill="#ff5a00" fill-opacity="0.5"/>
            <circle cx="32" cy="17" r="2" fill="#ff5a00" fill-opacity="0.5"/>
            <path d="M14 28c0 5.5 4.5 10 10 10s10-4.5 10-10" stroke="#ff5a00" stroke-width="2" stroke-linecap="round"/>
            <path d="M20 32c0 2.2 1.8 4 4 4s4-1.8 4-4" stroke="#ff5a00" stroke-width="1.5" stroke-linecap="round" opacity="0.5"/>
          </svg>
        </div>
        <h1 class="brand-title">贰掌柜宠物店</h1>
        <p class="brand-subtitle">管理后台</p>
      </div>

      <!-- 登录卡片 -->
      <div class="login-card">
        <h2 class="card-heading">账号登录</h2>
        <p class="card-desc">登录后即可管理商品、会员和订单</p>

        <el-form class="login-form" @submit.prevent="handleLogin">
          <el-form-item>
            <el-input
              v-model="form.username"
              placeholder="用户名"
              size="large"
              :prefix-icon="'User'"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item>
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              size="large"
              :prefix-icon="'Lock'"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="login-btn"
              :loading="loading"
              @click="handleLogin"
            >
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="card-footer">
          <span class="mock-hint">演示账号：admin / admin123</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0f0f1a;
  position: relative;
  overflow: hidden;
}

/* ========== 背景光晕 ========== */
.bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.35;
  pointer-events: none;
}

.bg-orb-1 {
  width: 500px;
  height: 500px;
  background: #ff5a00;
  top: -15%;
  right: -10%;
  animation: float-1 12s ease-in-out infinite;
}

.bg-orb-2 {
  width: 350px;
  height: 350px;
  background: #ff8a3d;
  bottom: -10%;
  left: -8%;
  animation: float-2 15s ease-in-out infinite;
}

.bg-orb-3 {
  width: 200px;
  height: 200px;
  background: #ffb366;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation: float-3 10s ease-in-out infinite;
  opacity: 0.15;
}

@keyframes float-1 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-30px, 20px); }
}

@keyframes float-2 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(20px, -30px); }
}

@keyframes float-3 {
  0%, 100% { transform: translate(-50%, -50%) scale(1); }
  50% { transform: translate(-50%, -50%) scale(1.3); }
}

/* ========== 网格纹理 ========== */
.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.02) 1px, transparent 1px);
  background-size: 60px 60px;
  pointer-events: none;
}

/* ========== 主容器 ========== */
.login-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 400px;
  padding: 0 20px;
  animation: container-in 0.6s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes container-in {
  from {
    opacity: 0;
    transform: translateY(24px) scale(0.97);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* ========== 品牌区 ========== */
.login-brand {
  text-align: center;
  margin-bottom: 32px;
}

.brand-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  animation: icon-in 0.5s 0.2s cubic-bezier(0.16, 1, 0.3, 1) both;

  svg {
    width: 100%;
    height: 100%;
  }
}

@keyframes icon-in {
  from {
    opacity: 0;
    transform: scale(0.6) rotate(-10deg);
  }
  to {
    opacity: 1;
    transform: scale(1) rotate(0deg);
  }
}

.brand-title {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 1px;
  margin: 0 0 4px;
  animation: text-in 0.5s 0.3s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.brand-subtitle {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
  margin: 0;
  letter-spacing: 3px;
  animation: text-in 0.5s 0.4s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes text-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ========== 登录卡片 ========== */
.login-card {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 36px 32px 28px;
  animation: card-in 0.6s 0.3s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes card-in {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.card-heading {
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  margin: 0 0 6px;
}

.card-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
  margin: 0 0 28px;
}

/* ========== 表单 ========== */
.login-form {
  :deep(.el-input__wrapper) {
    background: rgba(255, 255, 255, 0.06);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 10px;
    box-shadow: none;
    padding: 4px 12px;
    transition: all 0.25s;

    &:hover {
      border-color: rgba(255, 255, 255, 0.2);
    }

    &.is-focus {
      border-color: #ff5a00;
      box-shadow: 0 0 0 3px rgba(255, 90, 0, 0.12);
    }
  }

  :deep(.el-input__inner) {
    color: #fff;
    font-size: 14px;

    &::placeholder {
      color: rgba(255, 255, 255, 0.3);
    }
  }

  :deep(.el-input__prefix) {
    color: rgba(255, 255, 255, 0.35);
  }

  :deep(.el-form-item) {
    margin-bottom: 20px;
  }
}

/* ========== 按钮 ========== */
.login-btn {
  width: 100%;
  height: 46px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 4px;
  background: linear-gradient(135deg, #ff5a00, #ff7f33);
  border: none;
  transition: all 0.3s;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 20px rgba(255, 90, 0, 0.35);
  }

  &:active {
    transform: translateY(0);
  }
}

/* ========== 底部 ========== */
.card-footer {
  text-align: center;
  margin-top: 8px;
}

.mock-hint {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.25);
}
</style>
