import { BASE_URL, TOKEN_KEY } from './config'

export interface ApiResult<T = any> {
  code: number
  msg: string
  data: T
}

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: Record<string, any>
  auth?: boolean
  /** 静默失败：不弹 toast，由调用方处理 */
  silent?: boolean
  header?: Record<string, string>
}

let redirecting = false

/**
 * 统一请求
 */
export function request<T = any>(options: RequestOptions): Promise<T> {
  const { url, method = 'GET', data, auth = true, silent = false, header = {} } = options
  const token = uni.getStorageSync(TOKEN_KEY)

  return new Promise<T>((resolve, reject) => {
    uni.request({
      url: BASE_URL + url,
      method,
      data,
      timeout: 15000,
      header: {
        'Content-Type': 'application/json',
        ...(auth && token ? { 'App-Token': token } : {}),
        ...header
      },
      success: (res) => {
        const body = res.data as ApiResult<T>
        if (res.statusCode === 401 || body?.code === 401) {
          uni.removeStorageSync(TOKEN_KEY)
          if (!redirecting) {
            redirecting = true
            uni.showToast({ title: '登录已失效', icon: 'none' })
            setTimeout(() => {
              redirecting = false
              uni.reLaunch({ url: '/pages/index/index' })
            }, 600)
          }
          reject(new Error('未登录'))
          return
        }
        if (body && body.code === 200) {
          resolve(body.data)
          return
        }
        const msg = body?.msg || '请求失败'
        if (!silent) {
          uni.showToast({ title: msg, icon: 'none', duration: 2000 })
        }
        reject(new Error(msg))
      },
      fail: (err) => {
        if (!silent) {
          uni.showToast({ title: '网络异常，请稍后再试', icon: 'none' })
        }
        reject(err)
      }
    })
  })
}

/** 上传图片 */
export function uploadFile(filePath: string): Promise<{ url: string; name: string; size: number }> {
  const token = uni.getStorageSync(TOKEN_KEY)
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: BASE_URL + '/app/file/upload',
      filePath,
      name: 'file',
      header: token ? { 'App-Token': token } : {},
      success: (res) => {
        try {
          const body = JSON.parse(res.data) as ApiResult<{ url: string; name: string; size: number }>
          if (body.code === 200) {
            resolve(body.data)
          } else {
            uni.showToast({ title: body.msg || '上传失败', icon: 'none' })
            reject(new Error(body.msg))
          }
        } catch (e) {
          reject(e)
        }
      },
      fail: reject
    })
  })
}

/** 把后端返回的相对图片地址补全 */
export function fullUrl(url?: string): string {
  if (!url) return ''
  if (url.startsWith('http') || url.startsWith('data:')) return url
  return BASE_URL + url
}
