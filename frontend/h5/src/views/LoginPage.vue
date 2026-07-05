<template>
  <div class="login-page">
    <div class="login-bg-pattern"></div>
    <div class="login-header">
      <div class="login-logo-wrap">
        <img class="login-logo" :src="shopLogo" alt="贰掌柜宠物店" />
      </div>
      <h1 class="login-title">贰掌柜宠物店</h1>
    </div>

    <div class="login-card">
      <van-cell-group inset>
        <van-field
          v-model="phone"
          type="tel"
          label="手机号"
          placeholder="请输入手机号"
          maxlength="11"
          clearable
          :error-message="phoneError"
        />
        <van-field
          v-model="code"
          type="digit"
          label="验证码"
          placeholder="请输入验证码"
          maxlength="4"
          :error-message="codeError"
        >
          <template #button>
            <van-button
              size="small"
              type="primary"
              :disabled="!canSendCode"
              @click="handleSendCode"
            >
              {{ sendButtonText }}
            </van-button>
          </template>
        </van-field>
      </van-cell-group>

      <div class="login-action">
        <van-button
          type="primary"
          block
          round
          :loading="loading"
          loading-text="登录中..."
          @click="handleLogin"
        >
          登录
        </van-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import { useAuthStore } from '@/stores/auth'
import { useSmsCountdown } from '@/composables/useSmsCountdown'
import shopLogo from '@/assets/shop-logo.jpg'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const phone = ref('')
const code = ref('')
const phoneError = ref('')
const codeError = ref('')
const loading = ref(false)

const { countdown, isCounting, start: startCountdown } = useSmsCountdown()

const canSendCode = computed(() => /^1\d{10}$/.test(phone.value) && !isCounting.value)
const sendButtonText = computed(() => (isCounting.value ? `${countdown.value}s 后重发` : '获取验证码'))

async function handleSendCode() {
  phoneError.value = ''
  if (!/^1\d{10}$/.test(phone.value)) {
    phoneError.value = '请输入正确的手机号'
    return
  }
  try {
    await authStore.sendSmsCode(phone.value)
    showToast('验证码已发送')
    startCountdown()
  } catch {
    // error handled by interceptor
  }
}

async function handleLogin() {
  phoneError.value = ''
  codeError.value = ''
  let valid = true
  if (!/^1\d{10}$/.test(phone.value)) {
    phoneError.value = '请输入正确的手机号'
    valid = false
  }
  if (!code.value || code.value.length < 4) {
    codeError.value = '请输入4位验证码'
    valid = false
  }
  if (!valid) return

  loading.value = true
  try {
    await authStore.login(phone.value, code.value)
    const redirect = (route.query.redirect as string) || '/'
    router.replace(redirect)
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  background: linear-gradient(160deg, #fdf8f3 0%, #f0e8df 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  position: relative;
  overflow: hidden;
}

.login-bg-pattern {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 20% 80%, rgba(255, 140, 50, 0.06) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(255, 100, 30, 0.04) 0%, transparent 40%);
  pointer-events: none;
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
  position: relative;
  z-index: 1;
}

.login-logo-wrap {
  width: 72px;
  height: 72px;
  margin: 0 auto 12px;
  background: linear-gradient(135deg, #ff8a3d, #ff6b1a);
  border-radius: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(255, 106, 0, 0.25);
  overflow: hidden;
}

.login-logo {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.login-title {
  font-size: 26px;
  font-weight: 700;
  margin: 0 0 8px;
  letter-spacing: 1px;
  color: #2d2420;
}

.login-subtitle {
  font-size: 13px;
  margin: 0;
  letter-spacing: 0.5px;
  color: #8a7d74;
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: linear-gradient(180deg, #fff7f0 0%, #fff 40%);
  border-radius: $radius-xl;
  padding: 24px 6px 20px;
  box-shadow: 0 8px 32px rgba(255, 106, 0, 0.1), 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(255, 140, 50, 0.12);
  position: relative;
  z-index: 1;

  :deep(.van-cell-group--inset) {
    margin: 0;
    border-radius: 0;
  }

  :deep(.van-cell) {
    padding: 14px 16px;
    font-size: 15px;
  }
}

.login-action {
  margin-top: 20px;
  padding: 0 16px;

  :deep(.van-button--primary) {
    height: 46px;
    font-size: 16px;
    font-weight: 600;
    letter-spacing: 1px;
  }
}
</style>
