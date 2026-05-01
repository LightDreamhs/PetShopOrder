import { http, HttpResponse, delay } from 'msw'
import { memberProfile } from '../data/member'

export const memberHandlers = [
  http.get('/api/app/member/profile', async () => {
    await delay(200)
    return HttpResponse.json({ code: 200, message: 'success', data: memberProfile })
  }),
]
