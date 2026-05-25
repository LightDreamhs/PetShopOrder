export const products = [
  // 狗粮
  {
    id: 1, categoryId: 1, name: '皇家金毛成犬粮 15kg', type: 'GOODS' as const,
    coverImg: null, supportDelivery: true,
    skus: [
      { id: 101, specName: '15kg', price: '368.00', dealPrice: '298.00' },
      { id: 102, specName: '3kg', price: '89.00', dealPrice: '72.00' },
    ],
  },
  {
    id: 2, categoryId: 1, name: '伯纳天纯中大型犬粮', type: 'GOODS' as const,
    coverImg: null, supportDelivery: true,
    skus: [
      { id: 103, specName: '10kg', price: '199.00', dealPrice: '168.00' },
    ],
  },
  {
    id: 3, categoryId: 1, name: '渴望六种鱼狗粮', type: 'GOODS' as const,
    coverImg: null, supportDelivery: true,
    skus: [
      { id: 104, specName: '11.4kg', price: '599.00', dealPrice: '519.00' },
      { id: 105, specName: '2kg', price: '139.00', dealPrice: '119.00' },
    ],
  },
  // 猫粮
  {
    id: 4, categoryId: 2, name: '皇家英短成猫粮', type: 'GOODS' as const,
    coverImg: null, supportDelivery: true,
    skus: [
      { id: 201, specName: '10kg', price: '329.00', dealPrice: '269.00' },
      { id: 202, specName: '2kg', price: '79.00', dealPrice: '65.00' },
    ],
  },
  {
    id: 5, categoryId: 2, name: '巅峰鸡肉猫粮', type: 'GOODS' as const,
    coverImg: null, supportDelivery: true,
    skus: [
      { id: 203, specName: '1kg', price: '189.00', dealPrice: '159.00' },
    ],
  },
  {
    id: 6, categoryId: 2, name: '渴望鸡肉猫粮', type: 'GOODS' as const,
    coverImg: null, supportDelivery: true,
    skus: [
      { id: 204, specName: '5.4kg', price: '469.00', dealPrice: '399.00' },
      { id: 205, specName: '1.8kg', price: '169.00', dealPrice: '145.00' },
    ],
  },
  // 零食
  {
    id: 7, categoryId: 3, name: '疯狂小狗鸡肉干', type: 'GOODS' as const,
    coverImg: null, supportDelivery: true,
    skus: [
      { id: 301, specName: '500g', price: '39.90', dealPrice: '32.90' },
    ],
  },
  {
    id: 8, categoryId: 3, name: '猫条零食', type: 'GOODS' as const,
    coverImg: null, supportDelivery: true,
    skus: [
      { id: 302, specName: '15支装', price: '25.00', dealPrice: '19.90' },
      { id: 303, specName: '30支装', price: '45.00', dealPrice: '36.00' },
    ],
  },
  {
    id: 9, categoryId: 3, name: '宠物洁齿棒', type: 'GOODS' as const,
    coverImg: null, supportDelivery: true,
    skus: [
      { id: 304, specName: '24根装', price: '29.90', dealPrice: '24.90' },
    ],
  },
  // 用品
  {
    id: 10, categoryId: 4, name: '宠物湿巾', type: 'GOODS' as const,
    coverImg: null, supportDelivery: true,
    skus: [
      { id: 401, specName: '80抽', price: '19.90', dealPrice: '15.90' },
    ],
  },
  {
    id: 11, categoryId: 4, name: '宠物尿垫', type: 'GOODS' as const,
    coverImg: null, supportDelivery: true,
    skus: [
      { id: 402, specName: '60×45cm 50片', price: '35.00', dealPrice: '28.00' },
      { id: 403, specName: '60×60cm 30片', price: '32.00', dealPrice: '26.00' },
    ],
  },
  {
    id: 12, categoryId: 4, name: '宠物牵引绳', type: 'GOODS' as const,
    coverImg: null, supportDelivery: true,
    skus: [
      { id: 404, specName: 'S号（小型犬）', price: '29.00', dealPrice: '24.00' },
      { id: 405, specName: 'M号（中型犬）', price: '35.00', dealPrice: '28.00' },
      { id: 406, specName: 'L号（大型犬）', price: '42.00', dealPrice: '35.00' },
    ],
  },
  // 洗护服务
  {
    id: 13, categoryId: 5, name: '洗澡服务', type: 'SERVICE' as const,
    coverImg: null, supportDelivery: false,
    skus: [
      { id: 501, specName: '小型犬（<10kg）', price: '79.00', dealPrice: '67.15' },
      { id: 502, specName: '中型犬（10-25kg）', price: '99.00', dealPrice: '84.15' },
      { id: 503, specName: '大型犬（>25kg）', price: '129.00', dealPrice: '109.65' },
    ],
  },
  {
    id: 14, categoryId: 5, name: '药浴服务', type: 'SERVICE' as const,
    coverImg: null, supportDelivery: false,
    skus: [
      { id: 504, specName: '小型犬', price: '119.00', dealPrice: '101.15' },
      { id: 505, specName: '中大型犬', price: '159.00', dealPrice: '135.15' },
    ],
  },
  // 美容服务
  {
    id: 15, categoryId: 6, name: '美容修剪', type: 'SERVICE' as const,
    coverImg: null, supportDelivery: false,
    skus: [
      { id: 601, specName: '基础护理', price: '139.00', dealPrice: '118.15' },
      { id: 602, specName: '全套精修', price: '199.00', dealPrice: '169.15' },
    ],
  },
  {
    id: 16, categoryId: 6, name: '猫洗澡+修剪', type: 'SERVICE' as const,
    coverImg: null, supportDelivery: false,
    skus: [
      { id: 603, specName: '短毛猫', price: '129.00', dealPrice: '109.65' },
      { id: 604, specName: '长毛猫', price: '169.00', dealPrice: '143.65' },
    ],
  },
]
