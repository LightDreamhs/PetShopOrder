<template>
  <el-dialog v-model="visible" title="选择店铺位置" width="700px" @opened="onOpen">
    <div class="map-dialog-body">
      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索地点"
          clearable
          :prefix-icon="Search"
        />
      </div>

      <!-- 地图区域 -->
      <div class="map-container">
        <div ref="mapRef" class="map-el" />
        <div class="map-pin">
          <svg width="28" height="40" viewBox="0 0 28 40" fill="none">
            <path d="M14 0C6.268 0 0 6.268 0 14c0 10.5 14 26 14 26s14-15.5 14-26C28 6.268 21.732 0 14 0z" fill="#409eff"/>
            <circle cx="14" cy="14" r="6" fill="#fff"/>
          </svg>
        </div>
      </div>

      <!-- POI 列表 / 搜索结果 -->
      <div class="poi-list">
        <div v-if="poiList.length === 0" class="poi-empty">
          <span>拖动地图或搜索以选择位置</span>
        </div>
        <div
          v-for="(item, idx) in poiList"
          :key="idx"
          class="poi-item"
          :class="{ active: selectedIdx === idx }"
          @click="selectPoi(idx)"
        >
          <div class="poi-info">
            <div class="poi-title">{{ item.title }}</div>
            <div class="poi-addr">{{ item.address }}</div>
          </div>
          <div class="poi-radio">
            <el-icon v-if="selectedIdx === idx" class="radio-checked"><CircleCheckFilled /></el-icon>
            <span v-else class="radio-empty" />
          </div>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="confirmLocation">确认选点</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, CircleCheckFilled } from '@element-plus/icons-vue'
import { loadTMapSDK } from '@/utils/map'

interface PoiItem {
  title: string
  address: string
  lat: number
  lng: number
}

const props = defineProps<{
  lat: string | null
  lng: string | null
}>()

const emit = defineEmits<{
  confirm: [data: { lat: string; lng: string; address: string }]
}>()

const visible = defineModel<boolean>({ default: false })

const mapRef = ref<HTMLElement>()
const keyword = ref('')
const currentAddress = ref('')
const currentLat = ref(39.908823)
const currentLng = ref(116.397470)
const poiList = ref<PoiItem[]>([])
const selectedIdx = ref(0)

let map: any = null
let geocoder: any = null
let searchService: any = null
let debounceTimer: ReturnType<typeof setTimeout> | null = null
let searchTimer: ReturnType<typeof setTimeout> | null = null
let isSearching = false

async function onOpen() {
  try {
    await loadTMapSDK()
    await nextTick()
    initMap()
  } catch (e: any) {
    ElMessage.error(`${e?.message || '地图加载失败'}`)
    visible.value = false
  }
}

function initMap() {
  if (!mapRef.value) return
  const TMap = (window as any).TMap
  if (!TMap?.Map) {
    ElMessage.error('地图组件未就绪')
    visible.value = false
    return
  }

  const lat = props.lat ? parseFloat(props.lat) : 39.908823
  const lng = props.lng ? parseFloat(props.lng) : 116.397470
  currentLat.value = lat
  currentLng.value = lng

  if (map) {
    map.setCenter(new TMap.LatLng(lat, lng))
    fetchNearbyPois(lat, lng)
    return
  }

  map = new TMap.Map(mapRef.value, {
    center: new TMap.LatLng(lat, lng),
    zoom: 15,
  })

  geocoder = new TMap.service.Geocoder()
  searchService = new TMap.service.Search({
    pageSize: 20,
  })

  map.on('moveend', () => {
    if (isSearching) return
    const center = map.getCenter()
    currentLat.value = center.lat
    currentLng.value = center.lng
    fetchNearbyPois(center.lat, center.lng)
  })

  fetchNearbyPois(lat, lng)
}

function fetchNearbyPois(lat: number, lng: number) {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(async () => {
    try {
      const TMap = (window as any).TMap
      const result = await geocoder.getAddress({
        location: new TMap.LatLng(lat, lng),
        getPoi: true,
      })
      if (result.status === 0 && result.result) {
        currentAddress.value = result.result.address
        // 将逆地址解析结果作为第一项（代表地图中心）
        const pois: PoiItem[] = [{
          title: result.result.address || '当前位置',
          address: result.result.formatted_addresses?.recommend || result.result.address || '',
          lat,
          lng,
        }]
        // 追加附近 POI
        if (result.result.pois && result.result.pois.length > 0) {
          for (const poi of result.result.pois) {
            pois.push({
              title: poi.title,
              address: poi.address,
              lat: poi.location.lat,
              lng: poi.location.lng,
            })
          }
        }
        poiList.value = pois
        selectedIdx.value = 0
      }
    } catch {
      currentAddress.value = `${lat.toFixed(6)}, ${lng.toFixed(6)}`
      poiList.value = []
    }
  }, 300)
}

// 监听搜索关键词
watch(keyword, (val) => {
  if (searchTimer) clearTimeout(searchTimer)
  if (!val || !val.trim()) {
    clearSearch()
    return
  }

  isSearching = true
  searchTimer = setTimeout(async () => {
    if (!searchService) {
      console.error('[MapPicker] searchService 未初始化')
      return
    }
    try {
      const result = await searchService.searchRegion({
        keyword: val.trim(),
        cityName: '全国',
        pageIndex: 1,
      })
      console.log('[MapPicker] search result:', result)
      if (result.data && result.data.length > 0) {
        poiList.value = result.data.map((item: any) => ({
          title: item.title,
          address: item.address,
          lat: item.location.lat,
          lng: item.location.lng,
        }))
        selectedIdx.value = 0
        const first = poiList.value[0]
        const TMap = (window as any).TMap
        map.setCenter(new TMap.LatLng(first.lat, first.lng))
      } else {
        poiList.value = []
      }
    } catch (e) {
      console.error('[MapPicker] search error:', e)
      poiList.value = []
    }
  }, 400)
})

function clearSearch() {
  keyword.value = ''
  isSearching = false
  fetchNearbyPois(currentLat.value, currentLng.value)
}

function selectPoi(idx: number) {
  selectedIdx.value = idx
  const poi = poiList.value[idx]
  currentAddress.value = poi.title
  currentLat.value = poi.lat
  currentLng.value = poi.lng
  const TMap = (window as any).TMap
  map.setCenter(new TMap.LatLng(poi.lat, poi.lng))
}

function confirmLocation() {
  const poi = poiList.value[selectedIdx.value]
  if (!poi) {
    ElMessage.warning('请先选择位置')
    return
  }
  emit('confirm', {
    lat: poi.lat.toString(),
    lng: poi.lng.toString(),
    address: poi.title,
  })
  visible.value = false
}
</script>

<style scoped lang="scss">
.map-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 0;
  max-height: 70vh;
}

.search-bar {
  flex-shrink: 0;
  padding-bottom: 8px;
}

.map-container {
  height: 280px;
  position: relative;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
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

.poi-list {
  flex: 1;
  overflow-y: auto;
  margin-top: 8px;
  min-height: 120px;
  max-height: 260px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.poi-empty {
  padding: 24px;
  text-align: center;
  color: #909399;
  font-size: 13px;
}

.poi-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;
  gap: 10px;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: #f5f7fa;
  }

  &.active {
    background: #ecf5ff;
  }
}

.poi-info {
  flex: 1;
  min-width: 0;
}

.poi-title {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.poi-addr {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.poi-radio {
  flex-shrink: 0;
}

.radio-checked {
  color: #409eff;
  font-size: 20px;
}

.radio-empty {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid #c0c4cc;
  border-radius: 50%;
}
</style>
