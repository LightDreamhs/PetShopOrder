interface TMapLatLng {
  lat: number
  lng: number
}

interface TMapMapOptions {
  center?: TMapLatLng
  zoom?: number
  [key: string]: any
}

interface TMapMap {
  setCenter(latlng: TMapLatLng): void
  getCenter(): TMapLatLng
  on(event: string, callback: (...args: any[]) => void): void
  destroy(): void
  [key: string]: any
}

interface TMapServiceGeocoder {
  getAddress(params: { location: TMapLatLng; getPoi?: boolean }): Promise<any>
  getLocation(params: { address: string; region?: string }): Promise<any>
}

interface TMapServiceSearch {
  search(params: { keyword: string; location?: TMapLatLng; filter?: string; page_size?: number; page_index?: number }): Promise<any>
  searchNearby(params: { keyword: string; location: TMapLatLng; radius?: number; page_size?: number; page_index?: number }): Promise<any>
}

interface TMapServiceSuggestion {
  search(params: { keyword: string; location?: TMapLatLng; region?: string; page_size?: number }): Promise<any>
}

interface TMapServiceGeolocation {
  getLocation(): Promise<any>
}

interface TMapServiceStatic {
  new (): TMapServiceGeocoder
}

interface TMapConstructor {
  Map(container: HTMLElement | string, options: TMapMapOptions): TMapMap
  LatLng(lat: number, lng: number): TMapLatLng
  service: {
    Geocoder: TMapServiceStatic & { new (): TMapServiceGeocoder }
    Search: { new (): TMapServiceSearch }
    Suggestion: { new (): TMapServiceSuggestion }
    Geolocation: { new (options?: { key?: string }): TMapServiceGeolocation }
  }
  constants: Record<string, any>
}

declare const TMap: TMapConstructor
