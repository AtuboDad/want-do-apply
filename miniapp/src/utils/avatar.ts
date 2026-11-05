import { fullUrl } from './request'

/**
 * 头像统一入口。
 *
 * 头像有两种存法：
 * - `preset:av03` —— 我们自己的预设头像（萌粒风插画），直接读小程序包内图片，不走网络；
 * - `/upload/xxx.jpg` —— 用户自己用微信头像换的，走服务端。
 * 所以除了这层判断，别的页面不要再自己拼 fullUrl。
 */

const PRESET_PREFIX = 'preset:'

/** 预设头像 ID，顺序即选择面板里的排列顺序 */
export const PRESET_AVATARS = ['av01', 'av02', 'av03', 'av04', 'av05', 'av06', 'av07', 'av08']

/** 没设头像时的兜底：暖色底 + 小圆头 */
export const DEFAULT_AVATAR =
  'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMDAgMTAwIj48cmVjdCB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgcng9IjUwIiBmaWxsPSIjRkVFRkU2Ii8+PGNpcmNsZSBjeD0iNTAiIGN5PSIzOCIgcj0iMTYiIGZpbGw9IiNGMEM4QTQiLz48cGF0aCBkPSJNMjIgODJjMC0xNiAxMi0yOCAyOC0yOHMyOCAxMiAyOCAyOCIgZmlsbD0iI0YwQzhBNCIvPjwvc3ZnPg=='

/** 预设头像的包内路径 */
export function presetAvatarPath(id: string): string {
  return `/static/avatars/${id}.jpg`
}

/** 数据库里存的值，用于保存到后端 */
export function presetAvatarValue(id: string): string {
  return PRESET_PREFIX + id
}

export function isPresetAvatar(avatar?: string | null): boolean {
  return !!avatar && avatar.indexOf(PRESET_PREFIX) === 0
}

/** 把任意来源的头像字段换成 image 组件能直接用的地址 */
export function avatarUrl(avatar?: string | null): string {
  if (!avatar) return DEFAULT_AVATAR
  if (isPresetAvatar(avatar)) {
    const id = avatar.slice(PRESET_PREFIX.length)
    return PRESET_AVATARS.indexOf(id) >= 0 ? presetAvatarPath(id) : DEFAULT_AVATAR
  }
  return fullUrl(avatar) || DEFAULT_AVATAR
}
