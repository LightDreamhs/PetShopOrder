import { delay, http, HttpResponse } from 'msw'
import { systemConfig, systemConfigChangeLogs, updateSystemConfigData } from '../data/system-config'
import type { UpdateSystemConfigRequest } from '@/types'

export const systemConfigHandlers = [
  http.get('/api/admin/system-config', async () => {
    await delay(200)
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: {
        config: systemConfig,
        changeLogs: systemConfigChangeLogs.slice(0, 20),
      },
    })
  }),

  http.put('/api/admin/system-config', async ({ request }) => {
    await delay(300)
    const body = (await request.json()) as UpdateSystemConfigRequest

    if (!body.deliveryRadiusKm || body.deliveryRadiusKm <= 0) {
      return HttpResponse.json({ code: 400, message: '配送半径必须大于 0', data: null })
    }

    if (body.deliveryFeeType === 'FIXED' && Number(body.fixedDeliveryFee) < 0) {
      return HttpResponse.json({ code: 400, message: '固定运费不能小于 0', data: null })
    }

    if (body.deliveryFeeType === 'TIERED' && (!body.tieredDeliveryFeeRules || body.tieredDeliveryFeeRules.length === 0)) {
      return HttpResponse.json({ code: 400, message: '分段运费至少需要一条规则', data: null })
    }

    updateSystemConfigData(body, '老板')
    return HttpResponse.json({ code: 200, message: 'success', data: systemConfig })
  }),

  http.post('/api/admin/system-config/test-webhook', async ({ request }) => {
    await delay(400)
    const body = (await request.json()) as { webhookUrl?: string }
    if (!body.webhookUrl) {
      return HttpResponse.json({ code: 400, message: '请先填写 Webhook 地址', data: null })
    }
    return HttpResponse.json({
      code: 200,
      message: '测试消息已发送',
      data: { deliveredAt: new Date().toISOString() },
    })
  }),
]
