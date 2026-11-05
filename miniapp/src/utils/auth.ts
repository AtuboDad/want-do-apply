/**
 * 登录态工具（对外统一入口）
 *
 * 具体实现放在 `api/auth.ts`（要发请求），这里只做转发，
 * 让页面既可以从 `utils/auth` 拿登录态便捷方法，也不必关心实现位置。
 */
export { login, ensureLogin, refreshMe, updateProfile } from '../api/auth'
export type { LoginResult } from '../api/auth'
