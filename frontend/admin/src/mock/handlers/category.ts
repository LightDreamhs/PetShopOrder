import { http, HttpResponse, delay } from 'msw'
import { categories, getCategoryNextId } from '../data/categories'

export const categoryHandlers = [
  http.get('/api/admin/categories', async ({ request }) => {
    await delay(200)
    const url = new URL(request.url)
    const type = url.searchParams.get('type')
    const filtered = type ? categories.filter((c) => c.type === type) : categories
    return HttpResponse.json({ code: 200, message: 'success', data: filtered })
  }),

  http.post('/api/admin/categories', async ({ request }) => {
    await delay(200)
    const body = (await request.json()) as any
    const newCat = {
      id: getCategoryNextId(),
      name: body.name,
      icon: body.icon || null,
      type: body.type,
      sort: body.sort || 0,
      productCount: 0,
    }
    categories.push(newCat)
    return HttpResponse.json({ code: 200, message: 'success', data: newCat })
  }),

  http.put('/api/admin/categories/:id', async ({ request, params }) => {
    await delay(200)
    const id = Number(params.id)
    const body = (await request.json()) as any
    const cat = categories.find((c) => c.id === id)
    if (!cat) {
      return HttpResponse.json({ code: 400, message: '分类不存在', data: null })
    }
    Object.assign(cat, body)
    return HttpResponse.json({ code: 200, message: 'success', data: cat })
  }),

  http.delete('/api/admin/categories/:id', async ({ params }) => {
    await delay(200)
    const id = Number(params.id)
    const cat = categories.find((c) => c.id === id)
    if (!cat) {
      return HttpResponse.json({ code: 400, message: '分类不存在', data: null })
    }
    if (cat.productCount > 0) {
      return HttpResponse.json({ code: 400, message: '该分类下有商品，无法删除', data: null })
    }
    const idx = categories.findIndex((c) => c.id === id)
    categories.splice(idx, 1)
    return HttpResponse.json({ code: 200, message: 'success', data: null })
  }),
]
