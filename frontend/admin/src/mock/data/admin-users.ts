let nextId = 4

export const adminUsers = [
  { id: 1, username: 'admin', realName: '老板', role: 'BOSS', roleLabel: '老板', status: 'ENABLED' as const },
  { id: 2, username: 'manager01', realName: '王店长', role: 'MANAGER', roleLabel: '店长', status: 'ENABLED' as const },
  { id: 3, username: 'staff01', realName: '小李', role: 'STAFF', roleLabel: '店员', status: 'ENABLED' as const },
]

export function getAdminUserNextId() {
  return nextId++
}
