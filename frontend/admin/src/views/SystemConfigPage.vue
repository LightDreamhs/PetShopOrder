<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onBeforeRouteLeave } from 'vue-router'
import { getSystemConfig, testQywxWebhook, updateSystemConfig } from '@/api/system-config'
import { uploadFile } from '@/api/file'
import MapLocationPicker from '@/components/MapLocationPicker.vue'
import type {
  DeliveryFeeTierRule,
  DeliveryFeeType,
  SystemConfig,
  SystemConfigChangeLog,
  UpdateSystemConfigRequest,
} from '@/types'

const loading = ref(false)
const loaded = ref(false)
const loadFailed = ref(false)
const saving = ref(false)
const testingWebhook = ref(false)
const uploadingQr = ref(false)
const changeLogs = ref<SystemConfigChangeLog[]>([])
const initialSnapshot = ref('')
const webhookEdited = ref(false)
const webhookRawValue = ref('')
const showShopMapPicker = ref(false)
const shopAddressText = ref('')
const webhookMaskedFromServer = ref(false)

const form = reactive<UpdateSystemConfigRequest>({
  shopLat: null,
  shopLng: null,
  deliveryRadiusKm: 5,
  deliveryMinAmount: '20.00',
  deliveryFeeType: 'TIERED',
  fixedDeliveryFee: '0.00',
  tieredDeliveryFeeRules: [{ id: 1, minDistanceKm: 0, maxDistanceKm: 3, fee: '0.00' }],
  orderTimeEnabled: true,
  orderStartTime: '09:00',
  orderEndTime: '21:00',
  qywxWebhookUrl: '',
  paymentQrUrl: '',
})

const feeTypeOptions: Array<{ label: string; value: DeliveryFeeType }> = [
  { label: '免运费', value: 'FREE' },
  { label: '固定运费', value: 'FIXED' },
  { label: '分段运费', value: 'TIERED' },
]

const showFixedFee = computed(() => form.deliveryFeeType === 'FIXED')
const showTieredFee = computed(() => form.deliveryFeeType === 'TIERED')
const isDirty = computed(() => loaded.value && buildSnapshot() !== initialSnapshot.value)

function createTierRule(): DeliveryFeeTierRule {
  return {
    id: Date.now() + Math.floor(Math.random() * 1000),
    minDistanceKm: 0,
    maxDistanceKm: 3,
    fee: '0.00',
  }
}

function applyConfig(config: SystemConfig) {
  form.shopLat = config.shopLat
  form.shopLng = config.shopLng
  shopAddressText.value = ''
  form.deliveryRadiusKm = config.deliveryRadiusKm
  form.deliveryMinAmount = config.deliveryMinAmount
  form.deliveryFeeType = config.deliveryFeeType
  form.fixedDeliveryFee = config.fixedDeliveryFee
  form.tieredDeliveryFeeRules = config.tieredDeliveryFeeRules.length
    ? config.tieredDeliveryFeeRules.map((rule) => ({ ...rule }))
    : [createTierRule()]
  form.orderTimeEnabled = config.orderTimeEnabled
  form.orderStartTime = config.orderStartTime
  form.orderEndTime = config.orderEndTime
  webhookRawValue.value = (config.qywxWebhookUrl || '').trim()
  webhookMaskedFromServer.value = webhookRawValue.value.includes('key=***') || webhookRawValue.value.includes('hook/***')
  form.qywxWebhookUrl = webhookRawValue.value
    ? (webhookMaskedFromServer.value ? webhookRawValue.value : maskWebhookUrl(webhookRawValue.value))
    : ''
  webhookEdited.value = false
  form.paymentQrUrl = config.paymentQrUrl || ''
}

function normalizeMoney(value: string) {
  const num = Number(value)
  return Number.isFinite(num) ? num.toFixed(2) : '0.00'
}

function normalizeDistance(value: number) {
  const num = Number(value)
  if (!Number.isFinite(num)) return 0
  return Math.round(num * 100) / 100
}

function normalizeTimeValue(value: string) {
  const [h = '0', m = '0'] = value.split(':')
  return Number(h) * 60 + Number(m)
}

