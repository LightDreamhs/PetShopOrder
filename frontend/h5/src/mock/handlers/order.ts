import { http, HttpResponse, delay } from 'msw'
import { products } from '../data/products'

let orderCounter = 0
const orders: any[] = []

export const orderHandlers = [
  http.post('/api/app/orders', async ({ request }) => {
    await delay(800)
    const body = await request.json() as any
    orderCounter++
    const orderNo = `PS${new Date().toISOString().slice(0, 10).replace(/-/g, '')}${String(orderCounter).padStart(3, '0')}`

    const calculatedItems = body.items.map((item: any) => {
      const product = products.find((p) => p.id === item.productId)
      const sku = product?.skus.find((s) => s.id === item.skuId)
      return {
        productName: product?.name ?? '未知商品',
        skuName: sku?.specName ?? null,
        type: product?.type ?? 'GOODS',
        originalPrice: sku?.price ?? '0.00',
        dealPrice: sku?.dealPrice ?? '0.00',
        quantity: item.quantity,
        subtotal: (parseFloat(sku?.dealPrice ?? '0') * item.quantity).toFixed(2),
      }
    })

    const goodsAmount = calculatedItems.filter((i: any) => i.type === 'GOODS').reduce((s: number, i: any) => s + parseFloat(i.subtotal), 0).toFixed(2)
    const serviceAmount = calculatedItems.filter((i: any) => i.type === 'SERVICE').reduce((s: number, i: any) => s + parseFloat(i.subtotal), 0).toFixed(2)

    let deliveryFee: string | null = '0.00'
    let deliveryDistanceMeter: number | null = 2300
    let deliveryDistanceText: string | null = '2.3km'
    if (body.needDelivery && deliveryDistanceMeter && deliveryDistanceMeter > 3000) {
      deliveryFee = '0.00'
    }
    if (!body.needDelivery) {
      deliveryDistanceMeter = null
      deliveryDistanceText = null
      deliveryFee = '0.00'
    }

    const totalAmount = (parseFloat(goodsAmount) + parseFloat(serviceAmount) + parseFloat(deliveryFee)).toFixed(2)

    const order = {
      id: orderCounter,
      orderNo,
      customerPhone: '138****8888',
      customerName: body.customerName ?? null,
      memberLevelSnapshot: '2000档会员',
      goodsAmount,
      serviceAmount,
      deliveryFee,
      totalAmount,
      needDelivery: body.needDelivery ?? false,
      deliveryAddress: body.deliveryAddress ?? null,
      deliveryDistanceMeter,
      deliveryDistanceText,
      remark: body.remark ?? null,
      createTime: new Date().toLocaleString('zh-CN', { hour12: false, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).replace(/\//g, '-'),
      items: calculatedItems,
      itemCount: calculatedItems.length,
      summaryText: `${calculatedItems[0]?.productName ?? ''}${calculatedItems.length > 1 ? ` 等${calculatedItems.length}件` : ''}`,
    }
    orders.unshift(order)

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: {
        orderNo,
        totalAmount,
        goodsAmount,
        serviceAmount,
        deliveryFee,
        deliveryDistanceMeter,
        deliveryDistanceText,
      },
    })
  }),

  http.get('/api/app/orders', async ({ request }) => {
    await delay(300)
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page')) || 1
    const size = Number(url.searchParams.get('size')) || 10

    const start = (page - 1) * size
    const end = start + size
    const list = orders.slice(start, end)

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: {
        list,
        total: orders.length,
        page,
        size,
      },
    })
  }),

  http.get('/api/app/orders/:id', async ({ params }) => {
    await delay(200)
    const id = Number(params.id)
    const order = orders.find((o) => o.id === id)
    if (!order) {
      return HttpResponse.json({ code: 404, message: '订单不存在', data: null })
    }
    return HttpResponse.json({ code: 200, message: 'success', data: order })
  }),
]
