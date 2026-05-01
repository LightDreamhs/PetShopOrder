import { http, HttpResponse, delay } from 'msw'
import { members, getMemberNextId } from '../data/members'
import { memberLevels } from '../data/member-levels'

export const memberHandlers = [
  http.get('/api/admin/members', async ({ request }) => {
    await delay(300)
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page')) || 1
    const size = Number(url.searchParams.get('size')) || 20
    const keyword = url.searchParams.get('keyword')?.toLowerCase() || ''
    const levelId = url.searchParams.get('levelId')

    let filtered = members
    if (keyword) {
      filtered = filtered.filter(
        (m) => m.name.toLowerCase().includes(keyword) || m.phones.some((p) => p.includes(keyword)),
      )
    }
    if (levelId) filtered = filtered.filter((m) => m.levelId === Number(levelId))

    const start = (page - 1) * size
    const list = filtered.slice(start, start + size)

    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: { list, total: filtered.length, page, size },
    })
  }),

  http.post('/api/admin/members', async ({ request }) => {
    await delay(300)
    const body = (await request.json()) as any
    const level = memberLevels.find((l) => l.id === body.levelId)

    // 检查手机号唯一性
    const existingPhone = body.phones?.find((p: string) =>
      members.some((m) => m.phones.includes(p)),
    )
    if (existingPhone) {
      return HttpResponse.json({ code: 400, message: `手机号 ${existingPhone} 已被使用`, data: null })
    }

    const newMember = {
      id: getMemberNextId(),
      name: body.name,
      phones: body.phones,
      levelId: body.levelId,
      levelName: level?.name || '',
      remark: body.remark || null,
      createTime: new Date().toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).replace(/\//g, '-'),
    }
    members.push(newMember)
    if (level) level.memberCount++
    return HttpResponse.json({ code: 200, message: 'success', data: newMember })
  }),

  http.put('/api/admin/members/:id', async ({ request, params }) => {
    await delay(300)
    const id = Number(params.id)
    const body = (await request.json()) as any
    const member = members.find((m) => m.id === id)
    if (!member) {
      return HttpResponse.json({ code: 400, message: '会员不存在', data: null })
    }

    // 手机号唯一性检查（排除自己）
    if (body.phones) {
      const existingPhone = body.phones.find((p: string) =>
        members.some((m) => m.id !== id && m.phones.includes(p)),
      )
      if (existingPhone) {
        return HttpResponse.json({ code: 400, message: `手机号 ${existingPhone} 已被使用`, data: null })
      }
      member.phones = body.phones
    }
    if (body.name !== undefined) member.name = body.name
    if (body.levelId !== undefined) {
      const oldLevel = memberLevels.find((l) => l.id === member.levelId)
      if (oldLevel) oldLevel.memberCount = Math.max(0, oldLevel.memberCount - 1)
      member.levelId = body.levelId
      const newLevel = memberLevels.find((l) => l.id === body.levelId)
      member.levelName = newLevel?.name || ''
      if (newLevel) newLevel.memberCount++
    }
    if (body.remark !== undefined) member.remark = body.remark

    return HttpResponse.json({ code: 200, message: 'success', data: member })
  }),

  http.delete('/api/admin/members/:id', async ({ params }) => {
    await delay(200)
    const id = Number(params.id)
    const idx = members.findIndex((m) => m.id === id)
    if (idx === -1) {
      return HttpResponse.json({ code: 400, message: '会员不存在', data: null })
    }
    const [removed] = members.splice(idx, 1)
    const level = memberLevels.find((l) => l.id === removed.levelId)
    if (level) level.memberCount = Math.max(0, level.memberCount - 1)
    return HttpResponse.json({ code: 200, message: 'success', data: null })
  }),
]
