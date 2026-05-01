import type { ProductDetail, ProductListItem } from '@/types'

let nextId = 7
let nextSkuId = 20

export const products: ProductDetail[] = [
  {
    id: 1,
    categoryId: 1,
    categoryName: '狗粮',
    name: '皇家金毛成犬粮',
    description: '专为金毛寻回犬设计的成犬粮',
    coverImg: null,
    type: 'GOODS',
    status: 'ON_SALE',
    supportDelivery: true,
    sort: 1,
    createTime: '2026-04-15 10:30',
    skus: [
      { id: 1, specName: '5kg', price: '189.00', memberPrice: '168.00', stock: 50, sort: 1 },
      { id: 2, specName: '10kg', price: '329.00', memberPrice: '295.00', stock: 30, sort: 2 },
      { id: 3, specName: '15kg', price: '459.00', memberPrice: '412.00', stock: 20, sort: 3 },
    ],
  },
  {
    id: 2,
    categoryId: 1,
    categoryName: '狗粮',
    name: '伯纳天纯小型犬粮',
    description: '适合小型犬的天然粮',
    coverImg: null,
    type: 'GOODS',
    status: 'ON_SALE',
    supportDelivery: true,
    sort: 2,
    createTime: '2026-04-16 14:20',
    skus: [
      { id: 4, specName: '3kg', price: '128.00', memberPrice: '115.00', stock: 60, sort: 1 },
      { id: 5, specName: '8kg', price: '258.00', memberPrice: '232.00', stock: 40, sort: 2 },
    ],
  },
  {
    id: 3,
    categoryId: 2,
    categoryName: '猫粮',
    name: '渴望六种鱼猫粮',
    description: '高蛋白无谷猫粮',
    coverImg: null,
    type: 'GOODS',
    status: 'ON_SALE',
    supportDelivery: true,
    sort: 1,
    createTime: '2026-04-17 09:00',
    skus: [
      { id: 6, specName: '1.8kg', price: '199.00', memberPrice: '179.00', stock: 25, sort: 1 },
      { id: 7, specName: '5.4kg', price: '489.00', memberPrice: '440.00', stock: 15, sort: 2 },
    ],
  },
  {
    id: 4,
    categoryId: 3,
    categoryName: '洗护美容',
    name: '小型犬洗澡',
    description: '包含洗澡+吹干+基础梳理',
    coverImg: null,
    type: 'SERVICE',
    status: 'ON_SALE',
    supportDelivery: false,
    sort: 1,
    createTime: '2026-04-18 11:00',
    skus: [
      { id: 8, specName: '小型犬（5kg以下）', price: '50.00', memberPrice: null, stock: -1, sort: 1 },
      { id: 9, specName: '中型犬（5-15kg）', price: '80.00', memberPrice: null, stock: -1, sort: 2 },
      { id: 10, specName: '大型犬（15kg以上）', price: '120.00', memberPrice: null, stock: -1, sort: 3 },
    ],
  },
  {
    id: 5,
    categoryId: 3,
    categoryName: '洗护美容',
    name: '宠物美容造型',
    description: '洗澡+修剪+造型设计',
    coverImg: null,
    type: 'SERVICE',
    status: 'ON_SALE',
    supportDelivery: false,
    sort: 2,
    createTime: '2026-04-18 11:30',
    skus: [
      { id: 11, specName: '贵宾造型', price: '150.00', memberPrice: null, stock: -1, sort: 1 },
      { id: 12, specName: '比熊造型', price: '150.00', memberPrice: null, stock: -1, sort: 2 },
      { id: 13, specName: '博美造型', price: '120.00', memberPrice: null, stock: -1, sort: 3 },
    ],
  },
  {
    id: 6,
    categoryId: 4,
    categoryName: '寄养',
    name: '宠物日托',
    description: '含喂食、遛弯、基础护理',
    coverImg: null,
    type: 'SERVICE',
    status: 'OFF_SALE',
    supportDelivery: false,
    sort: 1,
    createTime: '2026-04-20 16:00',
    skus: [
      { id: 14, specName: '小型犬', price: '60.00', memberPrice: null, stock: -1, sort: 1 },
      { id: 15, specName: '中型犬', price: '80.00', memberPrice: null, stock: -1, sort: 2 },
      { id: 16, specName: '大型犬', price: '100.00', memberPrice: null, stock: -1, sort: 3 },
      { id: 17, specName: '猫', price: '50.00', memberPrice: null, stock: -1, sort: 4 },
    ],
  },
]

export function getProductNextId() {
  return nextId++
}

export function getSkuNextId() {
  return nextSkuId++
}

export function toListItem(p: ProductDetail): ProductListItem {
  const minPrice = p.skus.length ? p.skus.reduce((min, s) => (parseFloat(s.price) < parseFloat(min) ? s.price : min), p.skus[0].price) : '0.00'
  return {
    id: p.id,
    name: p.name,
    coverImg: p.coverImg,
    categoryName: p.categoryName,
    type: p.type,
    status: p.status,
    supportDelivery: p.supportDelivery,
    sort: p.sort,
    skuCount: p.skus.length,
    minPrice,
    createTime: p.createTime,
  }
}
