import { http, HttpResponse, delay } from 'msw'

const ADMIN_ACCOUNT = {
  username: 'admin',
  password: 'admin123',
  profile: {
    id: 1,
    username: 'admin',
    realName: '老板',
    role: 'BOSS',
    roleLabel: '老板',
  },
}

export const authHandlers = [
  http.post('/api/admin/auth/login', async ({ request }) => {
    await delay(300)
    const body = (await request.json()) as any
    if (body.username === ADMIN_ACCOUNT.username && body.password === ADMIN_ACCOUNT.password) {
      return HttpResponse.json({ code: 200, message: 'success', data: ADMIN_ACCOUNT.profile })
    }
    return HttpResponse.json({ code: 400, message: '用户名或密码错误', data: null })
  }),

  http.post('/api/admin/auth/logout', async () => {
    await delay(100)
    return HttpResponse.json({ code: 200, message: 'success', data: null })
  }),

  http.get('/api/admin/auth/profile', async () => {
    await delay(100)
    return HttpResponse.json({ code: 200, message: 'success', data: ADMIN_ACCOUNT.profile })
  }),
]
