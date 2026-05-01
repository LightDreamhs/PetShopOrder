let nextId = 5

export const categories = [
  { id: 1, name: '狗粮', icon: null, type: 'GOODS', sort: 1, productCount: 3 },
  { id: 2, name: '猫粮', icon: null, type: 'GOODS', sort: 2, productCount: 2 },
  { id: 3, name: '洗护美容', icon: null, type: 'SERVICE', sort: 1, productCount: 3 },
  { id: 4, name: '寄养', icon: null, type: 'SERVICE', sort: 2, productCount: 1 },
]

export function getCategoryNextId() {
  return nextId++
}
