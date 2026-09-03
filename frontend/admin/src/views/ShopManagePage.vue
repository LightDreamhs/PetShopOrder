<template>
  <div class="shop-manage-page">
    <div class="page-toolbar">
      <el-button type="primary" :icon="Plus" @click="openCreate">新增门店</el-button>
    </div>

    <el-table :data="shopStore.shops" v-loading="loading" border stripe>
      <el-table-column prop="sort" label="排序" width="70" />
      <el-table-column prop="name" label="门店名称" min-width="140">
        <template #default="{ row }">
          <span class="shop-name">{{ row.name }}</span>
          <el-tag v-if="row.id === shopStore.currentShop?.id" size="small" class="current-tag">当前</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="code" label="编码" width="110" />
      <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
      <el-table-column prop="phone" label="电话" width="130" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 'OPEN'"
            inline-prompt
            active-text="营业"
            inactive-text="歇业"
            @change="(val: string | number | boolean) => toggleStatus(row, Boolean(val))"
          />
        </template>
      </el-table-column>
      <el-table-column label="进店二维码" width="220">
        <template #default="{ row }">
          <el-button size="small" @click="downloadQr(row)">下载二维码</el-button>
          <el-button size="small" text type="primary" @click="copyLink(row)">复制链接</el-button>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑门店' : '新增门店'"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" label-width="90px">
        <el-form-item label="门店名称" required>
          <el-input v-model="form.name" placeholder="如：二江寺站" maxlength="64" />
        </el-form-item>
        <el-form-item label="店铺编码" required>
          <el-input
            v-model="form.code"
            placeholder="2~32 位小写字母/数字/连字符，如 erjiangsi"
            maxlength="32"
            :disabled="!!editingId"
          />
          <div class="form-tip">用于进店链接 ?s={code}，创建后不可修改</div>
        </el-form-item>
        <el-form-item label="门店地址">
          <el-input v-model="form.address" placeholder="展示用" maxlength="255" />
        </el-form-item>
        <el-form-item label="门店电话">
          <el-input v-model="form.phone" maxlength="20" />
        </el-form-item>
        <el-form-item label="纬度" required>
          <el-input-number v-model="form.shopLat" :precision="7" :step="0.001" :controls="false" class="coord-input" />
        </el-form-item>
        <el-form-item label="经度" required>
          <el-input-number v-model="form.shopLng" :precision="7" :step="0.001" :controls="false" class="coord-input" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
          <div class="form-tip">数字越小越靠前（首位为默认店，顾客无进店参数时落默认店）</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useShopStore } from '@/stores/shop'
import {
  createShop,
  updateShop,
  updateShopStatus,
  fetchShopQrCodeBlob,
} from '@/api/shop'
import type { ShopInfo } from '@/types'

const shopStore = useShopStore()
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  code: '',
  name: '',
  phone: '',
  address: '',
  shopLat: 31.2304,
  shopLng: 121.4737,
  sort: 0,
})

onMounted(async () => {
  loading.value = true
  try {
    await shopStore.fetchShops()
  } finally {
    loading.value = false
  }
})

function openCreate() {
  editingId.value = null
  Object.assign(form, { code: '', name: '', phone: '', address: '', shopLat: 31.2304, shopLng: 121.4737, sort: 0 })
  dialogVisible.value = true
}

function openEdit(row: ShopInfo) {
  editingId.value = row.id
  Object.assign(form, {
    code: row.code,
    name: row.name,
    phone: row.phone || '',
    address: row.address || '',
    shopLat: row.shopLat,
    shopLng: row.shopLng,
    sort: row.sort,
  })
  dialogVisible.value = true
}

async function save() {
  if (!form.name.trim()) return ElMessage.warning('请填写门店名称')
  if (!/^[a-z0-9-]{2,32}$/.test(form.code)) return ElMessage.warning('编码须为 2~32 位小写字母/数字/连字符')
  saving.value = true
  try {
    if (editingId.value) {
      await updateShop(editingId.value, { ...form })
      ElMessage.success('已保存')
    } else {
      await createShop({ ...form })
      ElMessage.success('门店已创建，可在系统配置页为其设置配送规则')
    }
    dialogVisible.value = false
    await shopStore.fetchShops()
  } catch {
    // 错误提示由拦截器统一处理
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: ShopInfo, val: boolean) {
  const status = val ? 'OPEN' : 'CLOSED'
  if (!val) {
    await ElMessageBox.confirm(
      `歇业后「${row.name}」将不能下单与预约（顾客仍可浏览），确认歇业？`,
      '确认歇业',
      { type: 'warning' },
    )
  }
  try {
    await updateShopStatus(row.id, status)
    ElMessage.success(val ? '已恢复营业' : '已歇业')
    await shopStore.fetchShops()
  } catch {
    // ignore
  }
}

async function downloadQr(row: ShopInfo) {
  try {
    const blob = await fetchShopQrCodeBlob(row.id)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${row.name}-进店二维码.png`
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('二维码下载失败')
  }
}

async function copyLink(row: ShopInfo) {
  const url = row.entryUrl || `${window.location.origin}/?s=${row.code}`
  try {
    await navigator.clipboard.writeText(url)
    ElMessage.success('进店链接已复制')
  } catch {
    ElMessage.warning(`复制失败，请手动复制：${url}`)
  }
}
</script>

<style scoped lang="scss">
.shop-manage-page {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
}

.page-toolbar {
  margin-bottom: 16px;
}

.shop-name {
  font-weight: 600;
  margin-right: 8px;
}

.current-tag {
  margin-left: 4px;
}

.form-tip {
  font-size: 12px;
  color: #999;
  line-height: 1.5;
  margin-top: 4px;
}

.coord-input {
  width: 220px;
}
</style>
