<template>
  <el-dialog v-model="visible" title="选择店铺位置" width="700px" @open="onOpen">
    <div class="map-dialog-body">
      <div class="map-search-row">
        <el-input
          v-model="keyword"
          placeholder="搜索地点"
          clearable
          @input="onSearchInput"
        />
      </div>
      <div v-if="searchResults.length > 0" class="search-results">
        <div
          v-for="(item, idx) in searchResults"
          :key="idx"
          class="search-item"
          @click="selectSearchResult(item)"
        >
          <div class="search-item-title">{{ item.title }}</div>
          <div class="search-item-addr">{{ item.address }}</div>
        </div>
      </div>
      <div class="map-container">
        <div ref="mapRef" class="map-el" />
        <div class="map-pin">
          <svg width="28" height="40" viewBox="0 0 28 40" fill="none">
            <path d="M14 0C6.268 0 0 6.268 0 14c0 10.5 14 26 14 26s14-15.5 14-26C28 6.268 21.732 0 14 0z" fill="#409eff"/>
            <circle cx="14" cy="14" r="6" fill="#fff"/>
          </svg>
        </div>
      </div>
      <div class="map-footer">
        <el-icon><Location /></el-icon>
        <span class="footer-text">{{ currentAddress || '拖动地图选择位置' }}</span>
      </div>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="confirmLocation">确认</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Location } from '@element-plus/icons-vue'
import { loadTMapSDK } from '@/utils/map'

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
const searchResults = ref<{ title: string; address: string; lat: number; lng: number }[]>([])

let map: any = null
let geocoder: any = null
let suggestionService: any = null
let debounceTimer: ReturnType<typeof setTimeout> | null = null
let searchTimer: ReturnType<typeof setTimeout> | null = null

async function onOpen() {
  try {
    await loadTMapSDK()
    await nextTick()
    initMap()
  } catch {
    ElMessage.error('地图加载失败')
    visible.value = false
  }
}

function initMap() {
  if (!mapRef.value) return

  const TMap = (window as any).TMap
  const lat = props.lat ? parseFloat(props.lat) : 39.908823
  const lng = props.lng ? parseFloat(props.lng) : 116.397470
  currentLat.value = lat
  currentLng.value = lng

  if (map) {
    map.setCenter(new TMap.LatLng(lat, lng))
    reverseGeocode(lat, lng)
    return
  }

  map = new TMap.Map(mapRef.value, {
    center: new TMap.LatLng(lat, lng),
    zoom: 15,
  })

  geocoder = new TMap.service.Geocoder()
  suggestionService = new TMap.service.Suggestion()

  map.on('map_move_end', () => {
    const center = map.getCenter()
    currentLat.value = center.lat
    currentLng.value = center.lng
    reverseGeocode(center.lat, center.lng)
  })

  reverseGeocode(lat, lng)
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
      }
    } catch {
      // ignore
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
  keyword.value = ''
}

function confirmLocation() {
  if (!currentAddress.value) {
    ElMessage.warning('请先选择位置')
    return
  }
  emit('confirm', {
    lat: currentLat.value.toString(),
    lng: currentLng.value.toString(),
    address: currentAddress.value,
  })
  visible.value = false
}
</script>

<style scoped lang="scss">
.map-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.map-search-row {
  flex-shrink: 0;
}

.search-results {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.search-item {
  padding: 8px 12px;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: #f5f7fa;
  }
}

.search-item-title {
  font-size: 14px;
  color: #303133;
}

.search-item-addr {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.map-container {
  height: 400px;
  position: relative;
  border: 1px solid #ebeef5;
  border-radius: 4px;
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

.map-footer {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 0 0;
  color: #606266;
  font-size: 13px;
}

.footer-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
