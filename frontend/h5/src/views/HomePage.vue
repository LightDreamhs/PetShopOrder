<template>
  <div class="home-page">
    <!-- 顶部导航 -->
    <van-sticky>
      <div class="home-header">
        <div class="header-left">
          <h1 class="header-title">🐾 贰掌柜宠物店</h1>
          <span v-if="memberStore.isMember" class="member-badge">
            <van-icon name="crown-o" size="10" />
            {{ memberStore.memberLevelName }}
          </span>
        </div>
        <div class="header-right" @click="router.push('/orders')">
          <van-icon name="orders-o" size="20" />
          <span>订单</span>
        </div>
      </div>
      <van-search
        v-model="keyword"
        placeholder="搜索"
        shape="round"
        class="home-search"
      />
    </van-sticky>

    <!-- 主体：分类侧边栏 + 商品列表 -->
    <div class="home-body">
      <CategorySidebar
        v-model="activeTypeId"
        :categories="sidebarCategories"
      />
      <div class="product-list-wrap">
        <van-loading v-if="loading" class="loading-center" />
        <template v-else>
          <ProductCard
            v-for="p in filteredProducts"
            :key="p.id"
            :product="p"
            @select-spec="openSpecPicker"
            @change-qty="handleDirectQty"
          />
          <van-empty v-if="filteredProducts.length === 0" description="暂无商品" />
        </template>
      </div>
    </div>

    <!-- 购物车底栏 -->
    <CartBar />

    <!-- SKU 选择弹窗 -->
    <SkuSelectorPopup
      v-model:show="specPickerVisible"
      :product="selectedProduct"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import { useMemberStore } from '@/stores/member'
import { getProductsByType, getProductDetail } from '@/api/product'
import type { Product, ProductDetail } from '@/types'
import CategorySidebar from '@/components/home/CategorySidebar.vue'
import ProductCard from '@/components/home/ProductCard.vue'
import CartBar from '@/components/common/CartBar.vue'
import SkuSelectorPopup from '@/components/product/SkuSelectorPopup.vue'

const router = useRouter()
const cartStore = useCartStore()
const memberStore = useMemberStore()

const sidebarCategories = [
  { id: 1, name: '商品', type: 'GOODS' as const, icon: null, sort: 0 },
  { id: 2, name: '服务', type: 'SERVICE' as const, icon: null, sort: 0 },
]
const typeMap: Record<number, 'GOODS' | 'SERVICE'> = { 1: 'GOODS', 2: 'SERVICE' }

const activeTypeId = ref(1)
const allProducts = ref<Product[]>([])
const keyword = ref('')
const loading = ref(false)

const filteredProducts = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (kw) return allProducts.value.filter((p) => p.name.toLowerCase().includes(kw))
  return allProducts.value.filter((p) => p.type === typeMap[activeTypeId.value])
})

loading.value = true
getProductsByType().then((list) => {
  allProducts.value = list
}).finally(() => {
  loading.value = false
})

const specPickerVisible = ref(false)
const selectedProduct = ref<ProductDetail | null>(null)

async function openSpecPicker(product: Product) {
  try {
    selectedProduct.value = await getProductDetail(product.id)
    specPickerVisible.value = true
  } catch {
    // handled
  }
}

function handleDirectQty(product: Product, delta: number) {
  const currentQty = cartStore.getItemQty(product.id, null)
  const newQty = currentQty + delta

  if (newQty <= 0) {
    cartStore.removeItem(product.id, null)
  } else if (currentQty === 0 && delta > 0) {
    cartStore.addItem({
      productId: product.id,
      skuId: null,
      quantity: 1,
      productName: product.name,
      productCoverImg: product.coverImg,
      skuName: null,
      type: product.type,
      originalPrice: product.price,
      dealPrice: product.dealPrice,
    })
  } else {
    cartStore.updateQuantity(product.id, null, newQty)
  }
}
</script>

<style scoped lang="scss">
.home-page {
  min-height: 100vh;
  background: #fff;
}

.home-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-title {
  font-size: 19px;
  font-weight: 800;
  margin: 0;
  letter-spacing: -0.3px;
}

.member-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 10px;
  background: linear-gradient(135deg, #2c2c2c, #444);
  color: #ffd700;
  padding: 3px 8px;
  border-radius: 10px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.home-search {
  :deep(.van-search__content) {
    background: $bg;
  }
}

.header-right {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1px;
  cursor: pointer;
  color: $text-secondary;
  font-size: 10px;
  min-width: 32px;
  min-height: 32px;
  justify-content: center;
}

.home-body {
  display: flex;
  // header(~46px) + search(~52px)
  height: calc(100vh - 98px);
}

.product-list-wrap {
  flex: 1;
  padding: 8px 10px;
  // 购物车底栏出现时留出空间
  padding-bottom: 70px;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  background: $bg;
}

.loading-center {
  display: flex;
  justify-content: center;
  padding: 40px 0;
}
</style>
