import { setupWorker } from 'msw/browser'
import { authHandlers } from './handlers/auth'
import { productHandlers } from './handlers/product'
import { memberHandlers } from './handlers/member'
import { cartHandlers } from './handlers/cart'
import { orderHandlers } from './handlers/order'

const handlers = [
  ...authHandlers,
  ...productHandlers,
  ...memberHandlers,
  ...cartHandlers,
  ...orderHandlers,
]

const worker = setupWorker(...handlers)

export async function setupMock() {
  await worker.start({
    onUnhandledRequest: 'bypass',
  })
}
