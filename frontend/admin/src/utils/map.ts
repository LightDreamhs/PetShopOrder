let loadPromise: Promise<void> | null = null

export function loadTMapSDK(): Promise<void> {
  if ((window as any).TMap) return Promise.resolve()
  if (loadPromise) return loadPromise

  loadPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://map.qq.com/api/gljs?v=1.exp&key=${import.meta.env.VITE_TMAP_KEY}`
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('地图 SDK 加载失败'))
    document.head.appendChild(script)
  })
  return loadPromise
}
