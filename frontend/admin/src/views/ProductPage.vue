<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCategories } from '@/api/category'
import { getProducts, getProduct, createProduct, updateProduct, updateProductStatus, deleteProduct } from '@/api/product'
import { uploadFile } from '@/api/file'
import type { Category, ProductListItem, SkuDetail } from '@/types'

// ==================== 分类数据 ====================
const categories = ref<Category[]>([])

async function fetchCategories() {
  const res = await getCategories()
  const goods = res.data.find((c) => c.type === 'GOODS')
  const service = res.data.find((c) => c.type === 'SERVICE')
  categories.value = [
    goods ? { ...goods, name: '商品' } : null,
    service ? { ...service, name: '服务' } : null,
  ].filter((c): c is Category => c !== null)
}

// ==================== 商品列表 ====================
const products = ref<ProductListItem[]>([])
const productTotal = ref(0)
const productLoading = ref(false)

const productFilters = reactive({
  keyword: '',
  categoryId: undefined as number | undefined,
  type: undefined as string | undefined,
  status: undefined as string | undefined,
  page: 1,
  size: 20,
})

async function fetchProducts() {
  productLoading.value = true
  try {
    const params: any = { page: productFilters.page, size: productFilters.size }
    if (productFilters.keyword) params.keyword = productFilters.keyword
    if (productFilters.categoryId) params.categoryId = productFilters.categoryId
    if (productFilters.type) params.type = productFilters.type
    if (productFilters.status) params.status = productFilters.status
    const res = await getProducts(params)
    products.value = res.data.list
    productTotal.value = res.data.total
  } finally {
    productLoading.value = false
  }
}

function handleProductSearch() {
  productFilters.page = 1
  fetchProducts()
}

function handleProductReset() {
  productFilters.keyword = ''
  productFilters.categoryId = undefined
  productFilters.type = undefined
  productFilters.status = undefined
  productFilters.page = 1
  fetchProducts()
}

// ==================== 商品编辑 ====================
const productDialogVisible = ref(false)
const productFormMode = ref<'create' | 'edit'>('create')
const productFormLoading = ref(false)
const productForm = reactive({
  id: 0,
  categoryId: undefined as number | undefined,
  name: '',
  description: '',
  coverImg: '',
  type: 'GOODS' as 'GOODS' | 'SERVICE',
  supportDelivery: false,
  sort: 0,
  skus: [] as SkuDetail[],
})

const selectedCategoryType = computed(() => {
  const cat = categories.value.find((c) => c.id === productForm.categoryId)
  return cat?.type || 'GOODS'
})

watch(() => productForm.categoryId, () => {
  productForm.type = selectedCategoryType.value
  if (productForm.type === 'SERVICE') {
    productForm.supportDelivery = false
  }
})

const uploadLoading = ref(false)

async function handleUpload(options: any) {
  uploadLoading.value = true
  try {
    const res = await uploadFile(options.file)
    productForm.coverImg = res.data.url
    ElMessage.success('上传成功')
  } catch {
    // handled
  } finally {
    uploadLoading.value = false
  }
}

function addSkuRow() {
  productForm.skus.push({
    specName: '',
    price: '0.00',
    memberPrice: null,
    sort: productForm.skus.length + 1,
  })
}

function removeSkuRow(index: number) {
  productForm.skus.splice(index, 1)
}

function openProductCreate() {
  productFormMode.value = 'create'
  productForm.id = 0
  productForm.categoryId = undefined
  productForm.name = ''
  productForm.description = ''
  productForm.coverImg = ''
  productForm.type = 'GOODS'
  productForm.supportDelivery = false
  productForm.sort = 0
  productForm.skus = []
  addSkuRow()
  productDialogVisible.value = true
}

async function openProductEdit(row: ProductListItem) {
  productFormMode.value = 'edit'
  try {
    const res = await getProduct(row.id)
    const p = res.data
    productForm.id = p.id
    productForm.categoryId = p.categoryId
    productForm.name = p.name
    productForm.description = p.description || ''
    productForm.coverImg = p.coverImg || ''
    productForm.type = p.type
    productForm.supportDelivery = p.supportDelivery
    productForm.sort = p.sort
    productForm.skus = p.skus.map((s) => ({ ...s }))
    productDialogVisible.value = true
  } catch {
    // handled
  }
}

