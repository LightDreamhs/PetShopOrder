<template>
  <van-cell
    title="配送地址"
    is-link
    :value="displayAddress || '选择配送地址'"
    @click="openPicker"
  />

  <van-popup
    v-model:show="showPicker"
    position="right"
    :style="{ width: '100%', height: '100%' }"
    :overlay="false"
  >
    <div class="map-picker">
      <!-- 顶部搜索栏 -->
      <div class="map-header">
        <van-icon name="arrow-left" size="20" @click="showPicker = false" />
        <van-search
          v-model="keyword"
          placeholder="搜索地点"
          shape="round"
          class="map-search"
          @update:model-value="onSearchInput"
          @cancel="clearSearch"
        />
      </div>

      <!-- 搜索结果列表 -->
      <div v-if="searchResults.length > 0" class="search-results">
        <van-cell
          v-for="(item, idx) in searchResults"
          :key="idx"
          :title="item.title"
          :label="item.address"
          clickable
          @click="selectSearchResult(item)"
        />
      </div>
      <div v-else-if="searchNoResult" class="search-empty">
        <span>未找到相关地点，请尝试其他关键词</span>
      </div>

      <!-- 地图区域 -->
      <div class="map-container">
        <div ref="mapRef" class="map-el" />
        <!-- 中心 pin -->
        <div class="map-pin">
          <svg width="28" height="40" viewBox="0 0 28 40" fill="none">
            <path d="M14 0C6.268 0 0 6.268 0 14c0 10.5 14 26 14 26s14-15.5 14-26C28 6.268 21.732 0 14 0z" fill="#ee0a24"/>
            <circle cx="14" cy="14" r="6" fill="#fff"/>
          </svg>
        </div>
        <!-- 定位按钮 -->
        <div class="locate-btn" @click="locateUser">
          <van-icon name="aim" size="22" />
        </div>
      </div>

      <!-- 底部确认栏 -->
      <div class="map-footer safe-area-bottom">
        <div class="footer-address">
          <van-icon name="location-o" />
          <span class="footer-text">{{ currentAddress || '正在获取地址...' }}</span>
        </div>
        <van-button type="primary" round size="small" class="confirm-btn" @click="confirmAddress">
          确认选点
        </van-button>
      </div>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import { showToast } from 'vant'
import { loadTMapSDK } from '@/utils/map'

const emit = defineEmits<{
  confirm: [data: { address: string; lat: string; lng: string }]
}>()

const showPicker = ref(false)
const mapRef = ref<HTMLElement>()
const keyword = ref('')
const currentAddress = ref('')
const currentLat = ref(0)
const currentLng = ref(0)
const searchResults = ref<{ title: string; address: string; lat: number; lng: number }[]>([])
const searchNoResult = ref(false)

const savedAddress = ref('')
const displayAddress = computed(() => savedAddress.value)

let map: any = null
let geocoder: any = null
let searchService: any = null
let suggestionService: any = null
let debounceTimer: ReturnType<typeof setTimeout> | null = null
let searchTimer: ReturnType<typeof setTimeout> | null = null

async function openPicker() {
  showPicker.value = true
  try {
    await loadTMapSDK()
    await nextTick()
    initMap()
  } catch {
    showToast('地图加载失败，请检查网络')
    showPicker.value = false
  }
}

function initMap() {
  if (!mapRef.value) return
  if (map) {
    map.setCenter(new (window as any).TMap.LatLng(currentLat.value || 39.908823, currentLng.value || 116.397470))
    return
  }

  const TMap = (window as any).TMap
  const center = new TMap.LatLng(currentLat.value || 39.908823, currentLng.value || 116.397470)

  map = new TMap.Map(mapRef.value, {
    center,
    zoom: 15,
  })

  geocoder = new TMap.service.Geocoder()
  searchService = new TMap.service.Search()
  suggestionService = new TMap.service.Suggestion()

  map.on('map_move_end', () => {
    const center = map.getCenter()
    currentLat.value = center.lat
    currentLng.value = center.lng
    reverseGeocode(center.lat, center.lng)
  })

  // 首次逆地址解析
  reverseGeocode(center.lat, center.lng)
}