function getErrorMessage(error: unknown, fallback: string) {
  if (error && typeof error === 'object') {
    const err = error as { message?: string; response?: { data?: { message?: string } } }
    if (err.response?.data?.message) return err.response.data.message
    if (err.message) return err.message
  }
  return fallback
}

function maskWebhookUrl(url: string) {
  if (url.includes('open.feishu.cn')) {
    return url.replace(/hook\/[0-9a-f-]+/i, 'hook/***')
  }
  return url.replace(/([?&]key=)[^&]+/i, '$1***')
}

function isValidWebhookUrl(url: string) {
  if (/^https:\/\/open\.feishu\.cn\/open-apis\/bot\/v2\/hook\/[0-9a-f-]+$/i.test(url)) return true
  if (/^https:\/\/qyapi\.weixin\.qq\.com\/cgi-bin\/webhook\/send\?key=[A-Za-z0-9_-]+$/i.test(url)) return true
  return false
}

function resolveWebhookForSave(): string | undefined {
  const inputValue = form.qywxWebhookUrl?.trim() || ''
  if (webhookEdited.value) {
    return inputValue
  }
  if (!inputValue) {
    return ''
  }
  if (webhookMaskedFromServer.value) {
    // 后端只返回脱敏值时，前端不覆盖原值
    return undefined
  }
  return webhookRawValue.value
}

function resolveWebhookForTest(): string | null {
  const inputValue = form.qywxWebhookUrl?.trim() || ''
  if (webhookEdited.value) {
    return inputValue || null
  }
  if (!inputValue) {
    return null
  }
  if (webhookMaskedFromServer.value) {
    return null
  }
  return webhookRawValue.value || null
}

function handleWebhookInput() {
  webhookEdited.value = true
}

function buildSnapshot() {
  const tieredRules = [...form.tieredDeliveryFeeRules]
    .map((rule) => ({
      minDistanceKm: normalizeDistance(rule.minDistanceKm),
      maxDistanceKm: normalizeDistance(rule.maxDistanceKm),
      fee: normalizeMoney(rule.fee),
    }))
    .sort((a, b) => a.minDistanceKm - b.minDistanceKm)

  return JSON.stringify({
    shopLat: form.shopLat,
    shopLng: form.shopLng,
    deliveryRadiusKm: normalizeDistance(form.deliveryRadiusKm),
    deliveryMinAmount: normalizeMoney(form.deliveryMinAmount),
    deliveryFeeType: form.deliveryFeeType,
    fixedDeliveryFee: normalizeMoney(form.fixedDeliveryFee),
    tieredDeliveryFeeRules: tieredRules,
    orderTimeEnabled: form.orderTimeEnabled,
    orderStartTime: form.orderStartTime,
    orderEndTime: form.orderEndTime,
    qywxWebhookUrl: form.qywxWebhookUrl?.trim() || '',
    paymentQrUrl: form.paymentQrUrl || '',
    webhookEdited: webhookEdited.value,
  })
}

