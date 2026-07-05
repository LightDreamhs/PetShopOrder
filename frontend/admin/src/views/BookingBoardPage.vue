<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBookings, markBookingServiced, cancelBooking } from '@/api/appointment'
import type { BookingBoardItem, BookingStatus } from '@/types'

const bookings = ref<BookingBoardItem[]>([])
const total = ref(0)
const loading = ref(false)

const today = new Date()
const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`

const filters = reactive({
  date: todayStr as string | null,
  status: undefined as BookingStatus | undefined,
  keyword: '',
  page: 1,
  size: 20,
})

async function fetchBookings() {
  loading.value = true
  try {
    const params: any = { page: filters.page, size: filters.size }
    if (filters.date) params.date = filters.date
    if (filters.status) params.status = filters.status
    if (filters.keyword) params.keyword = filters.keyword
    const res = await getBookings(params)
    bookings.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  filters.page = 1
  fetchBookings()
}

function handleReset() {
  filters.date = todayStr
  filters.status = undefined
  filters.keyword = ''
  filters.page = 1
  fetchBookings()
}

const statusOptions: { label: string; value: BookingStatus; type: 'warning' | 'success' | 'info' }[] = [
  { label: '待服务', value: 'PENDING', type: 'warning' },
  { label: '已完成', value: 'SERVICED', type: 'success' },
  { label: '已取消', value: 'CANCELLED', type: 'info' },
]

function statusLabel(s: BookingStatus): string {
  return statusOptions.find((o) => o.value === s)?.label || s
}
function statusType(s: BookingStatus): 'warning' | 'success' | 'info' {
  return statusOptions.find((o) => o.value === s)?.type || 'info'
}

// 只显示 时:分（去掉秒和日期）
function fmtTime(dt: string): string {
  if (!dt) return ''
  return dt.replace('T', ' ').slice(11, 16)
}
function fmtDateTime(dt: string): string {
  if (!dt) return ''
  return dt.replace('T', ' ').slice(5, 16)
}

async function handleMarkServiced(row: BookingBoardItem) {
  try {
    await ElMessageBox.confirm(`确认将「${row.mainProductName}」预约标记为已完成？`, '标记完成', {
      type: 'warning',
      confirmButtonText: '确认完成',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await markBookingServiced(row.id)
    ElMessage.success('已标记完成')
    fetchBookings()
  } catch {
    // handled
  }
}

async function handleCancel(row: BookingBoardItem) {
  try {
    await ElMessageBox.confirm(
      `确认取消该预约？取消后时段将释放给其他顾客。`,
      '取消预约',
      { type: 'warning', confirmButtonText: '确认取消', cancelButtonText: '再想想', confirmButtonClass: 'el-button--danger' },
    )
  } catch {
    return
  }
  try {
    await cancelBooking(row.id)
    ElMessage.success('预约已取消')
    fetchBookings()
  } catch {
    // handled
  }
}

onMounted(() => {
  fetchBookings()
})
</script>

<template>
  <div class="booking-page">
    <!-- 筛选 -->
    <div class="page-card filter-card">
      <div class="filter-bar">
        <el-date-picker
          v-model="filters.date"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择日期"
          :clearable="true"
          style="width: 160px"
        />
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 120px">
          <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-input v-model="filters.keyword" placeholder="订单号/电话/联系人" clearable style="width: 220px" @keyup.enter="handleSearch" />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <span class="filter-tip">默认查询今日预约；清空日期查全部</span>
      </div>
    </div>

    <!-- 列表 -->
    <div class="page-card table-card">
      <el-table :data="bookings" v-loading="loading" stripe style="width: 100%" empty-text="暂无预约">
        <el-table-column label="预约时间" width="200">
          <template #default="{ row }">
            <div class="appt-time">
              <div>{{ fmtDateTime(row.startTime) }} ~ {{ fmtTime(row.endTime) }}</div>
              <div class="appt-duration">{{ row.totalDuration }} 分钟</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="主服务" min-width="160">
          <template #default="{ row }">
            <div>{{ row.mainProductName }}</div>
            <div v-if="row.mainSkuName" class="text-muted">{{ row.mainSkuName }}</div>
          </template>
        </el-table-column>
        <el-table-column label="顾客" min-width="160">
          <template #default="{ row }">
            <div>{{ row.customerName || '-' }}</div>
            <div class="text-muted">{{ row.customerPhone }}</div>
          </template>
        </el-table-column>
        <el-table-column label="宠物信息" min-width="140">
          <template #default="{ row }">
            <span v-if="row.petInfo">{{ row.petInfo }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="100" align="right">
          <template #default="{ row }">
            ¥{{ row.totalAmount }}
          </template>
        </el-table-column>
        <el-table-column label="订单号" width="180">
          <template #default="{ row }">
            <span class="order-no">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" effect="dark">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              type="success"
              size="small"
              link
              @click="handleMarkServiced(row)"
            >
              标记完成
            </el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              type="danger"
              size="small"
              link
              @click="handleCancel(row)"
            >
              取消
            </el-button>
            <span v-if="row.status !== 'PENDING'" class="text-muted">-</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="filters.page"
        v-model:page-size="filters.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="fetchBookings"
        @size-change="() => { filters.page = 1; fetchBookings() }"
      />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.booking-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.appt-time {
  line-height: 1.5;
}

.appt-duration {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.order-no {
  font-family: monospace;
  font-size: 12px;
}
</style>
