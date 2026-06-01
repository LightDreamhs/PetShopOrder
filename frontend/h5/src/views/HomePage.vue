<template>
  <div class="home-page">
    <!-- 顶部导航 -->
    <van-sticky>
      <div class="home-header">
        <div class="header-left">
          <div class="header-title-row">
            <h1 class="header-title">🐾 贰掌柜宠物店</h1>
          </div>
          <span v-if="memberStore.isMember" class="member-badge" :class="memberBadgeClass">
            <van-icon name="crown-o" size="10" />
            {{ memberStore.memberLevelName }}
          </span>
        </div>
        <div class="header-actions">
          <div class="header-action-btn" @click="router.push('/orders')">
            <van-icon name="orders-o" size="20" />
            <span>订单</span>
          </div>
          <div class="header-action-btn" @click="handleLogout">
            <van-icon name="revoke" size="20" />
            <span>退出</span>
          </div>
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
        v-model="activeType"
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
import { useAuthStore } from '@/stores/auth'
import { useMemberStore } from '@/stores/member'
import { getProductsByType, getProductDetail } from '@/api/product'
import type { Product, ProductDetail } from '@/types'
import CategorySidebar from '@/components/home/CategorySidebar.vue'
import ProductCard from '@/components/home/ProductCard.vue'
import CartBar from '@/components/common/CartBar.vue'
import SkuSelectorPopup from '@/components/product/SkuSelectorPopup.vue'

const router = useRouter()
const cartStore = useCartStore()
const authStore = useAuthStore()
const memberStore = useMemberStore()

const memberBadgeClass = computed(() => {
  const name = memberStore.memberLevelName || ''
  if (name.includes('5000')) return 'badge-diamond'
  if (name.includes('2000')) return 'badge-gold'
  if (name.includes('1000')) return 'badge-silver'
  return 'badge-bronze'
})

const activeType = ref<'GOODS' | 'SERVICE'>('GOODS')
const allProducts = ref<Product[]>([])
const keyword = ref('')
const loading = ref(false)

const filteredProducts = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (kw) return allProducts.value.filter((p) => p.name.toLowerCase().includes(kw))
  return allProducts.value.filter((p) => p.type === activeType.value)
})

loading.value = true
getProductsByType().then((list) => {
  allProducts.value = list
}).finally(() => {
  loading.value = false
})

const specPickerVisible = ref(false)
const selectedProduct = ref<ProductDetail | null>(null)

async function handleLogout() {
  await authStore.logout()
  const { resetAuthCheck } = await import('@/router')
  resetAuthCheck()
  router.replace('/login')
}

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
  flex-direction: column;
  gap: 4px;
}

.header-title-row {
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
  gap: 4px;
  font-size: 10px;
  padding: 2px 10px;
  border-radius: 10px;
  font-weight: 600;
  letter-spacing: 0.3px;
  width: fit-content;
}

.badge-diamond {
  background: linear-gradient(135deg, #1a1a2e, #4a4a6a);
  color: #e8d5f5;
}

.badge-gold {
  background: linear-gradient(135deg, #b8860b, #daa520);
  color: #fff;
}

.badge-silver {
  background: linear-gradient(135deg, #6b7b8d, #a8b8c8);
  color: #fff;
}

.badge-bronze {
  background: linear-gradient(135deg, #8b5e3c, #c4854a);
  color: #fff;
}

.home-search {
  :deep(.van-search__content) {
    background: $bg;
  }
}

.header-actions {
  display: flex;
  gap: 12px;
}

.header-action-btn {
  display: flex;
  align-items: center;
  gap: 3px;
  cursor: pointer;
  color: $text-secondary;
  font-size: 13px;
  padding: 6px 10px;
  border-radius: 14px;
  background: #f5f5f5;

  &:active {
    background: #ebebeb;
  }
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
