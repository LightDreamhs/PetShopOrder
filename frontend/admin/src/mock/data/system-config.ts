import type { DeliveryFeeTierRule, SystemConfig, SystemConfigChangeLog, UpdateSystemConfigRequest } from '@/types'

let nextLogId = 4
let nextTierRuleId = 4

function nowText() {
  return new Date()
    .toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
    .replace(/\//g, '-')
}

export let systemConfig: SystemConfig = {
  id: 1,
  deliveryRadiusKm: 5,
  deliveryMinAmount: '20.00',
  deliveryFeeType: 'TIERED',
  fixedDeliveryFee: '0.00',
  tieredDeliveryFeeRules: [
    { id: 1, minDistanceKm: 0, maxDistanceKm: 3, fee: '0.00' },
    { id: 2, minDistanceKm: 3, maxDistanceKm: 5, fee: '5.00' },
    { id: 3, minDistanceKm: 5, maxDistanceKm: 8, fee: '8.00' },
  ],
  orderTimeEnabled: true,
  orderStartTime: '09:00',
  orderEndTime: '21:00',
  qywxWebhookUrl: 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=mock-key',
  updatedBy: '老板',
  updatedAt: '2026-05-01 18:12',
}

export let systemConfigChangeLogs: SystemConfigChangeLog[] = [
  { id: 1, operatorName: '老板', changedAt: '2026-05-01 18:12', summary: '修改配送半径为 5km，起送价为 ¥20.00' },
  { id: 2, operatorName: '老板', changedAt: '2026-04-29 20:30', summary: '启用接单时段 09:00 - 21:00' },
  { id: 3, operatorName: '老板', changedAt: '2026-04-27 14:15', summary: '配置企微 Webhook 通知地址' },
]

function toMoneyText(value: string | number) {
  const num = typeof value === 'number' ? value : Number(value)
  return Number.isFinite(num) ? num.toFixed(2) : '0.00'
}

function withTierRuleId(rule: DeliveryFeeTierRule): DeliveryFeeTierRule {
  if (rule.id) return rule
  return { ...rule, id: nextTierRuleId++ }
}

export function updateSystemConfigData(payload: UpdateSystemConfigRequest, operatorName = '老板') {
  systemConfig = {
    ...systemConfig,
    deliveryRadiusKm: payload.deliveryRadiusKm,
    deliveryMinAmount: toMoneyText(payload.deliveryMinAmount),
    deliveryFeeType: payload.deliveryFeeType,
    fixedDeliveryFee: toMoneyText(payload.fixedDeliveryFee),
    tieredDeliveryFeeRules: payload.tieredDeliveryFeeRules.map(withTierRuleId),
    orderTimeEnabled: payload.orderTimeEnabled,
    orderStartTime: payload.orderStartTime,
    orderEndTime: payload.orderEndTime,
    qywxWebhookUrl: payload.qywxWebhookUrl || null,
    updatedBy: operatorName,
    updatedAt: nowText(),
  }

  let summary = `更新配送配置：半径 ${systemConfig.deliveryRadiusKm}km，起送 ¥${systemConfig.deliveryMinAmount}`
  if (systemConfig.deliveryFeeType === 'FREE') summary += '，运费策略：免运费'
  if (systemConfig.deliveryFeeType === 'FIXED') summary += `，运费策略：固定 ¥${systemConfig.fixedDeliveryFee}`
  if (systemConfig.deliveryFeeType === 'TIERED') summary += `，运费策略：分段 ${systemConfig.tieredDeliveryFeeRules.length} 条`

  systemConfigChangeLogs.unshift({
    id: nextLogId++,
    operatorName,
    changedAt: systemConfig.updatedAt,
    summary,
  })
}
