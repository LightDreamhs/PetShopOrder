<template>
  <div class="home-page">
    <!-- 顶部导航 -->
    <van-sticky>
      <div class="home-header">
        <div class="header-left">
          <div class="header-title-row">
            <img class="header-logo" :src="shopLogo" alt="贰掌柜宠物店" />
            <h1 class="header-title">贰掌柜宠物店</h1>
          </div>
          <span v-if="memberStore.isMember" class="member-badge" :class="memberBadgeClass">
            <van-icon name="crown-o" size="10" />
            {{ memberStore.memberLevelName }}
          </span>
        </div>
        <div class="header-actions">
          <!-- 我的：下拉菜单 -->
          <div ref="mineWrapper" class="header-action-btn mine-wrapper">
            <div class="mine-trigger" @click="toggleMineMenu">
              <van-icon name="manager-o" size="20" />
              <span>我的</span>
              <van-icon
                name="arrow-down"
                size="12"
                class="mine-arrow"
                :class="{ 'is-open': showMineMenu }"
              />
            </div>
            <transition name="mine-fade">
              <div v-if="showMineMenu" class="mine-dropdown">
                <div class="mine-item" @click="goOrders">
                  <van-icon name="orders-o" size="18" />
                  <span>我的订单</span>
                </div>
                <div class="mine-item" @click="goAddresses">
                  <van-icon name="location-o" size="18" />
                  <span>我的地址</span>
                </div>
              </div>
            </transition>
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
            @go-appointment="goAppointment"
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
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
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
import shopLogo from '@/assets/shop-logo.jpg'

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
  if (kw) {
    // 搜索时仍排除附加服务（附加服务跟随主服务，不在首页独立展示）
    return allProducts.value.filter(
      (p) => p.name.toLowerCase().includes(kw) && p.serviceCategory !== 'ADDON_SERVICE',
    )
  }
  return allProducts.value.filter(
    (p) => p.type === activeType.value && p.serviceCategory !== 'ADDON_SERVICE',
  )
})

loading.value = true
getProductsByType().then((list) => {
  allProducts.value = list
}).finally(() => {
  loading.value = false
})

const specPickerVisible = ref(false)
const selectedProduct = ref<ProductDetail | null>(null)

// 「我的」下拉菜单
const showMineMenu = ref(false)
const mineWrapper = ref<HTMLElement | null>(null)

function toggleMineMenu() {
  showMineMenu.value = !showMineMenu.value
}

function closeMineMenu(e: MouseEvent) {
  if (mineWrapper.value && !mineWrapper.value.contains(e.target as Node)) {
    showMineMenu.value = false
  }
}

function goOrders() {
  showMineMenu.value = false
  router.push('/orders')
}

function goAddresses() {
  showMineMenu.value = false
  router.push('/address/manage')
}

onMounted(() => {
  document.addEventListener('click', closeMineMenu)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeMineMenu)
})

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

function goAppointment(product: Product) {
  router.push(`/appointment/${product.id}`)
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

.header-logo {
  width: 30px;
  height: 30px;
  object-fit: contain;
  border-radius: 8px;
  flex-shrink: 0;
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

.mine-wrapper {
  position: relative;
  padding: 0;
  background: transparent;
}

.mine-trigger {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 6px 10px;
  border-radius: 14px;
  background: #f5f5f5;
  cursor: pointer;

  &:active {
    background: #ebebeb;
  }
}

.mine-arrow {
  transition: transform 0.2s;

  &.is-open {
    transform: rotate(180deg);
  }
}

.mine-dropdown {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  min-width: 140px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  z-index: 100;
}

.mine-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 11px 14px;
  font-size: 14px;
  color: $text;
  cursor: pointer;
  transition: background 0.15s;

  &:not(:last-child) {
    border-bottom: 1px solid #f5f5f5;
  }

  &:active {
    background: #f7f8fa;
  }
}

// 下拉淡入动画
.mine-fade-enter-active,
.mine-fade-leave-active {
  transition: opacity 0.18s, transform 0.18s;
}

.mine-fade-enter-from,
.mine-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
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
