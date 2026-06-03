let loadPromise: Promise<void> | null = null

export function loadTMapSDK(): Promise<void> {
  if ((window as any).TMap?.Map && (window as any).TMap?.service?.Geocoder) return Promise.resolve()
  if (loadPromise) return loadPromise

  loadPromise = new Promise<void>((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://map.qq.com/api/gljs?v=1.exp&key=${import.meta.env.VITE_TMAP_KEY}&libraries=service`

    const timeout = setTimeout(() => {
      loadPromise = null
      reject(new Error('地图 SDK 加载超时，请检查网络连接'))
    }, 15000)

    script.onload = () => {
      clearTimeout(timeout)
      // 等待 TMap 及 service 模块完全初始化
      const check = setInterval(() => {
        if ((window as any).TMap?.Map && (window as any).TMap?.service?.Geocoder) {
          clearInterval(check)
          resolve()
        }
      }, 50)
      // 兜底：3 秒后仍未就绪则视为失败
      setTimeout(() => {
        clearInterval(check)
        if (!(window as any).TMap?.Map) {
          loadPromise = null
          reject(new Error('地图 SDK 初始化失败'))
        }
      }, 3000)
    }
    script.onerror = () => {
      clearTimeout(timeout)
      loadPromise = null
      reject(new Error('地图 SDK 加载失败'))
    }
    document.head.appendChild(script)
  })
  return loadPromise
}
