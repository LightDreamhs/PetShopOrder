<template>
  <van-popup
    :show="show"
    position="right"
    :style="{ width: '100%', height: '100%' }"
    :overlay="false"
    @update:show="(v: boolean) => emit('update:show', v)"
  >
    <div class="map-picker">
      <!-- 顶部搜索栏 -->
      <div class="map-header">
        <van-icon name="arrow-left" size="20" @click="emit('update:show', false)" />
        <van-search
          v-model="keyword"
          placeholder="搜索地点"
          shape="round"
          class="map-search"
        />
      </div>

      <!-- 地图区域 -->
      <div class="map-container">
        <div ref="mapRef" class="map-el" />
        <div class="map-pin">
          <svg width="28" height="40" viewBox="0 0 28 40" fill="none">
            <path d="M14 0C6.268 0 0 6.268 0 14c0 10.5 14 26 14 26s14-15.5 14-26C28 6.268 21.732 0 14 0z" fill="#ee0a24"/>
            <circle cx="14" cy="14" r="6" fill="#fff"/>
          </svg>
        </div>
        <div class="locate-btn" @click="locateUser">
          <van-icon name="aim" size="22" />
        </div>
      </div>

      <!-- POI 列表 -->
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
          <van-icon v-if="selectedIdx === idx" name="success" color="#ee0a24" size="20" />
        </div>
      </div>

      <!-- 底部确认栏 -->
      <div class="map-footer safe-area-bottom">
        <div v-if="enableSave" class="save-row">
          <van-checkbox v-model="saveToCommon" shape="square" icon-size="16">保存为常用地址</van-checkbox>
          <van-tag
            v-for="lb in labelOptions"
            :key="lb"
            :type="selectedLabel === lb ? 'danger' : 'plain'"
            size="medium"
            round
            class="label-tag"
            @click="selectedLabel = selectedLabel === lb ? '' : lb"
          >
            {{ lb }}
          </van-tag>
        </div>
        <van-field
          v-model="detailInput"
          class="detail-field"
          label="门牌号"
          placeholder="楼栋/门牌号，如：3栋502室"
          clearable
        />
        <van-button type="primary" block round @click="confirmAddress">
          选择此地点
        </van-button>
      </div>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import { showToast } from 'vant'
import { loadTMapSDK } from '@/utils/map'
import { useAddressStore } from '@/stores/address'
import type { AddressLabel } from '@/types'

interface PoiItem {
  title: string
  address: string
  lat: number
  lng: number
}

const props = withDefaults(defineProps<{
  show: boolean
  enableSave?: boolean
}>(), {
  enableSave: true,
})

const emit = defineEmits<{
  'update:show': [v: boolean]
  confirm: [data: { address: string; detail: string; lat: string; lng: string; saved?: boolean }]
}>()

const addressStore = useAddressStore()
const labelOptions: AddressLabel[] = ['家', '公司', '学校', '其他']

const mapRef = ref<HTMLElement>()
const keyword = ref('')
const currentLat = ref(0)
const currentLng = ref(0)
const poiList = ref<PoiItem[]>([])
const selectedIdx = ref(0)
const saveToCommon = ref(true)
const selectedLabel = ref<AddressLabel>('')
const detailInput = ref('')

let map: any = null
let geocoder: any = null
let searchService: any = null
let debounceTimer: ReturnType<typeof setTimeout> | null = null
let searchTimer: ReturnType<typeof setTimeout> | null = null
let isSearching = false

// 打开时初始化地图
watch(() => props.show, async (v) => {
  if (!v) return
  keyword.value = ''
  selectedLabel.value = ''
  detailInput.value = ''
  try {
    await loadTMapSDK()
    await nextTick()
    initMap()
  } catch {
    showToast('地图加载失败，请检查网络')
    emit('update:show', false)
  }
})

function initMap() {
  if (!mapRef.value) return
  const TMap = (window as any).TMap
  if (!TMap?.Map) return

  const lat = currentLat.value || 39.908823
  const lng = currentLng.value || 116.397470

  if (map) {
    map.setCenter(new TMap.LatLng(lat, lng))
    fetchNearbyPois(lat, lng)
    return
  }

  const center = new TMap.LatLng(lat, lng)
  map = new TMap.Map(mapRef.value, { center, zoom: 15 })

  geocoder = new TMap.service.Geocoder()
  searchService = new TMap.service.Search({ pageSize: 20 })

  map.on('moveend', () => {
    if (isSearching) return
    const c = map.getCenter()
    currentLat.value = c.lat
    currentLng.value = c.lng
    fetchNearbyPois(c.lat, c.lng)
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
        const pois: PoiItem[] = [{
          title: result.result.address || '当前位置',
          address: result.result.formatted_addresses?.recommend || result.result.address || '',
          lat,
          lng,
        }]
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
      poiList.value = []
    }
  }, 300)
}

watch(keyword, (val) => {
  if (searchTimer) clearTimeout(searchTimer)
  if (!val || !val.trim()) {
    clearSearch()
    return
  }

  isSearching = true
  searchTimer = setTimeout(async () => {
    if (!searchService) return
    try {
      const result = await searchService.searchRegion({
        keyword: val.trim(),
        cityName: '全国',
        pageIndex: 1,
      })
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
    } catch {
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
  currentLat.value = poi.lat
  currentLng.value = poi.lng
  const TMap = (window as any).TMap
  map.setCenter(new TMap.LatLng(poi.lat, poi.lng))
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

async function confirmAddress() {
  const poi = poiList.value[selectedIdx.value]
  if (!poi) {
    showToast('请先选择地址')
    return
  }

  const detail = detailInput.value.trim()
  const payload = {
    address: poi.title,
    detail,
    lat: poi.lat.toString(),
    lng: poi.lng.toString(),
  }

  let saved = false
  if (props.enableSave && saveToCommon.value) {
    try {
      await addressStore.add({
        label: selectedLabel.value || null,
        address: poi.title,
        detail: detail || null,
        lat: poi.lat.toString(),
        lng: poi.lng.toString(),
      })
      saved = true
    } catch (e: any) {
      const msg = e?.response?.data?.message || '地址保存失败'
      showToast(msg)
      // 保存失败仍允许使用该地址下单
    }
  }

  emit('confirm', { ...payload, saved })
  emit('update:show', false)
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

.map-container {
  height: 40vh;
  position: relative;
  flex-shrink: 0;
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

.poi-list {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  border-top: 8px solid #f7f8fa;
}

.poi-empty {
  padding: 24px;
  text-align: center;
  color: $text-muted;
  font-size: 13px;
}

.poi-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  gap: 10px;
  border-bottom: 1px solid #f5f5f5;

  &:last-child {
    border-bottom: none;
  }

  &.active {
    background: #fff5f5;
  }
}

.poi-info {
  flex: 1;
  min-width: 0;
}

.poi-title {
  font-size: 14px;
  color: $text;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.poi-addr {
  font-size: 12px;
  color: $text-secondary;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.map-footer {
  flex-shrink: 0;
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
}

.save-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;

  :deep(.van-checkbox) {
    margin-right: auto;
  }
}

.label-tag {
  cursor: pointer;
}

.detail-field {
  margin-bottom: 12px;

  :deep(.van-field__label) {
    width: auto;
    margin-right: 8px;
    font-size: 13px;
    color: $text-secondary;
  }
}
</style>
