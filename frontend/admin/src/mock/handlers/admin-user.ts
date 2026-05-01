import { http, HttpResponse, delay } from 'msw'
import { adminUsers, getAdminUserNextId } from '../data/admin-users'

const ROLE_LABELS: Record<string, string> = { BOSS: '老板', MANAGER: '店长', STAFF: '店员' }

export const adminUserHandlers = [
  http.get('/api/admin/users', async () => {
    await delay(200)
    return HttpResponse.json({ code: 200, message: 'success', data: adminUsers })
  }),

  http.post('/api/admin/users', async ({ request }) => {
    await delay(300)
    const body = (await request.json()) as any
    if (adminUsers.some((u) => u.username === body.username)) {
      return HttpResponse.json({ code: 400, message: '用户名已存在', data: null })
    }
    const newUser = {
      id: getAdminUserNextId(),
      username: body.username,
      realName: body.realName,
      role: body.role,
      roleLabel: ROLE_LABELS[body.role] || body.role,
      status: 'ENABLED' as const,
    }
    adminUsers.push(newUser)
    return HttpResponse.json({ code: 200, message: 'success', data: newUser })
  }),

  http.put('/api/admin/users/:id', async ({ request, params }) => {
    await delay(200)
    const id = Number(params.id)
    const body = (await request.json()) as any
    const user = adminUsers.find((u) => u.id === id)
    if (!user) {
      return HttpResponse.json({ code: 400, message: '账号不存在', data: null })
    }
    if (body.realName !== undefined) user.realName = body.realName
    if (body.role !== undefined) {
      user.role = body.role
      user.roleLabel = ROLE_LABELS[body.role] || body.role
    }
    return HttpResponse.json({ code: 200, message: 'success', data: user })
  }),

  http.put('/api/admin/users/:id/status', async ({ request, params }) => {
    await delay(200)
    const id = Number(params.id)
    const body = (await request.json()) as any
    const user = adminUsers.find((u) => u.id === id)
    if (!user) {
      return HttpResponse.json({ code: 400, message: '账号不存在', data: null })
    }
    if (user.role === 'BOSS') {
      return HttpResponse.json({ code: 400, message: 'BOSS 账号不可禁用', data: null })
    }
    user.status = body.status
    return HttpResponse.json({ code: 200, message: 'success', data: null })
  }),

  http.put('/api/admin/users/:id/password', async ({ request, params }) => {
    await delay(200)
    const id = Number(params.id)
    const body = (await request.json()) as any
    const user = adminUsers.find((u) => u.id === id)
    if (!user) {
      return HttpResponse.json({ code: 400, message: '账号不存在', data: null })
    }
    if (!body.newPassword || body.newPassword.length < 6) {
      return HttpResponse.json({ code: 400, message: '密码长度不能少于6位', data: null })
    }
    return HttpResponse.json({ code: 200, message: 'success', data: null })
  }),

  http.delete('/api/admin/users/:id', async ({ params }) => {
    await delay(200)
    const id = Number(params.id)
    const user = adminUsers.find((u) => u.id === id)
    if (!user) {
      return HttpResponse.json({ code: 400, message: '账号不存在', data: null })
    }
    if (user.role === 'BOSS') {
      return HttpResponse.json({ code: 400, message: 'BOSS 账号不可删除', data: null })
    }
    const idx = adminUsers.findIndex((u) => u.id === id)
    adminUsers.splice(idx, 1)
    return HttpResponse.json({ code: 200, message: 'success', data: null })
  }),
]
