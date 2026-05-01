import { http, HttpResponse, delay } from 'msw'

export const fileHandlers = [
  http.post('/api/admin/files/upload', async () => {
    await delay(500)
    return HttpResponse.json({
      code: 200,
      message: 'success',
      data: {
        url: 'https://placehold.co/400x400/ff5a00/white?text=Pet+Shop',
        key: 'mock-image-' + Date.now(),
      },
    })
  }),

  http.delete('/api/admin/files/:key', async () => {
    await delay(200)
    return HttpResponse.json({ code: 200, message: 'success', data: null })
  }),
]