async function handleProductSubmit() {
  if (!productForm.name) { ElMessage.warning('请输入商品名称'); return }
  if (!productForm.categoryId) { ElMessage.warning('请选择分类'); return }
  if (productForm.skus.length === 0) { ElMessage.warning('请至少添加一个规格'); return }
  for (const sku of productForm.skus) {
    if (!sku.specName) { ElMessage.warning('请填写规格名称'); return }
    if (!sku.price || parseFloat(sku.price) <= 0) { ElMessage.warning('请填写有效的原价'); return }
  }
  productFormLoading.value = true
  try {
    const data: any = {
      categoryId: productForm.categoryId,
      name: productForm.name,
      description: productForm.description || undefined,
      coverImg: productForm.coverImg || undefined,
      type: productForm.type,
      supportDelivery: productForm.supportDelivery,
      sort: productForm.sort,
      skus: productForm.skus.map((s) => ({
        specName: s.specName,
        price: s.price,
        memberPrice: productForm.type === 'GOODS' ? s.memberPrice : null,
        sort: s.sort,
      })),
    }
    if (productFormMode.value === 'create') {
      await createProduct(data)
      ElMessage.success('商品创建成功')
    } else {
      await updateProduct(productForm.id, data)
      ElMessage.success('商品更新成功')
    }
    productDialogVisible.value = false
    fetchProducts()
    fetchCategories()
  } catch {
    // handled
  } finally {
    productFormLoading.value = false
  }
}

async function handleToggleStatus(row: ProductListItem) {
  const newStatus = row.status === 'ON_SALE' ? 'OFF_SALE' : 'ON_SALE'
  const label = newStatus === 'ON_SALE' ? '上架' : '下架'
  try {
    await ElMessageBox.confirm(`确认将「${row.name}」${label}？`, '确认操作', { type: 'warning' })
    await updateProductStatus(row.id, newStatus)
    ElMessage.success(`${label}成功`)
    fetchProducts()
  } catch {
    // cancelled
  }
}

async function handleProductDelete(row: ProductListItem) {
  try {
    await ElMessageBox.confirm(`确认删除商品「${row.name}」？建议使用下架代替删除。`, '确认删除', { type: 'warning' })
    await deleteProduct(row.id)
    ElMessage.success('删除成功')
    fetchProducts()
    fetchCategories()
  } catch {
    // cancelled
  }
}

onMounted(() => {
  fetchCategories()
  fetchProducts()
})
</script>

