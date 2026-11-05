import { request } from '../utils/request'
import { setToken, setUser, isLogin, userStore } from '../store/user'
import type { AppUserInfo } from '../store/user'

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
      // 本地调试时 manifest 里还是占位 AppID，uni.login 会失败。
      // 降级成 dev: 前缀的假 code，后端 mock 模式下会映射成固定用户。
      fail: () => resolve(devCode())
    })
    // #endif
    // #ifndef MP-WEIXIN
    // H5 / 其他平台本地联调：后端 mock 模式会把它映射成固定用户
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
export async function updateProfile(nickName: string, avatar: string): Promise<void> {
  await request({
    url: `/app/auth/profile?nickName=${encodeURIComponent(nickName)}&avatar=${encodeURIComponent(avatar)}`,
    method: 'PUT'
  })
  await refreshMe()
}