function validateForm() {
  if (form.deliveryRadiusKm <= 0) {
    ElMessage.warning('配送半径必须大于 0')
    return false
  }
  if (!Number.isFinite(Number(form.deliveryMinAmount)) || Number(form.deliveryMinAmount) < 0) {
    ElMessage.warning('起送价不能小于 0')
    return false
  }
  if (showFixedFee.value && (!Number.isFinite(Number(form.fixedDeliveryFee)) || Number(form.fixedDeliveryFee) < 0)) {
    ElMessage.warning('固定运费不能小于 0')
    return false
  }

  if (showTieredFee.value) {
    if (!form.tieredDeliveryFeeRules.length) {
      ElMessage.warning('请至少添加一条分段运费规则')
      return false
    }

    const sortedRules = [...form.tieredDeliveryFeeRules].sort((a, b) => a.minDistanceKm - b.minDistanceKm)
    for (const rule of sortedRules) {
      if (rule.minDistanceKm < 0 || rule.maxDistanceKm <= 0 || rule.maxDistanceKm <= rule.minDistanceKm) {
        ElMessage.warning('分段距离配置无效，请检查起止公里范围')
        return false
      }
      if (!Number.isFinite(Number(rule.fee)) || Number(rule.fee) < 0) {
        ElMessage.warning('分段运费不能小于 0')
        return false
      }
    }

    const EPS = 0.000001
    if (sortedRules[0].minDistanceKm > EPS) {
      ElMessage.warning('分段运费需从 0km 开始覆盖')
      return false
    }

    let coveredUntil = 0
    for (const rule of sortedRules) {
      if (rule.minDistanceKm > coveredUntil + EPS) {
        ElMessage.warning('分段运费存在覆盖缺口，请检查距离区间')
        return false
      }
      if (rule.minDistanceKm < coveredUntil - EPS) {
        ElMessage.warning('分段运费存在重叠区间，请检查距离区间')
        return false
      }
      coveredUntil = rule.maxDistanceKm
    }

    if (coveredUntil + EPS < form.deliveryRadiusKm) {
      ElMessage.warning('分段运费未覆盖完整配送半径')
      return false
    }
  }

  if (form.orderTimeEnabled && (!form.orderStartTime || !form.orderEndTime)) {
    ElMessage.warning('请完整设置接单开始与结束时间')
    return false
  }
  if (form.orderTimeEnabled && normalizeTimeValue(form.orderEndTime) <= normalizeTimeValue(form.orderStartTime)) {
    ElMessage.warning('接单结束时间必须晚于开始时间')
    return false
  }

  const webhookValue = resolveWebhookForSave()
  if (webhookValue && !isValidWebhookUrl(webhookValue)) {
    ElMessage.warning('Webhook 地址仅支持飞书（open.feishu.cn）或企业微信（qyapi.weixin.qq.com）机器人地址')
    return false
  }

  return true
}

async function fetchData() {
  loading.value = true
  loadFailed.value = false
  try {
    const res = await getSystemConfig()
    applyConfig(res.data.config)
    changeLogs.value = res.data.changeLogs
    initialSnapshot.value = buildSnapshot()
    loaded.value = true
  } catch (error) {
    loaded.value = false
    loadFailed.value = true
    changeLogs.value = []
    ElMessage.error(`配置加载失败：${getErrorMessage(error, '请稍后重试')}`)
  } finally {
    loading.value = false
  }
}

function addTierRule() {
  form.tieredDeliveryFeeRules.push(createTierRule())
}

function removeTierRule(index: number) {
  if (form.tieredDeliveryFeeRules.length <= 1) {
    ElMessage.warning('至少保留一条分段规则')
    return
  }
  form.tieredDeliveryFeeRules.splice(index, 1)
}

async function handleSave() {
  if (!validateForm()) return
  saving.value = true
  try {
    const payload: UpdateSystemConfigRequest = {
      shopLat: form.shopLat || undefined,
      shopLng: form.shopLng || undefined,
      deliveryRadiusKm: form.deliveryRadiusKm,
      deliveryMinAmount: normalizeMoney(form.deliveryMinAmount),
      deliveryFeeType: form.deliveryFeeType,
      fixedDeliveryFee: normalizeMoney(form.fixedDeliveryFee),
      tieredDeliveryFeeRules: form.tieredDeliveryFeeRules
        .map((rule) => ({
          id: rule.id,
          minDistanceKm: Number(rule.minDistanceKm),
          maxDistanceKm: Number(rule.maxDistanceKm),
          fee: normalizeMoney(rule.fee),
        }))
        .sort((a, b) => a.minDistanceKm - b.minDistanceKm),
      orderTimeEnabled: form.orderTimeEnabled,
      orderStartTime: form.orderStartTime,
      orderEndTime: form.orderEndTime,
    }
    const webhookValue = resolveWebhookForSave()
    if (webhookValue !== undefined) {
      payload.qywxWebhookUrl = webhookValue
    }
    payload.paymentQrUrl = form.paymentQrUrl || ''

    const res = await updateSystemConfig(payload)
    applyConfig(res.data)
    initialSnapshot.value = buildSnapshot()
    ElMessage.success('系统配置已保存')
    await fetchData()
  } catch (error) {
    ElMessage.error(`配置保存失败：${getErrorMessage(error, '请稍后重试')}`)
  } finally {
    saving.value = false
  }
}