<template>
  <div class="product-page">
    <div class="page-card filter-card">
      <div class="filter-bar">
        <el-input
          v-model="productFilters.keyword"
          placeholder="搜索商品名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleProductSearch"
          @clear="handleProductSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>

        <el-select v-model="productFilters.categoryId" placeholder="全部分类" clearable style="width: 140px" @change="handleProductSearch">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>

        <el-select v-model="productFilters.type" placeholder="全部类型" clearable style="width: 120px" @change="handleProductSearch">
          <el-option label="商品" value="GOODS" />
          <el-option label="服务" value="SERVICE" />
        </el-select>

        <el-select v-model="productFilters.status" placeholder="全部状态" clearable style="width: 120px" @change="handleProductSearch">
          <el-option label="上架中" value="ON_SALE" />
          <el-option label="已下架" value="OFF_SALE" />
        </el-select>

        <el-button type="primary" @click="handleProductSearch">
          <el-icon><Search /></el-icon>查询
        </el-button>
        <el-button @click="handleProductReset">重置</el-button>

        <div style="flex: 1" />
        <el-button type="primary" @click="openProductCreate">
          <el-icon><Plus /></el-icon>新增商品
        </el-button>
      </div>
    </div>

    <!-- 商品表格 -->
    <div class="page-card table-card">
      <el-table :data="products" v-loading="productLoading" stripe style="width: 100%">
        <el-table-column label="封面" width="70" align="center">
          <template #default="{ row }">
            <el-avatar v-if="row.coverImg" :src="row.coverImg" :size="40" shape="square" />
            <el-avatar v-else :size="40" shape="square" style="background: #f5f5f5; color: #ccc">
              <el-icon :size="20"><Picture /></el-icon>
            </el-avatar>
          </template>
        </el-table-column>

        <el-table-column prop="name" label="商品名称" min-width="160" />

        <el-table-column prop="categoryName" label="分类" width="100" />

        <el-table-column prop="type" label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.type === 'GOODS' ? 'primary' : 'success'" size="small" effect="plain">
              {{ row.type === 'GOODS' ? '商品' : '服务' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ON_SALE' ? 'success' : 'info'" size="small" effect="dark">
              {{ row.status === 'ON_SALE' ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="最低价" width="100" align="right">
          <template #default="{ row }">
            <span class="text-orange">¥{{ row.minPrice }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="skuCount" label="SKU" width="60" align="center" />

        <el-table-column prop="sort" label="排序" width="60" align="center" />

        <el-table-column prop="createTime" label="创建时间" width="140" />

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openProductEdit(row)">编辑</el-button>
            <el-button
              link
              :type="row.status === 'ON_SALE' ? 'warning' : 'success'"
              size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 'ON_SALE' ? '下架' : '上架' }}
            </el-button>
            <el-button link type="danger" size="small" @click="handleProductDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="productFilters.page"
        v-model:page-size="productFilters.size"
        :total="productTotal"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="() => fetchProducts()"
        @size-change="() => { productFilters.page = 1; fetchProducts() }"
      />
    </div>
    <el-dialog
      v-model="productDialogVisible"
      :title="productFormMode === 'create' ? '新增商品' : '编辑商品'"
      width="720px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form label-width="90px" @submit.prevent="handleProductSubmit">
        <el-form-item label="商品名称" required>
          <el-input v-model="productForm.name" placeholder="请输入商品名称" />
        </el-form-item>

        <el-form-item label="所属分类" required>
          <el-select v-model="productForm.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option-group label="商品分类">
              <el-option v-for="c in categories.filter(c => c.type === 'GOODS')" :key="c.id" :label="c.name" :value="c.id" />
            </el-option-group>
            <el-option-group label="服务分类">
              <el-option v-for="c in categories.filter(c => c.type === 'SERVICE')" :key="c.id" :label="c.name" :value="c.id" />
            </el-option-group>
          </el-select>
        </el-form-item>

        <el-form-item label="商品描述">
          <el-input v-model="productForm.description" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>

        <el-form-item label="封面图">
          <el-upload
            class="cover-uploader"
            :show-file-list="false"
            :http-request="handleUpload"
            accept="image/*"
          >
            <el-avatar v-if="productForm.coverImg" :src="productForm.coverImg" :size="80" shape="square" />
            <div v-else class="upload-placeholder">
              <el-icon :size="24" class="upload-icon"><Plus /></el-icon>
            </div>
          </el-upload>
        </el-form-item>

        <el-form-item v-if="productForm.type === 'GOODS'" label="支持配送">
          <el-switch v-model="productForm.supportDelivery" />
        </el-form-item>

        <el-form-item label="排序">
          <el-input-number v-model="productForm.sort" :min="0" :max="999" />
        </el-form-item>

        <!-- SKU 管理 -->
        <el-form-item label="规格管理" required>
          <div class="sku-section">
            <div class="sku-header">
              <el-button size="small" @click="addSkuRow">
                <el-icon><Plus /></el-icon>添加规格
              </el-button>
              <span class="sku-count">共 {{ productForm.skus.length }} 个规格</span>
            </div>
            <div class="sku-guide">
              <span>规格名：如「500g」「单次洗澡」</span>
              <span>原价/会员价：填写元，支持小数（如 99.00）</span>
              <span>排序：数字越小越靠前</span>
            </div>
            <div class="sku-columns">
              <span style="width: 160px">规格名</span>
              <span style="width: 100px">原价(元)</span>
              <span v-if="productForm.type === 'GOODS'" style="width: 100px">会员价(元)</span>
              <span style="width: 100px">排序</span>
              <span style="width: 24px"></span>
            </div>

            <div class="sku-list">
              <div v-for="(sku, index) in productForm.skus" :key="index" class="sku-row">
                <el-input v-model="sku.specName" placeholder="如：500g / 单次" style="width: 160px" />
                <el-input v-model="sku.price" placeholder="如：99.00" style="width: 100px" />
                <el-input
                  v-if="productForm.type === 'GOODS'"
                  v-model="sku.memberPrice"
                  placeholder="如：89.00"
                  style="width: 100px"
                />
                <el-input-number v-model="sku.sort" :min="0" :max="999" style="width: 100px" controls-position="right" />
                <el-button link type="danger" @click="removeSkuRow(index)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="productDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="productFormLoading" @click="handleProductSubmit">
          {{ productFormMode === 'create' ? '创建' : '保存' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.product-page {
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

.text-orange {
  color: #ff5a00;
  font-weight: 500;
}

/* SKU 管理 */
.sku-section {
  width: 100%;
}

.sku-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.sku-count {
  font-size: 12px;
  color: #999;
}

.sku-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sku-guide {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  margin-bottom: 8px;
  font-size: 12px;
  color: #999;
}

.sku-columns {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  padding: 0 12px;
  font-size: 12px;
  color: #666;
}

.sku-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #fafafa;
  border-radius: 8px;
}

/* 上传 */
.cover-uploader {
  :deep(.el-upload) {
    border: 1px dashed #ddd;
    border-radius: 8px;
    cursor: pointer;
    overflow: hidden;
    transition: border-color 0.2s;

    &:hover {
      border-color: #ff5a00;
    }
  }
}

.upload-placeholder {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
}

.upload-icon {
  color: #ccc;
}
</style>
