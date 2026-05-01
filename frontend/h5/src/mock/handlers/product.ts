import { http, HttpResponse, delay } from 'msw'
import { categories } from '../data/categories'
import { products } from '../data/products'

export const productHandlers = [
  http.get('/api/app/products', async ({ request }) => {
    await delay(300)
    const url = new URL(request.url)
    const type = url.searchParams.get('type') as 'GOODS' | 'SERVICE' | null
    const list = products
      .filter((p) => !type || p.type === type)
      .map((p) => ({
        id: p.id,
        name: p.name,
        coverImg: p.coverImg,
        type: p.type,
        supportDelivery: p.supportDelivery,
        price: p.skus.reduce((min, s) => (parseFloat(s.price) < parseFloat(min) ? s.price : min), p.skus[0].price),
        dealPrice: p.skus.reduce((min, s) => (parseFloat(s.dealPrice) < parseFloat(min) ? s.dealPrice : min), p.skus[0].dealPrice),
        hasSpec: p.skus.length > 1,
      }))
    return HttpResponse.json({ code: 200, message: 'success', data: list })
  }),

  http.get('/api/app/categories', async ({ request }) => {
    await delay(200)
    const url = new URL(request.url)
    const type = url.searchParams.get('type') as 'GOODS' | 'SERVICE' | null
    const list = type ? categories.filter((c) => c.type === type) : categories
    return HttpResponse.json({ code: 200, message: 'success', data: list })
  }),

  http.get('/api/app/categories/:categoryId/products', async ({ params }) => {
    await delay(300)
    const categoryId = Number(params.categoryId)
    const list = products
      .filter((p) => p.categoryId === categoryId)
      .map((p) => ({
        id: p.id,
        name: p.name,
        coverImg: p.coverImg,
        type: p.type,
        supportDelivery: p.supportDelivery,
        price: p.skus.reduce((min, s) => (parseFloat(s.price) < parseFloat(min) ? s.price : min), p.skus[0].price),
        dealPrice: p.skus.reduce((min, s) => (parseFloat(s.dealPrice) < parseFloat(min) ? s.dealPrice : min), p.skus[0].dealPrice),
        hasSpec: p.skus.length > 1,
      }))
    return HttpResponse.json({ code: 200, message: 'success', data: list })
  }),

  http.get('/api/app/products/:id', async ({ params }) => {
    await delay(200)
    const id = Number(params.id)
    const product = products.find((p) => p.id === id)
    if (!product) {
      return HttpResponse.json({ code: 404, message: '商品不存在', data: null })
    }
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: {
        id: product.id,
        name: product.name,
        description: `${product.name} - 优质宠物用品，您的爱宠值得拥有`,
        coverImg: product.coverImg,
        type: product.type,
        supportDelivery: product.supportDelivery,
        skus: product.skus,
      },
    })
  }),
]
