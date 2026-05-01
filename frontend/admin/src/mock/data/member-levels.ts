let nextId = 4

export const memberLevels = [
  { id: 1, name: '500档会员', discountRate: '0.95', sort: 1, status: 'ENABLED' as const, memberCount: 3 },
  { id: 2, name: '1000档会员', discountRate: '0.90', sort: 2, status: 'ENABLED' as const, memberCount: 2 },
  { id: 3, name: '2000档会员', discountRate: '0.85', sort: 3, status: 'ENABLED' as const, memberCount: 1 },
]

export function getMemberLevelNextId() {
  return nextId++
}
