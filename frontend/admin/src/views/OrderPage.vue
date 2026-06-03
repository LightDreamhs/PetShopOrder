<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { getOrders, getOrder, updateOrderProcessed, getNewOrderCount } from '@/api/order'
import type { OrderListItem, OrderDetail } from '@/types'

const loading = ref(false)
const orders = ref<OrderListItem[]>([])
const total = ref(0)

const filters = reactive({
  keyword: '',
  processed: undefined as boolean | undefined,
  needDelivery: undefined as boolean | undefined,
  dateRange: null as [string, string] | null,
  page: 1,
  size: 20,
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const currentDetail = ref<OrderDetail | null>(null)

// 新订单轮询
const POLL_INTERVAL = 15_000
let pollTimer: ReturnType<typeof setInterval> | null = null
let lastCheckTime = ''

function formatNow(): string {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ` +
    `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}

async function pollNewOrders() {
  if (!lastCheckTime) return
  try {
    const res = await getNewOrderCount(lastCheckTime)
    if (res.data.count > 0) {
      lastCheckTime = formatNow()
      fetchOrders()
      ElNotification({
        title: '新订单通知',
        message: '有新订单来了',
        type: 'warning',
        duration: 5000,
        position: 'bottom-right',
        onClick: () => {
          if (filters.page !== 1) {
            filters.page = 1
            fetchOrders()
          }
        },
      })
    } else {
      lastCheckTime = formatNow()
    }
  } catch {
    // 静默失败，不干扰用户
  }
}

function startPolling() {
  stopPolling()
  pollTimer = setInterval(pollNewOrders, POLL_INTERVAL)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function handleVisibilityChange() {
  if (document.hidden) {
    stopPolling()
  } else {
    pollNewOrders()
    startPolling()
  }
}

async function fetchOrders() {
  loading.value = true
  try {
    const params: any = {
      page: filters.page,
      size: filters.size,
    }
    if (filters.keyword) params.keyword = filters.keyword
    if (filters.processed !== undefined) params.processed = filters.processed
    if (filters.needDelivery !== undefined) params.needDelivery = filters.needDelivery
    if (filters.dateRange) {
      params.startTime = filters.dateRange[0]
      params.endTime = filters.dateRange[1]
    }
    const res = await getOrders(params)
    orders.value = res.data.list
    total.value = res.data.total
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  filters.page = 1
  fetchOrders()
}

function handleReset() {
  filters.keyword = ''
  filters.processed = undefined
  filters.needDelivery = undefined
  filters.dateRange = null
  filters.page = 1
  fetchOrders()
}

function handlePageChange(page: number) {
  filters.page = page
  fetchOrders()
}

function handleSizeChange(size: number) {
  filters.size = size
  filters.page = 1
  fetchOrders()
}

async function handleViewDetail(id: number) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await getOrder(id)
    currentDetail.value = res.data
  } catch {
    // handled by interceptor
  } finally {
    detailLoading.value = false
  }
}

async function handleToggleProcessed(row: OrderListItem) {
  const newStatus = !row.processed
  const action = newStatus ? '标记为已处理' : '标记为未处理'
  try {
    await ElMessageBox.confirm(`确认将订单 ${row.orderNo} ${action}？`, '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await updateOrderProcessed(row.id, newStatus)
    ElMessage.success(`${action}成功`)
    fetchOrders()
    // 如果详情抽屉打开且是同一订单，刷新详情
    if (currentDetail.value?.id === row.id) {
      const res = await getOrder(row.id)
      currentDetail.value = res.data
    }
  } catch {
    // cancelled or error
  }
}

function formatAmount(val: string) {
  return `¥${val}`
}

onMounted(() => {
  fetchOrders()
  lastCheckTime = formatNow()
  startPolling()
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onBeforeUnmount(() => {
  stopPolling()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<template>
  <div class="order-page">
    <!-- 筛选栏 -->
    <div class="page-card filter-card">
      <div class="filter-bar">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索订单号 / 手机号 / 联系人"
          clearable
          style="width: 260px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select
          v-model="filters.processed"
          placeholder="处理状态"
          clearable
          style="width: 130px"
          @change="handleSearch"
        >
          <el-option label="未处理" :value="false" />
          <el-option label="已处理" :value="true" />
        </el-select>

        <el-select
          v-model="filters.needDelivery"
          placeholder="配送状态"
          clearable
          style="width: 130px"
          @change="handleSearch"
        >
          <el-option label="配送订单" :value="true" />
          <el-option label="到店自取" :value="false" />
        </el-select>

        <el-date-picker
          v-model="filters.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 260px"
          @change="handleSearch"
        />

        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <!-- 订单表格 -->
    <div class="page-card table-card">
      <el-table
        :data="orders"
        v-loading="loading"
        stripe
        style="width: 100%"
        row-key="id"
        @row-click="(row: OrderListItem) => handleViewDetail(row.id)"
      >
        <el-table-column prop="orderNo" label="订单号" width="160" fixed>
          <template #default="{ row }">
            <span class="order-no">{{ row.orderNo }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="customerPhone" label="手机号" width="130" />

        <el-table-column prop="customerName" label="联系人" width="90">
          <template #default="{ row }">
            {{ row.customerName || '-' }}
          </template>
        </el-table-column>

        <el-table-column prop="memberLevelSnapshot" label="会员" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.memberLevelSnapshot" size="small" type="warning" effect="plain">
              {{ row.memberLevelSnapshot }}
            </el-tag>
            <span v-else class="text-muted">非会员</span>
          </template>
        </el-table-column>

        <el-table-column prop="goodsAmount" label="用品" width="90" align="right">
          <template #default="{ row }">
            <span>{{ formatAmount(row.goodsAmount) }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="serviceAmount" label="服务" width="90" align="right">
          <template #default="{ row }">
            <span>{{ formatAmount(row.serviceAmount) }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="deliveryFee" label="配送费" width="90" align="right">
          <template #default="{ row }">
            <span>{{ row.deliveryFee === '0.00' ? '免运费' : formatAmount(row.deliveryFee) }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="totalAmount" label="合计" width="100" align="right">
          <template #default="{ row }">
            <span class="text-orange font-bold">{{ formatAmount(row.totalAmount) }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="needDelivery" label="配送" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.needDelivery" size="small" type="success" effect="plain">
              {{ row.deliveryDistanceText || '配送' }}
            </el-tag>
            <span v-else class="text-muted">到店</span>
          </template>
        </el-table-column>

        <el-table-column prop="processed" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.processed ? 'success' : 'danger'"
              size="small"
              effect="dark"
            >
              {{ row.processed ? '已处理' : '未处理' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="下单时间" width="160" />

        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              @click.stop="handleViewDetail(row.id)"
            >
              详情
            </el-button>
            <el-button
              link
              :type="row.processed ? 'warning' : 'success'"
              size="small"
              @click.stop="handleToggleProcessed(row)"
            >
              {{ row.processed ? '标为未处理' : '标为已处理' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="filters.page"
        v-model:page-size="filters.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- 订单详情抽屉 -->
    <el-drawer
      v-model="detailVisible"
      title="订单详情"
      direction="rtl"
      size="480px"
      :destroy-on-close="true"
    >
      <div v-loading="detailLoading">
        <template v-if="currentDetail">
          <!-- 基本信息 -->
          <div class="detail-section">
            <h4 class="detail-heading">基本信息</h4>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="订单号">{{ currentDetail.orderNo }}</el-descriptions-item>
              <el-descriptions-item label="手机号">{{ currentDetail.customerPhoneRaw }}</el-descriptions-item>
              <el-descriptions-item label="联系人">{{ currentDetail.customerName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="会员等级">
                <el-tag v-if="currentDetail.memberLevelSnapshot" size="small" type="warning">
                  {{ currentDetail.memberLevelSnapshot }}
                </el-tag>
                <span v-else>非会员</span>
              </el-descriptions-item>
              <el-descriptions-item label="下单时间">{{ currentDetail.createTime }}</el-descriptions-item>
              <el-descriptions-item label="处理状态">
                <el-tag :type="currentDetail.processed ? 'success' : 'danger'" size="small" effect="dark">
                  {{ currentDetail.processed ? '已处理' : '未处理' }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- 商品明细 -->
          <div class="detail-section">
            <h4 class="detail-heading">商品明细</h4>
            <el-table :data="currentDetail.items" size="small" border>
              <el-table-column prop="productName" label="商品" min-width="120">
                <template #default="{ row }">
                  {{ row.productName }}
                  <el-tag v-if="row.skuName" size="small" type="info" effect="plain" style="margin-left: 4px">
                    {{ row.skuName }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="type" label="类型" width="60" align="center">
                <template #default="{ row }">
                  {{ row.type === 'GOODS' ? '用品' : '服务' }}
                </template>
              </el-table-column>
              <el-table-column label="单价" width="90" align="right">
                <template #default="{ row }">
                  <div>
                    <span class="text-orange">{{ formatAmount(row.dealPrice) }}</span>
                  </div>
                  <div v-if="row.dealPrice !== row.originalPrice" class="text-muted text-xs line-through">
                    {{ formatAmount(row.originalPrice) }}
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="quantity" label="数量" width="50" align="center" />
              <el-table-column label="小计" width="90" align="right">
                <template #default="{ row }">
                  {{ formatAmount(row.subtotal) }}
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 费用汇总 -->
          <div class="detail-section">
            <h4 class="detail-heading">费用汇总</h4>
            <div class="fee-list">
              <div class="fee-row">
                <span>用品金额</span>
                <span>{{ formatAmount(currentDetail.goodsAmount) }}</span>
              </div>
              <div class="fee-row">
                <span>服务金额</span>
                <span>{{ formatAmount(currentDetail.serviceAmount) }}</span>
              </div>
              <div class="fee-row">
                <span>配送费</span>
                <span>{{ currentDetail.deliveryFee === '0.00' ? '免运费' : formatAmount(currentDetail.deliveryFee) }}</span>
              </div>
              <div class="fee-row fee-total">
                <span>合计</span>
                <span class="text-orange">{{ formatAmount(currentDetail.totalAmount) }}</span>
              </div>
            </div>
          </div>

          <!-- 配送信息 -->
          <div v-if="currentDetail.needDelivery" class="detail-section">
            <h4 class="detail-heading">配送信息</h4>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="配送地址">{{ currentDetail.deliveryAddress }}</el-descriptions-item>
              <el-descriptions-item label="配送距离">{{ currentDetail.deliveryDistanceText }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- 备注 -->
          <div v-if="currentDetail.remark" class="detail-section">
            <h4 class="detail-heading">备注</h4>
            <div class="remark-box">{{ currentDetail.remark }}</div>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style lang="scss" scoped>
.order-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-card {
  padding: 16px 20px;
}

.table-card {
  padding: 20px;
}

.order-no {
  color: #ff5a00;
  font-weight: 500;
  cursor: pointer;
}

.text-muted {
  color: #999;
  font-size: 12px;
}

.text-xs {
  font-size: 11px;
}

.line-through {
  text-decoration: line-through;
}

.font-bold {
  font-weight: 600;
}

:deep(.el-table) {
  cursor: pointer;
}

/* 详情 */
.detail-section {
  margin-bottom: 24px;
}

.detail-heading {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin: 0 0 10px;
  padding-left: 10px;
  border-left: 3px solid #ff5a00;
}

.fee-list {
  background: #fafafa;
  border-radius: 8px;
  padding: 12px 16px;
}

.fee-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 13px;
  color: #666;

  &.fee-total {
    border-top: 1px solid #eee;
    margin-top: 6px;
    padding-top: 10px;
    font-size: 15px;
    font-weight: 600;
    color: #333;
  }
}

.remark-box {
  background: #fafafa;
  border-radius: 8px;
  padding: 12px 16px;
  font-size: 13px;
  color: #666;
  line-height: 1.6;
}
</style>
