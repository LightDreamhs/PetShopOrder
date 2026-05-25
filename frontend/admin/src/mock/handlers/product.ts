import { http, HttpResponse, delay } from 'msw'
import { products, toListItem, getProductNextId, getSkuNextId } from '../data/products'

export const productHandlers = [
  http.get('/api/admin/products', async ({ request }) => {
    await delay(300)
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page')) || 1
    const size = Number(url.searchParams.get('size')) || 20
    const keyword = url.searchParams.get('keyword')?.toLowerCase() || ''
    const type = url.searchParams.get('type')
    const status = url.searchParams.get('status')

    let filtered = products
    if (keyword) filtered = filtered.filter((p) => p.name.toLowerCase().includes(keyword))
    if (type) filtered = filtered.filter((p) => p.type === type)
    if (status) filtered = filtered.filter((p) => p.status === status)

    const start = (page - 1) * size
    const list = filtered.slice(start, start + size).map(toListItem)

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: { list, total: filtered.length, page, size },
    })
  }),

  http.get('/api/admin/products/:id', async ({ params }) => {
    await delay(200)
    const id = Number(params.id)
    const p = products.find((p) => p.id === id)
    if (!p) {
      return HttpResponse.json({ code: 400, message: '商品不存在', data: null })
    }
    return HttpResponse.json({ code: 200, message: 'success', data: p })
  }),

  http.post('/api/admin/products', async ({ request }) => {
    await delay(300)
    const body = (await request.json()) as any
    const newProduct = {
      id: getProductNextId(),
      name: body.name,
      description: body.description || null,
      coverImg: body.coverImg || null,
      type: body.type,
      status: 'ON_SALE' as const,
      supportDelivery: body.supportDelivery || false,
      sort: body.sort || 0,
      createTime: new Date().toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).replace(/\//g, '-'),
      skus: (body.skus || []).map((s: any) => ({ ...s, id: getSkuNextId() })),
    }
    products.push(newProduct)
    return HttpResponse.json({ code: 200, message: 'success', data: newProduct })
  }),

  http.put('/api/admin/products/:id', async ({ request, params }) => {
    await delay(300)
    const id = Number(params.id)
    const body = (await request.json()) as any
    const p = products.find((p) => p.id === id)
    if (!p) {
      return HttpResponse.json({ code: 400, message: '商品不存在', data: null })
    }
    if (body.name !== undefined) p.name = body.name
    if (body.description !== undefined) p.description = body.description
    if (body.coverImg !== undefined) p.coverImg = body.coverImg
    if (body.type !== undefined) p.type = body.type
    if (body.supportDelivery !== undefined) p.supportDelivery = body.supportDelivery
    if (body.sort !== undefined) p.sort = body.sort
    if (body.skus) {
      p.skus = body.skus.map((s: any) => ({
        ...s,
        id: s.id || getSkuNextId(),
      }))
    }
    return HttpResponse.json({ code: 200, message: 'success', data: p })
  }),

  http.put('/api/admin/products/:id/status', async ({ request, params }) => {
    await delay(200)
    const id = Number(params.id)
    const body = (await request.json()) as any
    const p = products.find((p) => p.id === id)
    if (!p) {
      return HttpResponse.json({ code: 400, message: '商品不存在', data: null })
    }
    p.status = body.status
    return HttpResponse.json({ code: 200, message: 'success', data: null })
  }),

  http.delete('/api/admin/products/:id', async ({ params }) => {
    await delay(200)
    const id = Number(params.id)
    const idx = products.findIndex((p) => p.id === id)
    if (idx === -1) {
      return HttpResponse.json({ code: 400, message: '商品不存在', data: null })
    }
    products.splice(idx, 1)
    return HttpResponse.json({ code: 200, message: 'success', data: null })
  }),
]
