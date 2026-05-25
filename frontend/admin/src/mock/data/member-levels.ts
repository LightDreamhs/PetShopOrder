let nextId = 5

export const memberLevels = [
  { id: 1, name: '500档会员', discountRate: '0.90', sort: 10, status: 'ENABLED' as const, memberCount: 2 },
  { id: 2, name: '1000档会员', discountRate: '0.85', sort: 20, status: 'ENABLED' as const, memberCount: 2 },
  { id: 3, name: '2000档会员', discountRate: '0.80', sort: 30, status: 'ENABLED' as const, memberCount: 1 },
  { id: 4, name: '5000档会员', discountRate: '0.70', sort: 40, status: 'ENABLED' as const, memberCount: 1 },
]

export function getMemberLevelNextId() {
  return nextId++
}