async function handleTestWebhook() {
  const webhookValue = resolveWebhookForTest()
  if (!webhookValue) {
    if (!webhookEdited.value && webhookMaskedFromServer.value) {
      ElMessage.warning('当前仅有脱敏地址，请重新输入完整 Webhook 后再测试')
      return
    }
    ElMessage.warning('请先填写群机器人 Webhook 地址')
    return
  }
  if (!isValidWebhookUrl(webhookValue)) {
    ElMessage.warning('Webhook 地址仅支持飞书（open.feishu.cn）或企业微信（qyapi.weixin.qq.com）机器人地址')
    return
  }
  testingWebhook.value = true
  try {
    await testQywxWebhook(webhookValue)
    ElMessage.success('测试消息已发送')
  } catch (error) {
    ElMessage.error(`测试发送失败：${getErrorMessage(error, '请检查地址或网络后重试')}`)
  } finally {
    testingWebhook.value = false
  }
}

async function handleQrUpload(options: { file: File }) {
  uploadingQr.value = true
  try {
    const res = await uploadFile(options.file)
    form.paymentQrUrl = res.data.url
    ElMessage.success('收款码上传成功')
  } catch {
    ElMessage.error('上传失败')
  } finally {
    uploadingQr.value = false
  }
}

function handleQrRemove() {
  form.paymentQrUrl = ''
}

function handleShopLocationConfirm(data: { lat: string; lng: string; address: string }) {
  form.shopLat = data.lat
  form.shopLng = data.lng
  shopAddressText.value = data.address
}

