import { http, HttpResponse, delay } from 'msw'
import { memberLevels, getMemberLevelNextId } from '../data/member-levels'

export const memberLevelHandlers = [
  http.get('/api/admin/member-levels', async () => {
    await delay(200)
    return HttpResponse.json({ code: 200, message: 'success', data: memberLevels })
  }),

  http.post('/api/admin/member-levels', async ({ request }) => {
    await delay(200)
    const body = (await request.json()) as any
    const newLevel = {
      id: getMemberLevelNextId(),
      name: body.name,
      discountRate: body.discountRate,
      sort: body.sort || 0,
      status: 'ENABLED' as const,
      memberCount: 0,
    }
    memberLevels.push(newLevel)
    return HttpResponse.json({ code: 200, message: 'success', data: newLevel })
  }),

  http.put('/api/admin/member-levels/:id', async ({ request, params }) => {
    await delay(200)
    const id = Number(params.id)
    const body = (await request.json()) as any
    const level = memberLevels.find((l) => l.id === id)
    if (!level) {
      return HttpResponse.json({ code: 400, message: '等级不存在', data: null })
    }
    if (body.name !== undefined) level.name = body.name
    if (body.discountRate !== undefined) level.discountRate = body.discountRate
    if (body.sort !== undefined) level.sort = body.sort
    return HttpResponse.json({ code: 200, message: 'success', data: level })
  }),

  http.put('/api/admin/member-levels/:id/status', async ({ request, params }) => {
    await delay(200)
    const id = Number(params.id)
    const body = (await request.json()) as any
    const level = memberLevels.find((l) => l.id === id)
    if (!level) {
      return HttpResponse.json({ code: 400, message: '等级不存在', data: null })
    }
    if (body.status === 'DISABLED' && level.memberCount > 0) {
      return HttpResponse.json({ code: 400, message: '该等级下有会员，无法停用', data: null })
    }
    level.status = body.status
    return HttpResponse.json({ code: 200, message: 'success', data: null })
  }),

  http.delete('/api/admin/member-levels/:id', async ({ params }) => {
    await delay(200)
    const id = Number(params.id)
    const level = memberLevels.find((l) => l.id === id)
    if (!level) {
      return HttpResponse.json({ code: 400, message: '等级不存在', data: null })
    }
    if (level.memberCount > 0) {
      return HttpResponse.json({ code: 400, message: '该等级下有会员，无法删除', data: null })
    }
    const idx = memberLevels.findIndex((l) => l.id === id)
    memberLevels.splice(idx, 1)
    return HttpResponse.json({ code: 200, message: 'success', data: null })
  }),
]
