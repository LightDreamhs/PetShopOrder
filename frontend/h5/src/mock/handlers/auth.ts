import { http, HttpResponse, delay } from 'msw'

const VALID_CODE = '1234'
const sessions: Map<string, { phone: string }> = new Map()
let sessionCounter = 0

export const authHandlers = [
  http.post('/api/app/auth/sms-code', async ({ request }) => {
    await delay(300)
    const body = await request.json() as { phone: string }
    if (!body.phone || !/^1\d{10}$/.test(body.phone)) {
      return HttpResponse.json({ code: 400, message: '手机号格式错误', data: null })
    }
    return HttpResponse.json({ code: 200, message: 'success', data: null })
  }),

  http.post('/api/app/auth/login', async ({ request }) => {
    await delay(500)
    const body = await request.json() as { phone: string; code: string }
    if (body.code !== VALID_CODE) {
      return HttpResponse.json({ code: 400, message: '验证码错误', data: null })
    }
    sessionCounter++
    const sid = `mock_sid_${sessionCounter}`
    sessions.set(sid, { phone: body.phone })
    const masked = body.phone.replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2')
    return HttpResponse.json(
      { code: 200, message: 'success', data: { phone: masked, isNew: false } },
      {
        headers: {
          'Set-Cookie': `petshop_mock_session=${sid}; Path=/; HttpOnly`,
        },
      },
    )
  }),

  http.post('/api/app/auth/logout', async () => {
    await delay(200)
    return HttpResponse.json(
      { code: 200, message: 'success', data: null },
      {
        headers: {
          'Set-Cookie': `petshop_mock_session=; Path=/; HttpOnly; Max-Age=0`,
        },
      },
    )
  }),

  http.get('/api/app/auth/check', async ({ cookies }) => {
    await delay(100)
    const sid = cookies.petshop_mock_session
    const session = sid ? sessions.get(sid) : null
    if (session) {
      return HttpResponse.json({
        code: 200,
        message: 'success',
        data: {
          loggedIn: true,
          phone: session.phone.replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2'),
        },
      })
    }
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: { loggedIn: false, phone: null },
    })
  }),
]