function reverseGeocode(lat: number, lng: number) {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(async () => {
    try {
      const TMap = (window as any).TMap
      const result = await geocoder.getAddress({ location: new TMap.LatLng(lat, lng) })
      if (result.status === 0 && result.result) {
        currentAddress.value = result.result.address
      }
    } catch {
      currentAddress.value = `${lat.toFixed(6)}, ${lng.toFixed(6)}`
    }
  }, 300)
}

function onSearchInput(val: string) {
  if (searchTimer) clearTimeout(searchTimer)
  searchResults.value = []
  searchNoResult.value = false

  if (!val.trim()) return

  searchTimer = setTimeout(async () => {
    try {
      const result = await suggestionService.search({
        keyword: val.trim(),
        page_size: 10,
      })
      if (result.status === 0 && result.data && result.data.length > 0) {
        searchResults.value = result.data.map((item: any) => ({
          title: item.title,
          address: item.address,
          lat: item.location.lat,
          lng: item.location.lng,
        }))
      } else {
        searchNoResult.value = true
      }
    } catch {
      searchNoResult.value = true
    }
  }, 400)
}

function selectSearchResult(item: { title: string; address: string; lat: number; lng: number }) {
  const TMap = (window as any).TMap
  map.setCenter(new TMap.LatLng(item.lat, item.lng))
  currentAddress.value = item.title
  currentLat.value = item.lat
  currentLng.value = item.lng
  searchResults.value = []
  searchNoResult.value = false
  keyword.value = ''
}

function clearSearch() {
  keyword.value = ''
  searchResults.value = []
  searchNoResult.value = false
}

async function locateUser() {
  try {
    const geolocation = new (window as any).TMap.service.Geolocation({
      key: import.meta.env.VITE_TMAP_KEY,
    })
    const result = await geolocation.getLocation()
    if (result.status === 0 && result.result) {
      const { lat, lng } = result.result.location
      const TMap = (window as any).TMap
      map.setCenter(new TMap.LatLng(lat, lng))
    } else {
      showToast('定位失败，请手动选择地址')
    }
  } catch {
    showToast('定位失败，请手动选择地址')
  }
}

function confirmAddress() {
  if (!currentAddress.value) {
    showToast('请先选择地址')
    return
  }
  savedAddress.value = currentAddress.value
  emit('confirm', {
    address: currentAddress.value,
    lat: currentLat.value.toString(),
    lng: currentLng.value.toString(),
  })
  showPicker.value = false
}
</script>

<style scoped lang="scss">
.map-picker {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
}

.map-header {
  display: flex;
  align-items: center;
  padding: 8px 12px 0;
  gap: 4px;
  flex-shrink: 0;
  padding-top: calc(env(safe-area-inset-top) + 8px);

  .van-icon-arrow-left {
    flex-shrink: 0;
    padding: 8px;
    cursor: pointer;
  }
}

.map-search {
  flex: 1;
  padding: 0;
}

.search-results {
  position: absolute;
  top: 56px;
  left: 0;
  right: 0;
  max-height: 40vh;
  overflow-y: auto;
  background: #fff;
  z-index: 10;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding-top: env(safe-area-inset-top);
}

.search-empty {
  position: absolute;
  top: 56px;
  left: 0;
  right: 0;
  padding: 24px;
  text-align: center;
  color: $text-muted;
  font-size: 14px;
  background: #fff;
  z-index: 10;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding-top: calc(env(safe-area-inset-top) + 24px);
}

.map-container {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.map-el {
  width: 100%;
  height: 100%;
}

.map-pin {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -100%);
  z-index: 5;
  pointer-events: none;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.3));
}

.locate-btn {
  position: absolute;
  right: 12px;
  bottom: 12px;
  width: 40px;
  height: 40px;
  background: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  z-index: 5;
}

.map-footer {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}

.footer-address {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  flex: 1;
  min-width: 0;
  padding-right: 12px;
}

.footer-text {
  font-size: 13px;
  color: $text;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.confirm-btn {
  flex-shrink: 0;
  min-width: 90px;
}
</style>
