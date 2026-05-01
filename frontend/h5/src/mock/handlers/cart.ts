import { http, HttpResponse, delay } from 'msw'
import { products } from '../data/products'

export const cartHandlers = [
  http.post('/api/app/cart/calculate', async ({ request }) => {
    await delay(300)
    const body = await request.json() as {
      items: { productId: number; skuId: number | null; quantity: number }[]
      deliveryLat?: string
      deliveryLng?: string
    }

    const calculatedItems = body.items.map((item) => {
      const product = products.find((p) => p.id === item.productId)
      const sku = product?.skus.find((s) => s.id === item.skuId)
      if (!product || !sku) return null
      const subtotal = (parseFloat(sku.dealPrice) * item.quantity).toFixed(2)
      return {
        productId: item.productId,
        skuId: item.skuId,
        productName: product.name,
        skuName: sku.specName,
        type: product.type,
        originalPrice: sku.price,
        dealPrice: sku.dealPrice,
        quantity: item.quantity,
        subtotal,
      }
    }).filter(Boolean)

    const goodsItems = calculatedItems.filter((i) => i!.type === 'GOODS')
    const serviceItems = calculatedItems.filter((i) => i!.type === 'SERVICE')

    const goodsAmount = goodsItems.reduce((sum, i) => sum + parseFloat(i!.subtotal), 0).toFixed(2)
    const serviceAmount = serviceItems.reduce((sum, i) => sum + parseFloat(i!.subtotal), 0).toFixed(2)
    const goodsOriginal = goodsItems.reduce((sum, i) => sum + parseFloat(i!.originalPrice) * i!.quantity, 0)

    const hasDeliveryCoords = body.deliveryLat && body.deliveryLng
    let deliveryFee: string | null = null
    let deliveryDistanceMeter: number | null = null
    let deliveryDistanceText: string | null = null

    if (hasDeliveryCoords && goodsItems.length > 0) {
      // 模拟距离：随机 1.5-5km
      const distance = 2300
      deliveryDistanceMeter = distance
      deliveryDistanceText = distance <= 1000 ? `${distance}m` : `${(distance / 1000).toFixed(1)}km`
      deliveryFee = distance <= 3000 ? '0.00' : null
    }

    const totalAmount = (parseFloat(goodsAmount) + parseFloat(serviceAmount) + (deliveryFee ? parseFloat(deliveryFee) : 0)).toFixed(2)
    const minAmount = 20

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: {
        items: calculatedItems,
        goodsAmount,
        serviceAmount,
        deliveryFee,
        totalAmount,
        deliveryCheck: {
          canDeliver: goodsItems.length > 0,
          deliverableGoodsOriginal: goodsOriginal.toFixed(2),
          minAmount: minAmount.toFixed(2),
          reachedMinAmount: goodsOriginal >= minAmount,
          gap: goodsOriginal < minAmount ? (minAmount - goodsOriginal).toFixed(2) : null,
          deliveryDistanceMeter,
          deliveryDistanceText,
        },
      },
    })
  }),
]
