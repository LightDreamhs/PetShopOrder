// ========== 通用 ==========
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface Paginated<T> {
  list: T[]
  total: number
  page: number
  size: number
}

// ========== 认证 ==========
export interface LoginParams {
  username: string
  password: string
}

export interface AdminProfile {
  id: number
  username: string
  realName: string
  role: 'BOSS' | 'MANAGER' | 'STAFF'
  roleLabel: string
}

// ========== 商品 ==========
export interface ProductListItem {
  id: number
  name: string
  coverImg: string | null
  type: 'GOODS' | 'SERVICE'
  status: 'ON_SALE' | 'OFF_SALE'
  supportDelivery: boolean
  sort: number
  skuCount: number
  minPrice: string
  createTime: string
}

export interface SkuDetail {
  id?: number
  specName: string
  price: string
  memberPrice: string | null
  sort: number
}

export interface ProductDetail {
  id: number
  name: string
  description: string | null
  coverImg: string | null
  type: 'GOODS' | 'SERVICE'
  status: 'ON_SALE' | 'OFF_SALE'
  supportDelivery: boolean
  sort: number
  createTime: string
  skus: SkuDetail[]
}

export interface ProductForm {
  name: string
  description?: string
  coverImg?: string
  type: 'GOODS' | 'SERVICE'
  supportDelivery?: boolean
  sort?: number
  skus?: SkuDetail[]
}

// ========== 会员等级 ==========
export interface MemberLevel {
  id: number
  name: string
  discountRate: string
  sort: number
  status: 'ENABLED' | 'DISABLED'
  memberCount: number
}

export interface MemberLevelForm {
  name: string
  discountRate: string
  sort?: number
}

// ========== 会员 ==========
export interface MemberListItem {
  id: number
  name: string
  phones: string[]
  levelId: number
  levelName: string
  remark: string | null
  createTime: string
}

export interface MemberForm {
  name: string
  phones: string[]
  levelId: number
  remark?: string
}

// ========== 订单 ==========
export interface OrderListItem {
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
  deliveryDistanceMeter: number | null
  deliveryDistanceText: string | null
  processed: boolean
  remark: string | null
  createTime: string
}

export interface OrderItemDetail {
  productName: string
  skuName: string | null
  type: 'GOODS' | 'SERVICE'
  originalPrice: string
  dealPrice: string
  quantity: number
  subtotal: string
}

export interface OrderDetail {
  id: number
  orderNo: string
  customerPhone: string
  customerPhoneRaw: string
  customerName: string | null
  memberLevelSnapshot: string | null
  goodsAmount: string
  serviceAmount: string
  deliveryFee: string
  totalAmount: string
  needDelivery: boolean
  deliveryAddress: string | null
  deliveryLat: string | null
  deliveryLng: string | null
  deliveryDistanceMeter: number | null
  deliveryDistanceText: string | null
  processed: boolean
  remark: string | null
  createTime: string
  items: OrderItemDetail[]
}

// ========== 文件上传 ==========
export interface UploadResult {
  url: string
  key: string
}

// ========== 系统配置 ==========
export type DeliveryFeeType = 'FREE' | 'FIXED' | 'TIERED'

export interface DeliveryFeeTierRule {
  id: number
  minDistanceKm: number
  maxDistanceKm: number
  fee: string
}

export interface SystemConfig {
  id: number
  deliveryRadiusKm: number
  deliveryMinAmount: string
  deliveryFeeType: DeliveryFeeType
  fixedDeliveryFee: string
  tieredDeliveryFeeRules: DeliveryFeeTierRule[]
  orderTimeEnabled: boolean
  orderStartTime: string
  orderEndTime: string
  qywxWebhookUrl: string | null
  updatedBy: string
  updatedAt: string
}

export interface SystemConfigChangeLog {
  id: number
  operatorName: string
  changedAt: string
  summary: string
}

export interface UpdateSystemConfigRequest {
  deliveryRadiusKm: number
  deliveryMinAmount: string
  deliveryFeeType: DeliveryFeeType
  fixedDeliveryFee: string
  tieredDeliveryFeeRules: DeliveryFeeTierRule[]
  orderTimeEnabled: boolean
  orderStartTime: string
  orderEndTime: string
  qywxWebhookUrl?: string
}

// ========== 账号管理 ==========
export interface AdminUser {
  id: number
  username: string
  realName: string
  role: 'BOSS' | 'MANAGER' | 'STAFF'
  roleLabel: string
  status: 'ENABLED' | 'DISABLED'
}

export interface AdminUserForm {
  username: string
  password: string
  realName: string
  role: 'MANAGER' | 'STAFF'
}
