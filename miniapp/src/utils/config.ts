/**
 * 接口地址配置
 *
 * H5 开发时走 vite 代理（/app → 后端），无需改这里；
 * 微信开发者工具调试时改成你本机或服务器的地址，并在「详情-本地设置」勾选
 * 「不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书」。
 */
// #ifdef H5
export const BASE_URL = ''
// #endif

// #ifndef H5
export const BASE_URL = 'http://127.0.0.1:8080'
// #endif

export const TOKEN_KEY = 'wda_token'
export const USER_KEY = 'wda_user'
export const APP_NAME = '钱包请等等'

/**
 * uni.login 失败时是否降级成 dev: 假 code。
 *
 * `npm run dev:*`（开发模式）为 true，方便在占位/无权限场景下继续调页面；
 * `npm run build:*`（正式包）为 false —— 线上包绝不把用户伪装成调试账号，
 * 登录失败就如实报错，由后端走真实微信校验。
 */
export const DEV_LOGIN_FALLBACK = !!(import.meta as any).env?.DEV