onBeforeRouteLeave(async () => {
  if (!isDirty.value) return true
  try {
    await ElMessageBox.confirm('当前有未保存的系统配置，确认离开当前页面？', '未保存变更', {
      type: 'warning',
      confirmButtonText: '离开',
      cancelButtonText: '继续编辑',
    })
    return true
  } catch {
    return false
  }
})

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="system-config-page" v-loading="loading">
    <div v-if="loadFailed" class="page-card">
      <el-empty description="系统配置加载失败，请重试">
        <el-button type="primary" @click="fetchData">重新加载</el-button>
      </el-empty>
    </div>

    <div v-if="loaded" class="page-card">
      <div class="header-row">
        <div>
          <h3 class="block-title">系统配置</h3>
          <p class="block-desc">用于控制配送规则、接单时段与新订单通知。</p>
        </div>
        <el-button type="primary" :loading="saving" @click="handleSave">
          保存配置
        </el-button>
      </div>

      <el-form label-width="140px" class="config-form">
        <h4 class="section-title">配送规则</h4>
        <el-form-item label="店铺位置">
          <div class="shop-location-row">
            <el-input
              :model-value="shopAddressText || (form.shopLat ? `${form.shopLat}, ${form.shopLng}` : '')"
              placeholder="未设置，请选择店铺位置"
              readonly
              style="flex: 1"
            />
            <el-button @click="showShopMapPicker = true">地图选点</el-button>
          </div>
          <p class="shop-location-tip">用于计算配送距离，请设置为店铺实际位置</p>
        </el-form-item>
        <MapLocationPicker
          v-model="showShopMapPicker"
          :lat="form.shopLat"
          :lng="form.shopLng"
          @confirm="handleShopLocationConfirm"
        />

        <el-form-item label="配送半径（km）">
          <el-input-number v-model="form.deliveryRadiusKm" :min="1" :max="30" :precision="1" :step="0.5" />
        </el-form-item>

        <el-form-item label="起送价（元）">
          <el-input v-model="form.deliveryMinAmount" style="max-width: 280px">
            <template #append>元</template>
          </el-input>
        </el-form-item>

        <el-form-item label="运费策略">
          <el-radio-group v-model="form.deliveryFeeType">
            <el-radio-button
              v-for="item in feeTypeOptions"
              :key="item.value"
              :label="item.value"
            >
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="showFixedFee" label="固定运费（元）">
          <el-input v-model="form.fixedDeliveryFee" style="max-width: 280px">
            <template #append>元</template>
          </el-input>
        </el-form-item>

        <el-form-item v-if="showTieredFee" label="分段运费">
          <div class="tier-list">
            <div
              v-for="(rule, index) in form.tieredDeliveryFeeRules"
              :key="rule.id"
              class="tier-row"
            >
              <el-input-number v-model="rule.minDistanceKm" :min="0" :max="30" :step="0.5" />
              <span class="divider">~</span>
              <el-input-number v-model="rule.maxDistanceKm" :min="0.5" :max="30" :step="0.5" />
              <el-input v-model="rule.fee" class="fee-input">
                <template #append>元</template>
              </el-input>
              <el-button link type="danger" @click="removeTierRule(index)">删除</el-button>
            </div>
            <el-button size="small" @click="addTierRule">新增分段</el-button>
          </div>
        </el-form-item>

        <h4 class="section-title">接单设置</h4>
        <el-form-item label="限制接单时段">
          <el-switch v-model="form.orderTimeEnabled" />
        </el-form-item>

        <el-form-item v-if="form.orderTimeEnabled" label="开始时间">
          <el-time-select
            v-model="form.orderStartTime"
            start="00:00"
            step="00:30"
            end="23:30"
            style="width: 180px"
          />
        </el-form-item>

        <el-form-item v-if="form.orderTimeEnabled" label="结束时间">
          <el-time-select
            v-model="form.orderEndTime"
            start="00:00"
            step="00:30"
            end="23:30"
            style="width: 180px"
          />
        </el-form-item>

        <h4 class="section-title">通知配置</h4>
        <el-form-item label="群机器人 Webhook">
          <div class="webhook-row">
            <el-input
              v-model="form.qywxWebhookUrl"
              placeholder="支持飞书或企业微信机器人 Webhook 地址"
              @input="handleWebhookInput"
            />
            <el-button :loading="testingWebhook" :disabled="!form.qywxWebhookUrl" @click="handleTestWebhook">发送测试</el-button>
          </div>
        </el-form-item>

        <h4 class="section-title">收款设置</h4>
        <el-form-item label="收款二维码">
          <div class="qr-upload-area">
            <div v-if="form.paymentQrUrl" class="qr-preview">
              <el-image :src="form.paymentQrUrl" fit="contain" style="width: 160px; height: 160px; border: 1px solid #eee; border-radius: 4px" />
              <el-button link type="danger" @click="handleQrRemove">删除</el-button>
            </div>
            <el-upload
              v-else
              :show-file-list="false"
              accept="image/*"
              :http-request="handleQrUpload"
            >
              <el-button :loading="uploadingQr">上传收款码</el-button>
            </el-upload>
            <p class="qr-tip">顾客下单成功后将展示此收款码，支持随时更换</p>
          </div>
        </el-form-item>
      </el-form>
    </div>

    <div v-if="loaded" class="page-card">
      <h4 class="section-title">配置变更记录</h4>
      <el-table :data="changeLogs" stripe>
        <el-table-column prop="changedAt" label="时间" width="170" />
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column prop="summary" label="变更内容" min-width="320" />
      </el-table>
    </div>
  </div>
</template>

<style scoped lang="scss">
.system-config-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.block-title {
  font-size: 18px;
  color: #222;
  margin: 0;
}

.block-desc {
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
}

.config-form {
  margin-top: 18px;
}

.section-title {
  margin: 10px 0 16px;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.tier-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tier-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.divider {
  color: #909399;
}

.fee-input {
  width: 180px;
}

.webhook-row {
  width: 100%;
  display: flex;
  gap: 10px;
}

.qr-upload-area {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.qr-preview {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.qr-tip {
  color: #909399;
  font-size: 12px;
  margin: 0;
}

.shop-location-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.shop-location-tip {
  color: #909399;
  font-size: 12px;
  margin: 4px 0 0;
}
</style>
