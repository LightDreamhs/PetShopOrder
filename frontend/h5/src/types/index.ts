// 通用 API 响应
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

// 分页响应
export interface Paginated<T> {
  list: T[]
  total: number
  page: number
  size: number
}

// 分类
export interface Category {
  id: number
  name: string
  icon: string | null
  type: 'GOODS' | 'SERVICE'
  sort: number
}

// 商品列表项
export interface Product {
  id: number
  name: string
  coverImg: string | null
  type: 'GOODS' | 'SERVICE'
  supportDelivery: boolean
  price: string
  dealPrice: string
  hasSpec: boolean
}

// SKU 价格
export interface SkuPrice {
  id: number
  specName: string
  price: string
  dealPrice: string
}

// 商品详情
export interface ProductDetail {
  id: number
  name: string
  description: string | null
  coverImg: string | null
  type: 'GOODS' | 'SERVICE'
  supportDelivery: boolean
  skus: SkuPrice[]
}

// 会员信息
export interface MemberProfile {
  isMember: boolean
  memberLevel: {
    name: string
    discountRate: string
  } | null
  serviceDiscountText: string | null
}

// 购物车项
export interface CartItem {
  productId: number
  skuId: number | null
  quantity: number
  // 缓存的展示数据
  productName: string
  productCoverImg: string | null
  skuName: string | null
  type: 'GOODS' | 'SERVICE'
  originalPrice: string
  dealPrice: string
}

// 购物车计算请求
export interface CartCalculateRequest {
  items: { productId: number; skuId: number | null; quantity: number }[]
  deliveryLat?: string
  deliveryLng?: string
}

// 计算后的单项
export interface CalculatedItem {
  productId: number
  skuId: number | null
  productName: string
  skuName: string | null
  type: 'GOODS' | 'SERVICE'
  originalPrice: string
  dealPrice: string
  quantity: number
  subtotal: string
}

// 配送校验
export interface DeliveryCheck {
  canDeliver: boolean
  deliverableGoodsOriginal: string
  minAmount: string
  reachedMinAmount: boolean
  gap: string | null
  deliveryDistanceMeter: number | null
  deliveryDistanceText: string | null
}

// 购物车计算响应
export interface CartCalculateResult {
  items: CalculatedItem[]
  goodsAmount: string
  serviceAmount: string
  deliveryFee: string | null
  totalAmount: string
  deliveryCheck: DeliveryCheck
}

// 下单请求
export interface CreateOrderRequest {
  items: { productId: number; skuId: number | null; quantity: number }[]
  customerName?: string
  remark?: string
  needDelivery: boolean
  deliveryLat?: string
  deliveryLng?: string
  deliveryAddress?: string
}

// 下单响应
export interface CreateOrderResult {
  orderNo: string
  totalAmount: string
  goodsAmount: string
  serviceAmount: string
  deliveryFee: string
  deliveryDistanceMeter: number | null
  deliveryDistanceText: string | null
}

// 订单列表项
export interface OrderListItem {
  id: number
  orderNo: string
  totalAmount: string
  goodsAmount: string
  serviceAmount: string
  needDelivery: boolean
  createTime: string
  itemCount: number
  summaryText: string
}

// 订单明细项
export interface OrderItemDetail {
  productName: string
  skuName: string | null
  type: 'GOODS' | 'SERVICE'
  originalPrice: string
  dealPrice: string
  quantity: number
  subtotal: string
}

// 订单详情
export interface OrderDetail {
  id: number
  orderNo: string
  customerPhone: string
  customerName: string | null
  memberLevelSnapshot: string | null
  goodsAmount: string
  serviceAmount: string
  deliveryFee: string
  totalAmount: string
  needDelivery: boolean
  deliveryAddress: string | null
  deliveryDistanceMeter: number | null
  deliveryDistanceText: string | null
  remark: string | null
  createTime: string
  items: OrderItemDetail[]
}

// 登录响应
export interface LoginResult {
  phone: string
  isNew: boolean
}

// 认证检查
export interface AuthCheck {
  loggedIn: boolean
  phone: string | null
}
