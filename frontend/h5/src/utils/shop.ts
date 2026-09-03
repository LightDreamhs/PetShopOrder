/**
 * 当前门店编码的本地持久化（H5 多店）。
 *
 * 进店链路：门店二维码 URL 带 ?s={code} → main.ts 启动时解析并落 localStorage；
 * 之后所有 API 请求由 axios 拦截器附带 X-Shop-Code 请求头，后端据此解析当前店。
 */
const KEY = 'petshop_shop_code'

export function getStoredShopCode(): string {
  return localStorage.getItem(KEY) || ''
}

export function setStoredShopCode(code: string) {
  if (code) {
    localStorage.setItem(KEY, code)
  } else {
    localStorage.removeItem(KEY)
  }
}

/** 从当前 URL 提取进店参数 ?s={code}（有则返回并规范化） */
export function takeShopCodeFromUrl(): string | null {
  const params = new URLSearchParams(window.location.search)
  const code = params.get('s')
  if (code && /^[a-zA-Z0-9-]{2,32}$/.test(code)) {
    return code.toLowerCase()
  }
  return null
}
