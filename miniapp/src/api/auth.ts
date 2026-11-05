import { request } from '../utils/request'
import { setToken, setUser, isLogin, userStore } from '../store/user'
import type { AppUserInfo } from '../store/user'
import { DEV_LOGIN_FALLBACK } from '../utils/config'

export interface LoginResult {
  token: string
  expireAt: number
  user: AppUserInfo
  newUser: boolean
}

/** 拿微信登录 code */
function getWxCode(): Promise<string> {
  return new Promise((resolve, reject) => {
    // #ifdef MP-WEIXIN
    uni.login({
      provider: 'weixin',
      success: (res) => resolve(res.code as string),
      fail: (err) => {
        // 开发构建下退化成 dev: 假 code，方便在开发者工具里继续调业务
        // 正式包不降级：后端会走真实微信校验，伪造凭证必须失败
        if (DEV_LOGIN_FALLBACK) {
          resolve(devCode())
        } else {
          console.error('微信登录失败', err)
          reject(new Error('微信登录失败，请稍后重试'))
        }
      }
    })
    // #endif
    // #ifndef MP-WEIXIN
    // H5 仅用于本地调试，固定用 dev: 假 code（后端需开启 allow-dev-code）
    resolve(devCode())
    // #endif
  })
}

/**
 * 生成调试用的假 code。
 *
 * 每台设备/每个开发者工具实例一个固定 ID，清缓存后会换一个，
 * 这样本地可以同时模拟两个用户来测「发起 + 批阅」。
 */
function devCode(): string {
  const DEV_ID_KEY = 'wda_dev_id'
  let id = uni.getStorageSync(DEV_ID_KEY)
  if (!id) {
    id = Math.random().toString(36).slice(2, 8)
    uni.setStorageSync(DEV_ID_KEY, id)
  }
  return 'dev:' + id
}

let loginPromise: Promise<LoginResult> | null = null

/** 登录（并发调用只会真正执行一次） */
export function login(extra?: { nickName?: string; avatar?: string }): Promise<LoginResult> {
  if (loginPromise) return loginPromise
  loginPromise = (async () => {
    const code = await getWxCode()
    const res = await request<LoginResult>({
      url: '/app/auth/login',
      method: 'POST',
      auth: false,
      data: { code, ...extra }
    })
    setToken(res.token)
    setUser(res.user)
    return res
  })()
  loginPromise.finally(() => {
    loginPromise = null
  })
  return loginPromise
}

/** 确保已登录：已有 token 直接用，否则静默登录 */
export async function ensureLogin(): Promise<void> {
  if (isLogin() && userStore.user) return
  await login()
}

/** 刷新当前用户 */
export async function refreshMe(): Promise<AppUserInfo> {
  const me = await request<AppUserInfo>({ url: '/app/auth/me' })
  setUser(me)
  return me
}

/** 更新昵称头像 */
/**
 * 更新昵称 / 头像 / 会员角标开关。
 *
 * 三个参数都可选，只传要改的那个即可（后端按非空字段更新）。
 */
export async function updateProfile(
  nickName?: string,
  avatar?: string,
  showVip?: string
): Promise<void> {
  const qs: string[] = []
  if (nickName !== undefined) qs.push(`nickName=${encodeURIComponent(nickName)}`)
  if (avatar !== undefined) qs.push(`avatar=${encodeURIComponent(avatar)}`)
  if (showVip !== undefined) qs.push(`showVip=${encodeURIComponent(showVip)}`)
  await request({
    url: `/app/auth/profile?${qs.join('&')}`,
    method: 'PUT'
  })
  await refreshMe()
}
