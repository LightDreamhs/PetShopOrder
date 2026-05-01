import { setupWorker } from 'msw/browser'
import { authHandlers } from './handlers/auth'
import { categoryHandlers } from './handlers/category'
import { productHandlers } from './handlers/product'
import { memberLevelHandlers } from './handlers/member-level'
import { memberHandlers } from './handlers/member'
import { orderHandlers } from './handlers/order'
import { fileHandlers } from './handlers/file'
import { systemConfigHandlers } from './handlers/system-config'
import { adminUserHandlers } from './handlers/admin-user'

export const worker = setupWorker(
  ...authHandlers,
  ...categoryHandlers,
  ...productHandlers,
  ...memberLevelHandlers,
  ...memberHandlers,
  ...orderHandlers,
  ...fileHandlers,
  ...systemConfigHandlers,
  ...adminUserHandlers,
)
