import { http, HttpResponse, delay } from 'msw'
import { orders, toListItem } from '../data/orders'

export const orderHandlers = [
  http.get('/api/admin/orders', async ({ request }) => {
    await delay(300)
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page')) || 1
    const size = Number(url.searchParams.get('size')) || 20
    const keyword = url.searchParams.get('keyword')?.toLowerCase() || ''
    const processed = url.searchParams.get('processed')
    const needDelivery = url.searchParams.get('needDelivery')
    const startTime = url.searchParams.get('startTime')
    const endTime = url.searchParams.get('endTime')

    let filtered = orders
    if (keyword) {
      filtered = filtered.filter(
        (o) =>
          o.orderNo.toLowerCase().includes(keyword) ||
          o.customerPhoneRaw.includes(keyword) ||
          o.customerPhone.includes(keyword) ||
          (o.customerName && o.customerName.toLowerCase().includes(keyword)),
      )
    }
    if (processed !== null && processed !== '') {
      filtered = filtered.filter((o) => o.processed === (processed === 'true'))
    }
    if (needDelivery !== null && needDelivery !== '') {
      filtered = filtered.filter((o) => o.needDelivery === (needDelivery === 'true'))
    }
    if (startTime) {
      filtered = filtered.filter((o) => o.createTime >= startTime)
    }
    if (endTime) {
      filtered = filtered.filter((o) => o.createTime <= endTime + ' 23:59')
    }

    const start = (page - 1) * size
    const list = filtered.slice(start, start + size).map(toListItem)

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: { list, total: filtered.length, page, size },
    })
  }),

  http.get('/api/admin/orders/:id', async ({ params }) => {
    await delay(200)
    const id = Number(params.id)
    const o = orders.find((o) => o.id === id)
    if (!o) {
      return HttpResponse.json({ code: 400, message: '订单不存在', data: null })
    }
    return HttpResponse.json({ code: 200, message: 'success', data: o })
  }),

  http.put('/api/admin/orders/:id/processed', async ({ request, params }) => {
    await delay(200)
    const id = Number(params.id)
    const body = (await request.json()) as any
    const o = orders.find((o) => o.id === id)
    if (!o) {
      return HttpResponse.json({ code: 400, message: '订单不存在', data: null })
    }
    o.processed = body.processed
    return HttpResponse.json({ code: 200, message: 'success', data: null })
  }),
]
