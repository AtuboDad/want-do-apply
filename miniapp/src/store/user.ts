import { reactive } from 'vue'
import { TOKEN_KEY, USER_KEY } from '../utils/config'

export interface AppUserInfo {
  id: number
  nickName: string
  avatar: string
  /** 是否展示会员角标：'1' 展示 '0' 隐藏 */
  showVip?: string
  gender?: string
  creditScore?: number
  applyCount?: number
  voteCount?: number
  adoptedCount?: number
  status?: string
}

export const userStore = reactive<{
  token: string
  user: AppUserInfo | null
  unread: number
}>({
  token: uni.getStorageSync(TOKEN_KEY) || '',
  user: uni.getStorageSync(USER_KEY) || null,
  unread: 0
})

export function setToken(token: string) {
  userStore.token = token
  uni.setStorageSync(TOKEN_KEY, token)
}

export function setUser(user: AppUserInfo | null) {
  userStore.user = user
  if (user) {
    uni.setStorageSync(USER_KEY, user)
  } else {
    uni.removeStorageSync(USER_KEY)
  }
}

export function clearAuth() {
  userStore.token = ''
  userStore.user = null
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(USER_KEY)
}

export function isLogin(): boolean {
  return !!userStore.token
}
